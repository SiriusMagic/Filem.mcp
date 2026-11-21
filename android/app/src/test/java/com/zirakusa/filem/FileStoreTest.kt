package com.zirakusa.filem

import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileFailure
import com.zirakusa.filem.data.FileStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Real files in a temporary «storage»: list, create, rename, copy, move, trash and restore, and nothing escapes. */
@RunWith(RobolectricTestRunner::class)
class FileStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val root by lazy { tmp.newFolder("storage") }
    private var clock = 1_000_000L
    private val store by lazy { FileStore(root) { clock } }

    private fun file(path: String, text: String = "x") = File(root, path).apply { parentFile!!.mkdirs(); writeText(text) }

    @Test fun listsFoldersFirstAndHidesTheAppFolderAndDotFiles() {
        file("Download/b.pdf"); file("a.txt"); file(".nomedia"); File(root, ".filem").mkdirs()
        assertEquals(listOf("Download", "a.txt"), store.list("").map { it.name })
        assertEquals(1, store.list("").first().children)
        store.showHidden = true
        assertEquals(listOf("Download", ".nomedia", "a.txt"), store.list("").map { it.name })
    }

    @Test fun createRenameCopyMove() {
        file("Docs/nota.txt", "hola")
        assertEquals("Docs/Fotos", store.createFolder("Docs", "Fotos").path)
        assertEquals("ALREADY_EXISTS", assertThrows(FileFailure::class.java) { store.createFolder("Docs", "Fotos") }.code)
        assertEquals("BAD_NAME", assertThrows(FileFailure::class.java) { store.createFolder("Docs", "a/b") }.code)
        assertEquals("Docs/apunte.txt", store.rename("Docs/nota.txt", "apunte.txt").path)
        store.transfer(listOf("Docs/apunte.txt"), "Docs/Fotos", move = false)
        store.transfer(listOf("Docs/apunte.txt"), "Docs/Fotos", move = false)
        assertEquals(listOf("apunte (1).txt", "apunte.txt"), store.list("Docs/Fotos").map { it.name })
        store.transfer(listOf("Docs/Fotos"), "", move = true)
        assertTrue(File(root, "Fotos/apunte.txt").exists()); assertFalse(File(root, "Docs/Fotos").exists())
        assertEquals("INTO_ITSELF", assertThrows(FileFailure::class.java) { store.transfer(listOf("Docs"), "Docs", move = true) }.code)
    }

    @Test fun trashRestoresAndExpires() {
        file("Music/song.mp3", "abc")
        val entry = store.trash("Music/song.mp3")
        assertFalse(File(root, "Music/song.mp3").exists())
        assertEquals(listOf("song.mp3"), store.trashed().map { it.name })
        assertEquals("Music/song.mp3", store.restore(entry.id).path)
        store.trash("Music/song.mp3")
        clock += 31 * 86_400_000L
        assertEquals(1, store.emptyTrash(olderThanDays = 30))
        assertTrue(store.trashed().isEmpty())
    }

    @Test fun nothingOutsideTheStorage() {
        tmp.newFile("secret.txt")
        listOf("../secret.txt", ".filem/trash.json", "a/../../secret.txt").forEach {
            assertEquals("BAD_PATH", assertThrows(FileFailure::class.java) { store.info(it) }.code)
        }
        assertEquals("NOT_FOUND", assertThrows(FileFailure::class.java) { store.info("nada.txt") }.code)
    }

    @Test fun searchAndCategories() {
        file("DCIM/Cancion final.jpg"); file("Download/informe final.pdf", "12345"); file("Android/data/x.jpg")
        assertEquals(listOf("DCIM/Cancion final.jpg"), store.search("FINAL cancion").map { it.path })
        assertEquals(listOf("Download/informe final.pdf"), store.search("", Category.DOCUMENTS).map { it.path })
        val cats = store.categories()
        assertEquals(1, cats.getValue(Category.IMAGES).second) // Android/ is skipped
        assertEquals(5L to 1, cats.getValue(Category.DOWNLOADS))
    }
}
