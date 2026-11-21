package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.data.TrashEntry
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.theme.FilemColors

/** «Papelera»: what Filem (or an AI) deleted, restorable for 30 days. */
@Composable fun TrashScreen(state: FilesState) {
    var emptying by remember { mutableStateOf(false) }
    var forever by remember { mutableStateOf<TrashEntry?>(null) }
    Column(Modifier.fillMaxSize().background(FilemColors.Background)) {
        TopBar(stringResource(R.string.trash), { state.back() }, stringResource(R.string.back)) {
            if (state.trash.isNotEmpty()) TextButton({ emptying = true }) { Text(stringResource(R.string.empty_trash), color = FilemColors.Accent) }
        }
        Text(stringResource(R.string.trash_hint), Modifier.padding(20.dp), color = FilemColors.Muted)
        LazyColumn(Modifier.weight(1f)) {
            items(state.trash, key = { it.id }) { entry ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(FilemColors.Tile), contentAlignment = Alignment.Center) {
                        Icon(if (entry.isDir) Icons.Filled.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile, null, tint = FilemColors.Folder)
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = FilemColors.Text, fontSize = 17.sp)
                        Text("/" + FileRules.parent(entry.original) + " • " + friendlyDate(entry.deletedAt), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, color = FilemColors.Muted, fontSize = 13.sp)
                    }
                    TextButton({ state.restore(entry) }) { Text(stringResource(R.string.restore), color = FilemColors.Accent) }
                    TextButton({ forever = entry }) { Text(stringResource(R.string.delete), color = FilemColors.Danger) }
                }
            }
        }
    }
    if (emptying) ConfirmDialog(stringResource(R.string.empty_trash), stringResource(R.string.forever_text), stringResource(R.string.delete),
        { emptying = false }) { state.emptyTrash(); emptying = false }
    forever?.let { entry ->
        ConfirmDialog(stringResource(R.string.delete_forever_title, entry.name), stringResource(R.string.forever_text),
            stringResource(R.string.delete), { forever = null }) { state.deleteForever(entry); forever = null }
    }
}
