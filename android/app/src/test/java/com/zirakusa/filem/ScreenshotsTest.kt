package com.zirakusa.filem

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.zirakusa.filem.data.Opener
import com.zirakusa.filem.data.Openers
import com.zirakusa.filem.ui.screens.OpenWithDialog
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilemApp
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.screens.PermissionScreen
import com.zirakusa.filem.ui.theme.FilemColors
import com.zirakusa.filem.ui.theme.FilemTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The main screens drawn on a phone, saved to app/build/screenshots, and the taps that move between them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val tmp = TemporaryFolder()
    private val opened = mutableListOf<String>()
    private val actions = object : Actions {
        override fun open(item: FileItem) { opened += item.path }
        override fun share(paths: List<String>) {}
        override fun openArka() {}
    }

    private fun storage() = tmp.newFolder("storage").also { root ->
        listOf("Alarms", "Android", "Audiobooks", "bbb", "codex bot caja", "DCIM", "Documents", "Download", "Movies", "Music")
            .forEach { File(root, it).mkdirs() }
        val now = System.currentTimeMillis()
        fun put(path: String, bytes: Int, daysAgo: Int) = File(root, path).apply { parentFile!!.mkdirs(); writeBytes(ByteArray(bytes)) }
            .setLastModified(now - daysAgo * 86_400_000L - 60_000)
        File(root, "Download/build.txt").writeText("30 bytes de texto de prueba..")
        File(root, "Documents/README.md").writeText("# Filem")
        put("Music/cancion.mp3", 2048, 0)
        put("Download/Stone-1.7.23.apk", 9_400_000, 0)
        put("Download/fotos-viaje.zip", 52_000_000, 1)
        put("Documents/informe final.pdf", 640_000, 1)
        put("Movies/video.mp4", 120_000_000, 4)
        put("Documents/notas.txt", 900, 12)
    }

    private fun state() = FilesState(FileStore(storage()), null, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined)
        .also { it.refresh() }

    /** Ajustes › Icono de la app: the five folders, «Plano» chosen (the default). */
    @Test fun appIcons() {
        compose.setContent { FilemTheme { Box(Modifier.padding(16.dp)) {
            IconCard(FilemColors.Card, FilemColors.Text, FilemColors.Muted, FilemColors.Accent)
        } } }
        compose.onNodeWithText("Plano").assertExists()
        compose.onNodeWithText("Suave").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-icons.png")
    }

    @Test fun navigatorFolderGridAndSelection() {
        val state = state()
        compose.setContent { FilemTheme { FilemApp(state, actions) {} } }
        compose.onNodeWithText("Archivos locales").assertExists()
        compose.onNodeWithText("Ubicación").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-navigator.png")
        compose.onNodeWithText("Mi teléfono").performScrollTo().performClick()
        compose.onNodeWithText("codex bot caja").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-folder-list.png")
        compose.onNodeWithContentDescription("Ver en cuadrícula").performClick()
        compose.onNodeWithTag("grid").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-folder-grid.png")
        compose.onNodeWithText("Download").performClick()
        compose.onNodeWithText("build.txt").performClick()
        assertEquals(listOf("Download/build.txt"), opened)
        compose.onNodeWithText("build.txt").performTouchInput { longClick() }
        compose.onNodeWithText("1 seleccionado").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-selection.png")
    }

    @Test fun categoriesOpenTheirLists() {
        val state = state()
        compose.setContent { FilemTheme { FilemApp(state, actions) {} } }
        compose.onNodeWithText("Zips").performClick()
        compose.onNodeWithText("fotos-viaje.zip").assertExists()
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithText("APKs").performClick()
        compose.onNodeWithText("Stone-1.7.23.apk").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-apks.png")
        compose.onNodeWithText("Stone-1.7.23.apk").performClick()
        assertEquals(listOf("Download/Stone-1.7.23.apk"), opened)
    }

    @Test fun recentGroups() {
        val state = state()
        compose.setContent { FilemTheme { FilemApp(state, actions) {} } }
        compose.onNodeWithText("Reciente").performClick()
        compose.onNodeWithText("Hoy").assertExists()
        compose.onNodeWithText("cancion.mp3").assertExists()
        compose.onNodeWithText("Ayer").performClick()
        compose.onNodeWithText("fotos-viaje.zip").assertExists()
        // build.txt and README.md are new too: 8 files in the last 30 days.
        compose.onNodeWithTag("recent").performScrollToNode(hasText("8 elementos · Se muestran solo los archivos añadidos en los últimos 30 días"))
        compose.onNodeWithTag("recent").performScrollToIndex(0)
        compose.onRoot().captureRoboImage("build/screenshots/filem-recent.png")
        compose.onNodeWithText("Navegador").performClick()
        compose.onNodeWithText("Archivos locales").assertExists()
    }

    @Test fun searchWithHistory() {
        val state = state()
        state.rememberSearch("informe"); state.rememberSearch("fotos"); state.rememberSearch("Stone")
        compose.setContent { FilemTheme { FilemApp(state, actions) {} } }
        compose.onNodeWithContentDescription("Buscar").performClick()
        compose.onNodeWithText("Buscar historial").assertExists()
        compose.onNodeWithText("README.md").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-search.png")
        compose.onNodeWithText("fotos").performClick()
        compose.onNodeWithText("fotos-viaje.zip").performClick()
        assertEquals(listOf("fotos", "Stone", "informe"), state.history)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test fun openWithPutsOurAppsFirst() {
        val apps = Openers.order(listOf(Opener("org.videolan.vlc", "Main", "VLC"), Opener(Openers.STONE, "Main", "Stone"),
            Opener(Openers.STONE_VIDEO, "Main", "Stone Video")), "video/mp4", "com.zirakusa.filem")
        val picked = mutableListOf<String>(); var others = false
        compose.setContent { FilemTheme { OpenWithDialog("video.mp4", apps, Openers.ours("video/mp4"), { picked += it.label }, { others = true }) {} } }
        compose.onNodeWithText("Abrir con…").assertExists()
        captureScreenRoboImage("build/screenshots/filem-open-with.png")
        compose.onNodeWithText("Stone Video").performClick()
        compose.onNodeWithText("Otras apps").performClick()
        assertEquals(listOf("Stone Video"), picked); assertTrue(others)
    }

    @Test fun permissionScreen() {
        compose.setContent { FilemTheme { PermissionScreen {} } }
        compose.onNodeWithText("Dar acceso").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-permission.png")
    }
}
