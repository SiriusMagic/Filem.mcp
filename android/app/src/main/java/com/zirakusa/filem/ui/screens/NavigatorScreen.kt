package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.Screen
import com.zirakusa.filem.ui.theme.FilemColors

/** The 4×2 grid of «Navegador», in Xiaomi Files' order and colours. */
private val tiles = listOf(Category.IMAGES to FilemColors.CatBlue, Category.VIDEOS to FilemColors.CatPurple,
    Category.DOCUMENTS to FilemColors.CatYellow, Category.AUDIO to FilemColors.CatRed, Category.APPS to FilemColors.CatGreen,
    Category.ZIPS to FilemColors.CatSky, Category.RECEIVED to FilemColors.CatBlue, Category.DOWNLOADS to FilemColors.CatOrange)

/** «Navegador»: local files with the used space, the categories and «Ubicación» (phone, recently deleted, favourites). */
@Composable fun NavigatorScreen(state: FilesState) {
    Column(Modifier.fillMaxSize().background(FilemColors.Background).statusBarsPadding().verticalScroll(rememberScrollState())
        .padding(bottom = 16.dp)) {
        BigTitle(stringResource(R.string.tab_browser)) { MainMenu(state) }
        val (free, total) = state.space
        Section(Modifier.clickable { state.go(Screen.Browse("")) }.testTag("storage").padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.local_files), fontSize = 16.sp, color = FilemColors.Muted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(FileRules.size(total - free), fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = FilemColors.Text)
                        Text(" / " + FileRules.size(total), Modifier.padding(start = 4.dp, bottom = 5.dp), fontSize = 16.sp, color = FilemColors.Muted)
                    }
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = FilemColors.Muted)
            }
            val used = if (total > 0) ((total - free).toFloat() / total).coerceIn(0f, 1f) else 0f
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(FilemColors.Track)) {
                Box(Modifier.fillMaxWidth(used).fillMaxHeight().clip(CircleShape).background(FilemColors.Accent))
            }
        }
        Section(Modifier.padding(vertical = 18.dp, horizontal = 6.dp)) {
            tiles.chunked(4).forEachIndexed { i, row ->
                Row(Modifier.padding(top = if (i == 0) 0.dp else 18.dp)) {
                    row.forEach { (c, color) -> CategoryTile(c, color) { state.go(Screen.CategoryList(c)) } }
                }
            }
        }
        Text(stringResource(R.string.location), Modifier.padding(start = 24.dp, top = 26.dp, bottom = 10.dp), fontSize = 15.sp,
            color = FilemColors.Muted)
        Section {
            Place(Icons.Outlined.PhoneAndroid, stringResource(R.string.my_phone), FilemColors.CatSky) { state.go(Screen.Browse("")) }
            HorizontalDivider(Modifier.padding(start = 72.dp), color = FilemColors.Tile)
            Place(Icons.Outlined.Delete, stringResource(R.string.recently_deleted), FilemColors.CatRed) { state.go(Screen.Trash) }
            HorizontalDivider(Modifier.padding(start = 72.dp), color = FilemColors.Tile)
            Place(Icons.Outlined.FavoriteBorder, stringResource(R.string.favorites), FilemColors.CatOrange) { state.go(Screen.Starred) }
        }
    }
}

@Composable private fun Section(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(FilemColors.Card).then(modifier), content = content)

@Composable private fun RowScope.CategoryTile(category: Category, color: Color, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(category), null, Modifier.size(28.dp), tint = FilemColors.OnCat)
        }
        Text(stringResource(categoryNames.getValue(category)), Modifier.padding(top = 8.dp), fontSize = 13.sp,
            color = FilemColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun Place(icon: ImageVector, title: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = FilemColors.OnCat)
        }
        Text(title, Modifier.weight(1f).padding(start = 16.dp), fontSize = 17.sp, color = FilemColors.Text)
        Icon(Icons.Outlined.ChevronRight, null, tint = FilemColors.Muted)
    }
}

/** ⋮ of «Navegador»: Configuración and Papelera (the Modo MCP is in Configuración). */
@Composable private fun MainMenu(state: FilesState) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundButton(Icons.Outlined.MoreVert, stringResource(R.string.more_options)) { open = true }
        DropdownMenu(open, { open = false }, containerColor = FilemColors.Scrim) {
            listOf(R.string.settings to Screen.Settings, R.string.trash to Screen.Trash).forEach { (label, to) ->
                DropdownMenuItem({ Text(stringResource(label)) }, { open = false; state.go(to) })
            }
        }
    }
}
