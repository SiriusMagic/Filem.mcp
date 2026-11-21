package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.ui.Screen
import com.zirakusa.filem.ui.theme.FilemColors

/** The bottom bar of the two tabs: a pill with «Reciente» and «Navegador», and the round search button apart. */
@Composable fun TabBar(current: Screen, onTab: (Screen) -> Unit, onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(FilemColors.Background).navigationBarsPadding()
        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).height(64.dp).clip(CircleShape).background(FilemColors.Card).padding(5.dp)) {
            Tab(Icons.Outlined.Schedule, stringResource(R.string.tab_recent), current == Screen.Recent) { onTab(Screen.Recent) }
            Tab(Icons.Outlined.GridView, stringResource(R.string.tab_browser), current == Screen.Navigator) { onTab(Screen.Navigator) }
        }
        Spacer(Modifier.width(12.dp))
        RoundButton(Icons.Outlined.Search, stringResource(R.string.search), size = 64, onClick = onSearch)
    }
}

@Composable private fun RowScope.Tab(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) FilemColors.Accent else FilemColors.Muted
    Column(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(if (active) FilemColors.Chip else FilemColors.Card)
        .selectable(active, role = Role.Tab, onClick = onClick), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Text(label, fontSize = 12.sp, color = tint, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** A round button on a card background (⋮, filter, search, back). */
@Composable fun RoundButton(icon: ImageVector, label: String, tint: Color = FilemColors.Text, size: Int = 48, onClick: () -> Unit) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(FilemColors.Card).clickable(role = Role.Button, onClickLabel = label,
        onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, label, tint = tint)
    }
}

/** The big title of the tabs («Navegador», «Reciente») with its round buttons on the right. */
@Composable fun BigTitle(text: String, actions: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text, Modifier.weight(1f), fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = FilemColors.Text,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}
