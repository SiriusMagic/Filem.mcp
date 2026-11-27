package com.zirakusa.filem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.data.Freshness
import com.zirakusa.filem.data.Opener
import com.zirakusa.filem.data.Openers
import com.zirakusa.filem.data.Prefs
import com.zirakusa.filem.mcp.McpMode
import com.zirakusa.filem.mcp.showMcpOptions
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilemApp
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.screens.OpenWithDialog
import com.zirakusa.filem.ui.screens.PermissionScreen
import com.zirakusa.filem.ui.theme.FilemTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Asks for «Acceso a todos los archivos», then shows Filem; opens and shares files with other apps; starts Modo MCP. */
open class MainActivity : ComponentActivity(), Actions {
    /** «Elegir archivo» for another app (see [PickActivity]): the type asked for, or null when Filem is just open. */
    protected open val pickType: String? = null
    private var granted by mutableStateOf(false)
    private lateinit var state: FilesState
    /** An APK tapped while Filem could not install apps yet: it opens when the user comes back from the setting. */
    private var pendingApk: FileItem? = null
    /** «Abrir con…» on screen: the file, its type and the apps that can open it. */
    private var opening by mutableStateOf<Triple<FileItem, String, List<Opener>>?>(null)
    private val root: File get() = Environment.getExternalStorageDirectory()
    /** Android 8–10: the classic storage permissions. */
    private val legacy = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasPermission(); if (granted) state.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppIcon.load(this)
        enableEdgeToEdge()
        val store = FileStore(root)
        state = FilesState(store, Prefs(this), lifecycleScope)
        McpMode.onChanged = { state.refresh() }
        // Listens for the whole life of the screen, but only reads files when it is visible (see schedule).
        contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, watcher)
        McpMode.load(this)
        pickType?.let { state.message = getString(R.string.pick_hint) }
        lifecycleScope.launch(Dispatchers.IO) { runCatching { store.emptyTrash(olderThanDays = 30) } }
        setContent {
            FilemTheme {
                if (granted) FilemApp(state, this, ::finish) else PermissionScreen(::askPermission)
                opening?.let { (item, type, apps) ->
                    OpenWithDialog(item.name, apps, Openers.ours(type), onPick = { app ->
                        opening = null
                        try { startActivity(view(item, type).setClassName(app.packageName, app.activity)) } catch (_: Exception) { chooser(item, type) }
                    }, onOthers = { opening = null; chooser(item, type) }, onDismiss = { opening = null })
                }
            }
        }
    }

    private val freshness = Freshness()
    private var waiting: Job? = null
    /** Android's note that files changed (MediaStore): free while nothing is written. */
    private val watcher = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { freshness.changed(); schedule() }
    }

    /** Reads again when something changed and Filem is on screen; one read for many changes in a row. */
    private fun schedule() {
        if (!granted || waiting?.isActive == true) return
        val wait = freshness.next(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED), SystemClock.elapsedRealtime()) ?: return
        waiting = lifecycleScope.launch {
            delay(wait)
            freshness.refreshed(SystemClock.elapsedRealtime())
            state.refreshQuietly()
        }
    }

    override fun onStart() { super.onStart(); schedule() }

    override fun onDestroy() { contentResolver.unregisterContentObserver(watcher); super.onDestroy() }

    override fun onResume() {
        super.onResume()
        val now = hasPermission()
        if (now && !granted) state.refresh()
        granted = now
        pendingApk?.let { apk -> pendingApk = null; if (packageManager.canRequestPackageInstalls()) install(apk) }
    }

    private fun hasPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val app = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:$packageName".toUri())
            try { startActivity(app) } catch (_: ActivityNotFoundException) { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        } else legacy.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    private fun uri(path: String) = FileProvider.getUriForFile(this, "$packageName.files", File(root, path))
    private fun mime(name: String) = name.substringAfterLast('.', "").lowercase().let {
        Openers.mime(it, MimeTypeMap.getSingleton().getMimeTypeFromExtension(it))
    }
    private fun view(item: FileItem, type: String) = Intent(Intent.ACTION_VIEW).setDataAndType(uri(item.path), type)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    /** Apps that open this type (visible through the manifest's <queries>), ours first. */
    private fun openers(intent: Intent, type: String): List<Opener> {
        val found = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            else @Suppress("DEPRECATION") packageManager.queryIntentActivities(intent, 0)
        return Openers.order(found.map {
            Opener(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(packageManager).toString(),
                runCatching { it.loadIcon(packageManager) }.getOrNull())
        }, type, packageName)
    }

    /** Android's own chooser («Otras apps»). */
    private fun chooser(item: FileItem, type: String) {
        try { startActivity(Intent.createChooser(view(item, type), item.name)) }
        catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }
    }

    /** «Abrir con…»: our apps for that type first (Stone Video, Stone, Luma, Notas), then the rest; APKs install. */
    override fun open(item: FileItem) {
        pickType?.let { wanted -> deliver(item, wanted); return }
        if (item.extension == "apk") { install(item); return }
        val type = mime(item.name)
        val apps = runCatching { openers(view(item, type), type) }.getOrDefault(emptyList())
        if (apps.isEmpty()) chooser(item, type) else opening = Triple(item, type, apps)
    }

    /** «Elegir archivo»: hands the tapped file back to the app that asked (read once), if it is of the type it wants. */
    private fun deliver(item: FileItem, wanted: String) {
        val type = mime(item.name)
        if (!Openers.matches(type, wanted)) { Toast.makeText(this, R.string.pick_wrong_type, Toast.LENGTH_SHORT).show(); return }
        val uri = uri(item.path)
        setResult(RESULT_OK, Intent().setDataAndType(uri, type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri(item.name, uri) })
        finish()
    }

    /** Tapping an APK installs it with the phone's installer; the first time Android asks to allow Filem. */
    private fun install(item: FileItem) {
        if (!packageManager.canRequestPackageInstalls()) {
            pendingApk = item
            Toast.makeText(this, R.string.install_allow, Toast.LENGTH_LONG).show()
            try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:$packageName".toUri())) }
            catch (_: ActivityNotFoundException) { pendingApk = null }
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri(item.path), Openers.APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }
    }

    override fun share(paths: List<String>) {
        val files = paths.filter { File(root, it).isFile }
        if (files.isEmpty()) { Toast.makeText(this, R.string.share_folders, Toast.LENGTH_SHORT).show(); return }
        val uris = ArrayList(files.map(::uri))
        val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0]).setType(mime(files[0]))
            else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris).setType("*/*")
        intent.clipData = ClipData.newRawUri("", uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        startActivity(Intent.createChooser(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
    }

    override fun openArka() = showMcpOptions(this)
}

/**
 * «Elegir archivo» (ACTION_GET_CONTENT): Reloj's «Añadir nuevo», or any app, opens Filem to pick one file. Same
 * screens; tapping a file of the type asked returns it and closes. A separate activity, so the answer reaches the app.
 */
class PickActivity : MainActivity() {
    override val pickType: String get() = intent?.type ?: "*/*"
}
