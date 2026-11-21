package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.Screen
import com.zirakusa.filem.ui.theme.FilemColors

/** One folder of the storage. Also used for a category, «Destacados» and search results ([title] given, no FAB). */
@Composable fun BrowserScreen(state: FilesState, actions: Actions, title: String? = null) {
    var newFolder by remember { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    var trashing by remember { mutableStateOf(false) }
    val path = (state.screen as? Screen.Browse)?.path
    val storage = stringResource(R.string.internal_storage)
    Box(Modifier.fillMaxSize().background(FilemColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            if (state.selection.isNotEmpty()) SelectionBar(state, actions) { trashing = true }
            else TopBar(title ?: path?.substringAfterLast('/')?.ifEmpty { storage } ?: storage, { state.back() }, stringResource(R.string.back)) {
                IconButton(state::toggleGrid) {
                    Icon(if (state.grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                        stringResource(if (state.grid) R.string.view_list else R.string.view_grid), tint = FilemColors.Text)
                }
                Overflow(onSelectAll = state::selectAll, onSort = { sorting = true }, onNewFolder = { newFolder = true }.takeIf { path != null })
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = FilemColors.Accent)
            Listing(state, actions, Modifier.weight(1f)) { if (path != null) Breadcrumb(path, storage) { state.go(Screen.Browse(it)) } }
            state.clip?.let { clip ->
                Row(Modifier.fillMaxWidth().background(FilemColors.Bar).navigationBarsPadding().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(pluralStringResource(if (clip.move) R.plurals.moving else R.plurals.copying, clip.paths.size, clip.paths.size),
                        Modifier.weight(1f), color = FilemColors.Text)
                    TextButton({ state.clip = null }) { Text(stringResource(R.string.cancel), color = FilemColors.Text) }
                    Button(state::paste, enabled = path != null) { Text(stringResource(R.string.paste_here)) }
                }
            }
        }
        if (path != null && state.clip == null && state.selection.isEmpty()) FloatingActionButton({ newFolder = true },
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
            containerColor = FilemColors.Fab, contentColor = FilemColors.OnFab) {
            Icon(Icons.Outlined.CreateNewFolder, stringResource(R.string.new_folder))
        }
    }
    if (newFolder) NameDialog(stringResource(R.string.new_folder), "", stringResource(R.string.create_folder), { newFolder = false }) {
        state.createFolder(it); newFolder = false
    }
    if (sorting) SortDialog(state.sort, { sorting = false }) { state.sortBy(it); sorting = false }
    if (trashing) ConfirmDialog(pluralStringResource(R.plurals.trash_title, state.selection.size, state.selection.size),
        stringResource(R.string.trash_text), stringResource(R.string.move_to_trash), { trashing = false }) {
        state.trashSelected(); trashing = false
    }
}

@Composable private fun Overflow(onSelectAll: () -> Unit, onSort: () -> Unit, onNewFolder: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options), tint = FilemColors.Text) }
        DropdownMenu(open, { open = false }, containerColor = FilemColors.Scrim) {
            DropdownMenuItem({ Text(stringResource(R.string.select_all)) }, { open = false; onSelectAll() })
            DropdownMenuItem({ Text(stringResource(R.string.sort_by)) }, { open = false; onSort() })
            if (onNewFolder != null) DropdownMenuItem({ Text(stringResource(R.string.add_new_folder)) }, { open = false; onNewFolder() })
        }
    }
}

/** «Almacenamiento interno › Download › Fotos», each part tappable. */
@Composable private fun Breadcrumb(path: String, storage: String, go: (String) -> Unit) {
    val parts = path.split('/').filter { it.isNotEmpty() }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(storage, Modifier.clickable { go("") }, color = FilemColors.Accent, fontSize = 17.sp)
        parts.forEachIndexed { i, part ->
            Icon(Icons.Outlined.ChevronRight, null, tint = FilemColors.Muted)
            Text(part, Modifier.clickable { go(parts.take(i + 1).joinToString("/")) }, color = FilemColors.Accent, fontSize = 17.sp)
        }
    }
}

@Composable private fun SelectionBar(state: FilesState, actions: Actions, onTrash: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(FilemColors.Bar).statusBarsPadding().height(72.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton({ state.selection = emptySet() }) { Icon(Icons.Outlined.Close, stringResource(R.string.cancel), tint = FilemColors.Text) }
        Text(pluralStringResource(R.plurals.selected, state.selection.size, state.selection.size), Modifier.weight(1f),
            fontSize = 18.sp, color = FilemColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton({ actions.share(state.selection.toList()) }) { Icon(Icons.Outlined.Share, stringResource(R.string.share), tint = FilemColors.Text) }
        IconButton({ state.copy(move = false) }) { Icon(Icons.Outlined.ContentCopy, stringResource(R.string.copy_to), tint = FilemColors.Text) }
        IconButton({ state.copy(move = true) }) { Icon(Icons.Outlined.DriveFileMove, stringResource(R.string.move_to), tint = FilemColors.Text) }
        IconButton(onTrash) { Icon(Icons.Outlined.Delete, stringResource(R.string.move_to_trash), tint = FilemColors.Text) }
    }
}
