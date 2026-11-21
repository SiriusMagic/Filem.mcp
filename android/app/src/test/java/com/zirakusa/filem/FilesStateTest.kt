package com.zirakusa.filem

import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.data.RecentGroup
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The screen state: navigation and back, new folder, selection, copy/move with «Pegar aquí», trash and errors. */
@RunWith(RobolectricTestRunner::class)
class FilesStateTest {
    @get:Rule val tmp = TemporaryFolder()
    private val root by lazy { tmp.newFolder("storage").also { File(it, "Download").mkdirs(); File(it, "Download/a.pdf").writeText("pdf") } }
    private val state by lazy { FilesState(FileStore(root), null, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined) }

    @Test fun browseCreateAndGoBack() {
        state.go(Screen.Browse(""))
        assertEquals(listOf("Download"), state.items.map { it.name })
        state.open(state.items.first())
        assertEquals(listOf("a.pdf"), state.items.map { it.name })
        state.createFolder("Facturas")
        assertEquals(listOf("Facturas", "a.pdf"), state.items.map { it.name })
        state.createFolder("Facturas")
        assertTrue(state.message!!.contains("already exists"))
        assertTrue(state.back()); assertEquals(Screen.Browse(""), state.screen)
        assertTrue(state.back()); assertEquals(Screen.Navigator, state.screen)
        assertFalse(state.back())
    }

    @Test fun selectMoveAndTrash() {
        state.go(Screen.Browse("Download"))
        state.createFolder("Docs")
        state.toggle(state.items.first { it.name == "a.pdf" })
        state.copy(move = true)
        assertTrue(state.selection.isEmpty())
        state.go(Screen.Browse("Download/Docs"))
        state.paste()
        assertNull(state.clip)
        assertEquals(listOf("a.pdf"), state.items.map { it.name })
        state.selectAll(); state.trashSelected()
        assertTrue(state.items.isEmpty())
        state.go(Screen.Trash)
        assertEquals(listOf("a.pdf"), state.trash.map { it.name })
        state.restore(state.trash.first())
        assertTrue(File(root, "Download/Docs/a.pdf").exists())
    }

    @Test fun favoritesAndCategories() {
        File(root, "Download/fotos.zip").writeText("zip")
        state.go(Screen.Browse("Download"))
        state.star(state.items.first { it.name == "a.pdf" })
        state.go(Screen.Starred)
        assertEquals(listOf("a.pdf"), state.items.map { it.name })
        state.go(Screen.CategoryList(Category.ZIPS))
        assertEquals(listOf("fotos.zip"), state.items.map { it.name })
        state.go(Screen.CategoryList(Category.RECEIVED))
        assertEquals(setOf("a.pdf", "fotos.zip"), state.items.map { it.name }.toSet())
    }

    @Test fun opensOnNavigatorAndTabsKeepNoHistory() {
        assertEquals(Screen.Navigator, state.screen)
        state.refresh()
        assertTrue(state.space.second > 0)
        state.tab(Screen.Recent)
        state.go(Screen.Search)
        assertTrue(state.back()); assertEquals(Screen.Recent, state.screen)
        assertTrue(state.back()); assertEquals(Screen.Navigator, state.screen) // back from «Reciente» goes to «Navegador»
        assertFalse(state.back())
        state.go(Screen.Browse("")); state.tab(Screen.Recent); state.tab(Screen.Navigator)
        assertFalse(state.back())
    }

    @Test fun recentGroupsFilterAndFold() {
        val now = System.currentTimeMillis()
        File(root, "Download/a.pdf").setLastModified(now)
        File(root, "Download/foto.jpg").apply { writeText("j") }.setLastModified(now - 3 * 86_400_000L)
        File(root, "Download/viejo.txt").apply { writeText("v") }.setLastModified(now - 60 * 86_400_000L)
        state.tab(Screen.Recent)
        assertEquals(listOf("a.pdf", "foto.jpg"), state.recents.map { it.name })
        assertEquals(listOf(RecentGroup.TODAY, RecentGroup.LAST_7_DAYS), state.recentGroups.keys.toList())
        assertEquals(setOf(RecentGroup.TODAY), state.expanded)
        state.toggleGroup(RecentGroup.LAST_7_DAYS); state.toggleGroup(RecentGroup.TODAY)
        assertEquals(setOf(RecentGroup.LAST_7_DAYS), state.expanded)
        state.filterRecent(Category.IMAGES)
        assertEquals(mapOf(RecentGroup.LAST_7_DAYS to listOf("foto.jpg")), state.recentGroups.mapValues { g -> g.value.map { it.name } })
        state.filterRecent(null)
        assertEquals(2, state.recentGroups.values.sumOf { it.size })
    }

    @Test fun searchHistory() {
        state.rememberSearch("informe"); state.rememberSearch("  "); state.rememberSearch("fotos"); state.rememberSearch("Informe")
        assertEquals(listOf("Informe", "fotos"), state.history)
        state.clearHistory()
        assertTrue(state.history.isEmpty())
    }
}
