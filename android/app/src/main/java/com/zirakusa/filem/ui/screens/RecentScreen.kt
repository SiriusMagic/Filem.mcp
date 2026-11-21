package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.data.RecentGroup
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.Screen
import com.zirakusa.filem.ui.theme.FilemColors

private val groupNames = mapOf(RecentGroup.TODAY to R.string.today, RecentGroup.YESTERDAY to R.string.yesterday,
    RecentGroup.LAST_7_DAYS to R.string.last_7_days, RecentGroup.LAST_30_DAYS to R.string.last_30_days)

/** «Reciente»: the files of the last 30 days in groups (Hoy, Ayer, Últimos 7 y 30 días) that open and close. */
@Composable fun RecentScreen(state: FilesState, actions: Actions) {
    val groups = state.recentGroups
    val count = groups.values.sumOf { it.size }
    LazyColumn(Modifier.fillMaxSize().background(FilemColors.Background).statusBarsPadding().testTag("recent"),
        contentPadding = PaddingValues(bottom = 16.dp)) {
        item { BigTitle(stringResource(R.string.tab_recent)) { FilterMenu(state) } }
        if (state.loading && count == 0) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp), color = FilemColors.Accent) }
        groups.forEach { (group, files) ->
            val open = group in state.expanded
            item(key = group.name) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(FilemColors.Card).clickable { state.toggleGroup(group) }.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(groupNames.getValue(group)), fontSize = 18.sp, fontWeight = FontWeight.Medium, color = FilemColors.Text)
                    Text(files.size.toString(), Modifier.weight(1f).padding(start = 10.dp), fontSize = 15.sp, color = FilemColors.Muted)
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        stringResource(if (open) R.string.collapse else R.string.expand), tint = FilemColors.Muted)
                }
            }
            if (open) items(files, key = { group.name + "/" + it.path }) { item ->
                FileRow(item, state.store.root, selected = false, selecting = false, onClick = { actions.open(item) }, onLongClick = {}) {
                    RecentMenu(item, state, actions)
                }
            }
        }
        item {
            Text(pluralStringResource(R.plurals.recent_footer, count, count), Modifier.fillMaxWidth().padding(24.dp),
                fontSize = 13.sp, color = FilemColors.Muted, textAlign = TextAlign.Center)
        }
    }
}

/** The round filter button: only one type of file, or all. */
@Composable private fun FilterMenu(state: FilesState) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundButton(Icons.Outlined.FilterList, stringResource(R.string.filter),
            tint = if (state.recentFilter != null) FilemColors.Accent else FilemColors.Text) { open = true }
        DropdownMenu(open, { open = false }, containerColor = FilemColors.Scrim) {
            (listOf<Category?>(null) + Category.entries.filter { !it.isPlace }).forEach { c ->
                DropdownMenuItem({ Text(stringResource(c?.let { categoryNames.getValue(it) } ?: R.string.all_types)) },
                    { open = false; state.filterRecent(c) },
                    trailingIcon = { if (c == state.recentFilter) Icon(Icons.Outlined.Check, null, tint = FilemColors.Accent) })
            }
        }
    }
}

/** ⋮ of a recent file: open, see it in its folder, share. */
@Composable private fun RecentMenu(item: FileItem, state: FilesState, actions: Actions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options), tint = FilemColors.Text) }
        DropdownMenu(open, { open = false }, containerColor = FilemColors.Scrim) {
            DropdownMenuItem({ Text(stringResource(R.string.open)) }, { open = false; actions.open(item) })
            DropdownMenuItem({ Text(stringResource(R.string.show_in_folder)) }, { open = false; state.go(Screen.Browse(FileRules.parent(item.path))) })
            DropdownMenuItem({ Text(stringResource(R.string.share)) }, { open = false; actions.share(listOf(item.path)) })
        }
    }
}
