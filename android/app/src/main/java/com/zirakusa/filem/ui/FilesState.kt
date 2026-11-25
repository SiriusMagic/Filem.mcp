package com.zirakusa.filem.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.data.Prefs
import com.zirakusa.filem.data.RecentGroup
import com.zirakusa.filem.data.RecentRules
import com.zirakusa.filem.data.Sort
import com.zirakusa.filem.data.TrashEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the user is. Browse and the lists keep their own place in the back stack; the two tabs are its bottom. */
sealed interface Screen {
    data object Navigator : Screen
    data object Recent : Screen
    data class Browse(val path: String) : Screen
    data class CategoryList(val category: Category) : Screen
    data object Starred : Screen
    data object Search : Screen
    data object Trash : Screen
    data object Settings : Screen
}

/** Copy or move waiting for «Pegar aquí». */
data class Clip(val paths: List<String>, val move: Boolean)

/** The only owner of Filem's screen state. Screens only draw it and call these functions. */
class FilesState(val store: FileStore, private val prefs: Prefs?, private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO, private val now: () -> Long = System::currentTimeMillis) {
    private val stack = ArrayDeque<Screen>()
    var screen by mutableStateOf<Screen>(Screen.Navigator); private set
    var items by mutableStateOf(emptyList<FileItem>()); private set
    var loading by mutableStateOf(false); private set
    var grid by mutableStateOf(prefs?.grid ?: false); private set
    var sort by mutableStateOf(prefs?.sort ?: Sort()); private set
    var selection by mutableStateOf(emptySet<String>())
    var clip by mutableStateOf<Clip?>(null)
    var message by mutableStateOf<String?>(null)
    /** «Reciente»: the files of the last 30 days, the type filter and the open groups. */
    var recents by mutableStateOf(emptyList<FileItem>()); private set
    var recentFilter by mutableStateOf<Category?>(null); private set
    var expanded by mutableStateOf(setOf(RecentGroup.TODAY)); private set
    var history by mutableStateOf(prefs?.history ?: emptyList()); private set
    var space by mutableStateOf(0L to 0L); private set
    var trash by mutableStateOf(emptyList<TrashEntry>()); private set
    var starred by mutableStateOf(prefs?.starred() ?: emptySet()); private set
    var showHidden by mutableStateOf(prefs?.showHidden ?: false); private set

    init { store.showHidden = showHidden }

    val title: String get() = (screen as? Screen.Browse)?.path?.substringAfterLast('/').orEmpty()

    val recentGroups: Map<RecentGroup, List<FileItem>>
        get() = RecentRules.group(recents.filter { recentFilter == null || it.category == recentFilter }, now())

    /** The bottom bar: «Reciente» or «Navegador», with an empty back stack. */
    fun tab(next: Screen) {
        if (screen == next) return
        stack.clear(); screen = next; selection = emptySet(); refresh()
    }

    fun go(next: Screen) { stack.addLast(screen); screen = next; selection = emptySet(); items = emptyList(); refresh() }
    fun open(item: FileItem) = go(Screen.Browse(item.path))

    /** Back: leaves selection first, then the previous screen, then «Reciente» goes to «Navegador». False at the end. */
    fun back(): Boolean {
        if (selection.isNotEmpty()) { selection = emptySet(); return true }
        screen = stack.removeLastOrNull() ?: if (screen == Screen.Recent) Screen.Navigator else return false
        refresh(); return true
    }

    fun refresh() = run(reload = false) { loadScreen() }

    /** The same, without the loading bar and keeping the selection: after Android said files changed. */
    fun refreshQuietly() = run(reload = false, quiet = true) { loadScreen() }

    private fun loadScreen() {
        when (val s = screen) {
            Screen.Navigator -> { space = store.space(); trash = store.trashed() }
            Screen.Recent -> recents = store.recent(RECENT_MAX, RecentRules.since(now()))
            is Screen.Browse -> items = store.list(s.path, sort)
            is Screen.CategoryList -> items = store.search("", s.category, limit = 500)
            Screen.Starred -> items = FileRules.sorted(starred.mapNotNull { runCatching { store.info(it) }.getOrNull() }, sort)
            Screen.Trash, Screen.Settings -> trash = store.trashed()
            Screen.Search -> Unit
        }
    }

    fun search(query: String, category: Category?, sinceMs: Long, newestFirst: Boolean) = run(reload = false) {
        val found = store.search(query, category, limit = 500).filter { it.modified >= sinceMs }
        items = if (newestFirst) found else found.reversed()
    }

    fun toggleGroup(group: RecentGroup) { expanded = if (group in expanded) expanded - group else expanded + group }
    fun filterRecent(category: Category?) { recentFilter = category }

    /** «Buscar historial»: a search the user went through with (pressed search or opened a result). */
    fun rememberSearch(query: String) {
        val next = FileRules.addToHistory(history, query)
        if (next != history) { history = next; prefs?.history = next }
    }
    fun clearHistory() { history = emptyList(); prefs?.history = emptyList() }

    fun toggleGrid() { grid = !grid; prefs?.grid = grid }
    fun sortBy(next: Sort) { sort = next; prefs?.sort = next; refresh() }
    fun setHidden(on: Boolean) { showHidden = on; store.showHidden = on; prefs?.showHidden = on }

    fun toggle(item: FileItem) { selection = if (item.path in selection) selection - item.path else selection + item.path }
    fun selectAll() { selection = items.map { it.path }.toSet() }

    fun createFolder(name: String) = act { store.createFolder((screen as? Screen.Browse)?.path.orEmpty(), name.trim()) }
    fun rename(item: FileItem, name: String) = act { store.rename(item.path, name.trim()) }
    fun trashSelected(paths: Collection<String> = selection) = act { paths.forEach { store.trash(it) }; setStars(paths, false) }
    fun copy(paths: Collection<String> = selection, move: Boolean) { clip = Clip(paths.toList(), move); selection = emptySet() }
    fun paste() {
        val c = clip ?: return
        val to = (screen as? Screen.Browse)?.path ?: return
        act { store.transfer(c.paths, to, c.move); if (c.move) setStars(c.paths, false); clip = null }
    }
    fun star(item: FileItem) = setStars(listOf(item.path), item.path !in starred)
    private fun setStars(paths: Collection<String>, on: Boolean) {
        paths.forEach { prefs?.setStarred(it, on) }
        starred = if (on) starred + paths else starred - paths.toSet()
    }

    fun restore(entry: TrashEntry) = act { store.restore(entry.id) }
    fun deleteForever(entry: TrashEntry) = act { store.deleteForever(entry.id) }
    fun emptyTrash() = act { store.emptyTrash() }

    private fun act(block: () -> Unit) = run(reload = true, block = block)

    /**
     * Runs [block] off the main thread. A change ([reload]) then reloads the screen and clears the selection, even when
     * it failed half way (some files may have moved). A failure becomes [message].
     */
    private fun run(reload: Boolean, quiet: Boolean = false, block: () -> Unit) {
        if (!quiet) loading = true
        scope.launch {
            val error = withContext(io) {
                val failure = runCatching(block).exceptionOrNull()
                if (reload) runCatching { loadScreen() }
                failure
            }
            if (!quiet) loading = false
            if (reload) selection = emptySet()
            if (error != null) message = error.message ?: error.toString()
        }
    }

    companion object { const val RECENT_MAX = 2000 }
}
