package com.zirakusa.filem

import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.ui.FilesState
import com.zirakusa.filem.ui.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The screen state: navigation and back, new folder, selection, copy/move with «Pegar aquí», trash and errors. */
@RunWith(RobolectricTestRunner::class)
class FilesStateTest {
    @get:Rule val tmp = TemporaryFolder()
    private val root by lazy { tmp.newFolder("storage").also { File(it, "Download").mkdirs(); File(it, "Download/a.pdf").writeText("pdf") } }
    private val state by lazy { FilesState(FileStore(root), null, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined) }

    @Test fun browseCreateAndGoBack() {
        state.go(Screen.Browse(""))
        assertEquals(listOf("Download"), state.items.map { it.name })
        state.open(state.items.first())
        assertEquals(listOf("a.pdf"), state.items.map { it.name })
        state.createFolder("Facturas")
        assertEquals(listOf("Facturas", "a.pdf"), state.items.map { it.name })
        state.createFolder("Facturas")
        assertTrue(state.message!!.contains("already exists"))
        assertTrue(state.back()); assertEquals(Screen.Browse(""), state.screen)
        assertTrue(state.back()); assertEquals(Screen.Home, state.screen)
        assertFalse(state.back())
    }

    @Test fun selectMoveAndTrash() {
        state.go(Screen.Browse("Download"))
        state.createFolder("Docs")
        state.toggle(state.items.first { it.name == "a.pdf" })
        state.copy(move = true)
        assertTrue(state.selection.isEmpty())
        state.go(Screen.Browse("Download/Docs"))
        state.paste()
        assertNull(state.clip)
        assertEquals(listOf("a.pdf"), state.items.map { it.name })
        state.selectAll(); state.trashSelected()
        assertTrue(state.items.isEmpty())
        state.go(Screen.Trash)
        assertEquals(listOf("a.pdf"), state.trash.map { it.name })
        state.restore(state.trash.first())
        assertTrue(File(root, "Download/Docs/a.pdf").exists())
    }

    @Test fun homeCountsAndStars() {
        state.refresh()
        assertEquals(listOf("a.pdf"), state.recents.map { it.name })
        state.go(Screen.Browse("Download"))
        state.star(state.items.first())
        state.go(Screen.Starred)
        assertEquals(listOf("a.pdf"), state.items.map { it.name })
    }
}
