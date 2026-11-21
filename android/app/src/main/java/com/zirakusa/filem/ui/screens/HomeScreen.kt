package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.Screen
import com.zirakusa.filem.ui.theme.FilemColors

val categoryNames = mapOf(Category.DOWNLOADS to R.string.cat_downloads, Category.IMAGES to R.string.cat_images,
    Category.VIDEOS to R.string.cat_videos, Category.AUDIO to R.string.cat_audio, Category.DOCUMENTS to R.string.cat_documents,
    Category.APPS to R.string.cat_apps)

@Composable fun HomeScreen(state: FilesState, actions: Actions, openDrawer: () -> Unit) {
    Column(Modifier.fillMaxSize().background(FilemColors.Background).verticalScroll(rememberScrollState()).statusBarsPadding()
        .padding(bottom = 32.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(FilemColors.Bar)
            .clickable { state.go(Screen.Search) }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(openDrawer) { Icon(Icons.Outlined.Menu, stringResource(R.string.menu), tint = FilemColors.Text) }
            Text(stringResource(R.string.search_hint), Modifier.weight(1f), fontSize = 18.sp, color = FilemColors.Muted)
            Icon(Icons.Outlined.Search, stringResource(R.string.search), Modifier.padding(12.dp), tint = FilemColors.Text)
        }
        if (state.recents.isNotEmpty()) {
            Heading(stringResource(R.string.recent)) {
                Text(stringResource(R.string.see_all), Modifier.clickable { state.go(Screen.Search) }, color = FilemColors.Accent, fontSize = 17.sp)
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.recents, key = { it.path }) { item ->
                    Column(Modifier.width(150.dp).clip(RoundedCornerShape(24.dp)).background(FilemColors.Card)
                        .clickable { actions.open(item) }.padding(10.dp)) {
                        FileIcon(item, state.store.root, 130.dp)
                        Text(item.name, Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, color = FilemColors.Text)
                        Text(FileRules.size(item.size), fontSize = 13.sp, color = FilemColors.Muted)
                    }
                }
            }
        }
        Heading(stringResource(R.string.categories))
        Grid(Category.entries.map { c ->
            Tile(categoryIcon(c), stringResource(categoryNames.getValue(c)), FileRules.size(state.categories[c]?.first ?: 0)) {
                state.go(Screen.CategoryList(c))
            }
        })
        Heading(stringResource(R.string.collections))
        Grid(listOf(
            Tile(Icons.Outlined.StarOutline, stringResource(R.string.starred), null) { state.go(Screen.Starred) },
            Tile(Icons.Outlined.Delete, stringResource(R.string.trash), FileRules.size(state.trash.sumOf { it.size })) { state.go(Screen.Trash) },
        ))
        Heading(stringResource(R.string.all_storage))
        Grid(listOf(Tile(Icons.Outlined.PhoneAndroid, stringResource(R.string.internal_storage),
            stringResource(R.string.free_space, FileRules.size(state.space.first))) { state.go(Screen.Browse("")) }), tag = "storage")
    }
}

private class Tile(val icon: ImageVector, val title: String, val subtitle: String?, val onClick: () -> Unit)

@Composable private fun Heading(text: String, end: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), fontSize = 24.sp, color = FilemColors.Text)
        end()
    }
}

/** Two cards per row, like the phone's Files. */
@Composable private fun Grid(tiles: List<Tile>, tag: String = "") {
    Column(Modifier.padding(horizontal = 16.dp).testTag(tag), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tile ->
                    Row(Modifier.weight(1f).height(88.dp).clip(RoundedCornerShape(24.dp)).background(FilemColors.Card)
                        .clickable(onClick = tile.onClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(tile.icon, null, tint = FilemColors.Text)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(tile.title, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = FilemColors.Text)
                            tile.subtitle?.let { Text(it, fontSize = 15.sp, color = FilemColors.Muted, maxLines = 1) }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
