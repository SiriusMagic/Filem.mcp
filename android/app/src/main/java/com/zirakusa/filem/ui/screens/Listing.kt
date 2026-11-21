package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.theme.FilemColors

/** The files of a folder, a category or a search: list or grid, tap to open, long press to select, ⋮ for one item. */
@Composable fun Listing(state: FilesState, actions: Actions, modifier: Modifier = Modifier, header: @Composable () -> Unit = {}) {
    var renaming by remember { mutableStateOf<FileItem?>(null) }
    var info by remember { mutableStateOf<FileItem?>(null) }
    val selecting = state.selection.isNotEmpty()
    val click: (FileItem) -> Unit = { item ->
        when { selecting -> state.toggle(item); item.isDir -> state.open(item); else -> actions.open(item) }
    }
    if (state.items.isEmpty() && !state.loading) Column(modifier.fillMaxSize()) {
        header()
        Text(stringResource(R.string.empty_folder), Modifier.padding(24.dp), color = FilemColors.Muted)
    } else if (state.grid) LazyVerticalGrid(GridCells.Adaptive(116.dp), modifier.fillMaxSize().testTag("grid"),
        contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 96.dp)) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { header() }
        items(state.items, key = { it.path }) { item ->
            FileTile(item, state.store.root, item.path in state.selection, { click(item) }, { state.toggle(item) })
        }
    } else LazyColumn(modifier.fillMaxSize().testTag("list"), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { header() }
        items(state.items, key = { it.path }) { item ->
            FileRow(item, state.store.root, item.path in state.selection, selecting, { click(item) }, { state.toggle(item) }) {
                ItemMenu(item, state, actions, onRename = { renaming = item }, onInfo = { info = item })
            }
        }
    }
    renaming?.let { item ->
        NameDialog(stringResource(R.string.rename), item.name, stringResource(R.string.rename), { renaming = null }) {
            state.rename(item, it); renaming = null
        }
    }
    info?.let { InfoDialog(it) { info = null } }
}

@Composable private fun ItemMenu(item: FileItem, state: FilesState, actions: Actions, onRename: () -> Unit, onInfo: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options), tint = FilemColors.Text) }
        DropdownMenu(open, { open = false }, containerColor = FilemColors.Scrim) {
            @Composable fun entry(label: Int, action: () -> Unit) =
                DropdownMenuItem({ Text(stringResource(label)) }, { open = false; action() })
            entry(R.string.open) { if (item.isDir) state.open(item) else actions.open(item) }
            entry(R.string.rename, onRename)
            entry(R.string.copy_to) { state.copy(listOf(item.path), move = false) }
            entry(R.string.move_to) { state.copy(listOf(item.path), move = true) }
            entry(if (item.path in state.starred) R.string.unstar else R.string.star) { state.star(item) }
            if (!item.isDir) entry(R.string.share) { actions.share(listOf(item.path)) }
            entry(R.string.details, onInfo)
            entry(R.string.move_to_trash) { state.trashSelected(listOf(item.path)) }
        }
    }
}
