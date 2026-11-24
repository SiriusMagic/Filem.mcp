package com.zirakusa.filem.ui.screens

import com.zirakusa.filem.IconCard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.BuildConfig
import com.zirakusa.filem.R
import com.zirakusa.filem.mcp.McpMode
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.Screen
import com.zirakusa.filem.ui.theme.FilemColors

/** «Configuración»: hidden files and Modo MCP. */
@Composable fun SettingsScreen(state: FilesState) {
    Column(Modifier.fillMaxSize().background(FilemColors.Background)) {
        TopBar(stringResource(R.string.settings), { state.back() }, stringResource(R.string.back))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.show_hidden), fontSize = 18.sp, color = FilemColors.Text)
                    Text(stringResource(R.string.show_hidden_hint), color = FilemColors.Muted)
                }
                Switch(state.showHidden, state::setHidden)
            }
        }
        Card(Modifier.clickable { state.go(Screen.Mcp) }) {
            Text(stringResource(R.string.mcp_title), fontSize = 18.sp, color = FilemColors.Text)
            Text(stringResource(R.string.mcp_settings_hint), color = FilemColors.Muted)
            Text(stringResource(if (McpMode.active) R.string.mcp_status_on else R.string.mcp_status_off), color = FilemColors.Accent)
        }
        IconCard(FilemColors.Card, FilemColors.Text, FilemColors.Muted, FilemColors.Accent, androidx.compose.ui.graphics.Color(0xFF0F1A14))
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), Modifier.padding(24.dp), color = FilemColors.Muted)
    }
}

@Composable private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(FilemColors.Card).then(modifier).padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
