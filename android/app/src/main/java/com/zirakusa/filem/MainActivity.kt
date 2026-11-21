package com.zirakusa.filem

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.data.Prefs
import com.zirakusa.filem.mcp.McpMode
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilemApp
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.screens.PermissionScreen
import com.zirakusa.filem.ui.theme.FilemTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** Asks for «Acceso a todos los archivos», then shows Filem; opens and shares files with other apps; starts Modo MCP. */
class MainActivity : ComponentActivity(), Actions {
    private var granted by mutableStateOf(false)
    private lateinit var state: FilesState
    private val root: File get() = Environment.getExternalStorageDirectory()
    /** Android 8–10: the classic storage permissions. */
    private val legacy = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasPermission(); if (granted) state.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = FileStore(root)
        state = FilesState(store, Prefs(this), lifecycleScope)
        McpMode.onChanged = { state.refresh() }
        McpMode.load(this)
        lifecycleScope.launch(Dispatchers.IO) { runCatching { store.emptyTrash(olderThanDays = 30) } }
        setContent {
            FilemTheme {
                if (granted) FilemApp(state, this, ::finish) else PermissionScreen(::askPermission)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val now = hasPermission()
        if (now && !granted) state.refresh()
        granted = now
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
    private fun mime(name: String) = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase()) ?: "*/*"

    /** «Abrir»: the app the user picks for that type (Stone for music and video, Luma for photos…). */
    override fun open(item: FileItem) {
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri(item.path), mime(item.name)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(Intent.createChooser(intent, item.name)) }
        catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }
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

    override fun copyLink(text: String) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Filem MCP", text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    /** «Ir al Hub»: copies the hub's link (in case it must be pasted) and opens the Gateway Hub app. */
    override fun goToHub() {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Filem › Gateway Hub", McpMode.hubLink()))
        val open = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (open != null) startActivity(open) else Toast.makeText(this, R.string.mcp_paste_in_hub, Toast.LENGTH_LONG).show()
    }
}
