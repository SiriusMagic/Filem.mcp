package com.zirakusa.filem

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.ui.Actions
import com.zirakusa.filem.ui.FilemApp
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.screens.PermissionScreen
import com.zirakusa.filem.ui.theme.FilemTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
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
        override fun copyLink(text: String) {}
        override fun goToHub() {}
    }

    private fun storage() = tmp.newFolder("storage").also { root ->
        listOf("Alarms", "Android", "Audiobooks", "bbb", "codex bot caja", "DCIM", "Documents", "Download", "Movies", "Music")
            .forEach { File(root, it).mkdirs() }
        File(root, "Download/build.txt").writeText("30 bytes de texto de prueba..")
        File(root, "Documents/README.md").writeText("# Filem")
        File(root, "Music/cancion.mp3").writeBytes(ByteArray(2048))
    }

    @Test fun homeFolderGridAndSelection() {
        val state = FilesState(FileStore(storage()), null, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined)
        state.refresh()
        compose.setContent { FilemTheme { FilemApp(state, actions) {} } }
        compose.onNodeWithText("Categorías").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-home.png")
        compose.onNodeWithText("Almacenamiento interno").performScrollTo().performClick()
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

    @Test fun searchAndPermission() {
        val state = FilesState(FileStore(storage()), null, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined)
        state.refresh()
        compose.setContent { FilemTheme { FilemApp(state, actions) {} } }
        compose.onNodeWithText("Buscar en Filem").performClick()
        compose.onNodeWithText("README.md").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-search.png")
    }

    @Test fun permissionScreen() {
        compose.setContent { FilemTheme { PermissionScreen {} } }
        compose.onNodeWithText("Dar acceso").assertExists()
        compose.onRoot().captureRoboImage("build/screenshots/filem-permission.png")
    }
}
