package com.zirakusa.filem.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zirakusa.filem.R
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.ui.theme.FilemColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val categoryNames = mapOf(Category.DOWNLOADS to R.string.cat_downloads, Category.RECEIVED to R.string.cat_received,
    Category.IMAGES to R.string.cat_images, Category.VIDEOS to R.string.cat_videos, Category.AUDIO to R.string.cat_audio,
    Category.DOCUMENTS to R.string.cat_documents, Category.APPS to R.string.cat_apps, Category.ZIPS to R.string.cat_zips)

fun categoryIcon(category: Category?): ImageVector = when (category) {
    Category.DOWNLOADS -> Icons.Outlined.Download
    Category.RECEIVED -> Icons.Outlined.MoveToInbox
    Category.ZIPS -> Icons.Outlined.FolderZip
    Category.IMAGES -> Icons.Outlined.Image
    Category.VIDEOS -> Icons.Outlined.Movie
    Category.AUDIO -> Icons.Outlined.MusicNote
    Category.DOCUMENTS -> Icons.Outlined.Description
    Category.APPS -> Icons.Outlined.Android
    null -> Icons.AutoMirrored.Outlined.InsertDriveFile
}

/** "Hace 8 horas" this week, "29 oct" this year, "17 abr 2025" before, in the phone's language. */
fun friendlyDate(ms: Long, now: Long = System.currentTimeMillis()): String {
    if (now - ms in 0 until 7 * DateUtils.DAY_IN_MILLIS) return DateUtils.getRelativeTimeSpanString(ms, now, DateUtils.MINUTE_IN_MILLIS).toString()
    val year = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)
    val thisYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
    return SimpleDateFormat(if (year == thisYear) "d MMM" else "d MMM yyyy", Locale.getDefault()).format(Date(ms)).replace(".", "")
}

/** Folder, photo thumbnail, the app's own icon (APK) or the category icon, on a rounded tile. */
@Composable fun FileIcon(item: FileItem, root: File, size: Dp, modifier: Modifier = Modifier, apk: ApkInfo? = rememberApkInfo(item, root)) {
    val icon = apk?.icon
    Box(modifier.size(size).clip(RoundedCornerShape(size / 4)).background(FilemColors.Tile), contentAlignment = Alignment.Center) {
        if (item.category == Category.IMAGES) AsyncImage(File(root, item.path), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (icon != null) Image(icon, null, Modifier.size(size * 0.74f))
        else Icon(if (item.isDir) Icons.Filled.Folder else categoryIcon(item.category), null, Modifier.size(size * 0.38f),
            tint = FilemColors.Folder)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun FileRow(item: FileItem, root: File, selected: Boolean, selecting: Boolean, onClick: () -> Unit,
    onLongClick: () -> Unit, menu: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .background(if (selected) FilemColors.Chip else FilemColors.Background).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val apk = rememberApkInfo(item, root)
        Box { FileIcon(item, root, 64.dp, apk = apk); if (selected) Check(Modifier.align(Alignment.BottomEnd)) }
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Text(item.name, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = FilemColors.Text)
            Text(listOfNotNull(apk?.title, subtitle(item)).joinToString(" • "), fontSize = 14.sp, color = FilemColors.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!selecting) menu()
    }
}

fun subtitle(item: FileItem) = if (item.isDir) friendlyDate(item.modified) else "${FileRules.size(item.size)} • ${friendlyDate(item.modified)}"

@OptIn(ExperimentalFoundationApi::class)
@Composable fun FileTile(item: FileItem, root: File, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.padding(3.dp).clip(RoundedCornerShape(18.dp)).background(if (selected) FilemColors.Chip else FilemColors.Card)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(bottom = 14.dp)) {
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            FileIcon(item, root, 64.dp)
            if (selected) Check(Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Text(item.name, Modifier.padding(horizontal = 14.dp), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = FilemColors.Text)
    }
}

@Composable private fun Check(modifier: Modifier) = Icon(Icons.Filled.CheckCircle, null, modifier.size(22.dp), tint = FilemColors.Accent)

/** Round title bar of the inner screens: back, title, then [actions]. */
@Composable fun TopBar(title: String, back: () -> Unit, backLabel: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(FilemColors.Bar).statusBarsPadding().height(72.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, backLabel, tint = FilemColors.Text) }
        Text(title, Modifier.weight(1f).padding(start = 8.dp), fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = FilemColors.Text)
        actions()
    }
}
