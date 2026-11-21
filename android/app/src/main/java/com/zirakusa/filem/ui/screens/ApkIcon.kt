package com.zirakusa.filem.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.zirakusa.filem.data.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What an .apk says about itself: app name, version and icon (all null when it cannot be read). */
class ApkInfo(val label: String?, val version: String?, val icon: ImageBitmap?) {
    /** «Stone 1.7.23» under the file name. */
    val title: String? get() = label?.let { listOfNotNull(it, version).joinToString(" ") }
}

/** Reads each APK once, off the main thread, and keeps the last ones (by path and date) so scrolling is free. */
object ApkInfos {
    private const val ICON_PX = 128
    private val cache = LruCache<String, ApkInfo>(120)
    private val unreadable = ApkInfo(null, null, null)

    private fun key(file: File, modified: Long) = "${file.path}@$modified"
    fun cached(file: File, modified: Long): ApkInfo? = cache.get(key(file, modified))

    fun load(context: Context, file: File, modified: Long): ApkInfo = cached(file, modified)
        ?: (runCatching { read(context.packageManager, file.path) }.getOrNull() ?: unreadable).also { cache.put(key(file, modified), it) }

    private fun read(pm: PackageManager, path: String): ApkInfo? {
        val pkg = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
            else @Suppress("DEPRECATION") pm.getPackageArchiveInfo(path, 0)) ?: return null
        val app = pkg.applicationInfo ?: return null
        // Without these the archive's resources cannot be found, and label and icon fall back to the default ones.
        app.sourceDir = path; app.publicSourceDir = path
        return ApkInfo(app.loadLabel(pm).toString(), pkg.versionName, app.loadIcon(pm).toBitmap(ICON_PX, ICON_PX).asImageBitmap())
    }
}

/** The APK's info, loaded in the background; null meanwhile, and always for files that are not .apk. */
@Composable fun rememberApkInfo(item: FileItem, root: File): ApkInfo? {
    if (item.isDir || item.extension != "apk") return null
    val context = LocalContext.current.applicationContext
    val file = remember(root, item.path) { File(root, item.path) }
    return produceState(ApkInfos.cached(file, item.modified), file, item.modified) {
        if (value == null) value = withContext(Dispatchers.IO) { ApkInfos.load(context, file, item.modified) }
    }.value
}
