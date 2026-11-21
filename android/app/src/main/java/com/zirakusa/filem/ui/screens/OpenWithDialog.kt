package com.zirakusa.filem.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.zirakusa.filem.R
import com.zirakusa.filem.data.Opener
import com.zirakusa.filem.ui.theme.FilemColors

/** «Abrir con…»: our apps for that type first (marked), the other apps, and «Otras apps» (Android's chooser). */
@Composable fun OpenWithDialog(name: String, apps: List<Opener>, ours: List<String>, onPick: (Opener) -> Unit, onOthers: () -> Unit,
    onDismiss: () -> Unit) {
    Dialog(onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = FilemColors.Scrim) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(stringResource(R.string.open_with), Modifier.padding(horizontal = 24.dp), fontSize = 22.sp, color = FilemColors.Text)
                Text(name, Modifier.padding(horizontal = 24.dp, vertical = 4.dp), color = FilemColors.Muted, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                    items(apps, key = { it.packageName + "/" + it.activity }) { app ->
                        val icon = remember(app) { app.icon?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() } }
                        Row(Modifier.fillMaxWidth().clickable { onPick(app) }.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (icon != null) Image(icon, null, Modifier.size(40.dp))
                            else Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FilemColors.Tile))
                            Column(Modifier.padding(start = 16.dp)) {
                                Text(app.label, fontSize = 17.sp, color = FilemColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (app.packageName in ours) Text(stringResource(R.string.open_with_ours), color = FilemColors.Accent, fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().clickable(onClick = onOthers).padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(FilemColors.Tile), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Apps, null, tint = FilemColors.Text)
                            }
                            Text(stringResource(R.string.other_apps), Modifier.padding(start = 16.dp), fontSize = 17.sp, color = FilemColors.Text)
                        }
                    }
                }
                TextButton(onDismiss, Modifier.align(Alignment.End).padding(end = 16.dp)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}
