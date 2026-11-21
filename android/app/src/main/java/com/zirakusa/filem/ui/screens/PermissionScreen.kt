package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.filem.R
import com.zirakusa.filem.ui.theme.FilemColors

/** Before «Acceso a todos los archivos» is given: what for, and one button to Android's switch. */
@Composable fun PermissionScreen(onGrant: () -> Unit) {
    Column(Modifier.fillMaxSize().background(FilemColors.Background).padding(32.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.FolderOpen, null, Modifier.size(72.dp), tint = FilemColors.Accent)
        Text(stringResource(R.string.permission_title), Modifier.padding(top = 24.dp), fontSize = 26.sp, color = FilemColors.Text,
            textAlign = TextAlign.Center)
        Text(stringResource(R.string.permission_text), Modifier.padding(vertical = 16.dp), color = FilemColors.Muted, textAlign = TextAlign.Center)
        Button(onGrant) { Text(stringResource(R.string.permission_button)) }
    }
}
