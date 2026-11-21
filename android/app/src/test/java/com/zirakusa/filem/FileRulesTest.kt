package com.zirakusa.filem

import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.data.Sort
import com.zirakusa.filem.data.SortBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileRulesTest {
    @Test fun namesAreChecked() {
        assertNull(FileRules.nameProblem("Fotos 2026"))
        assertEquals("EMPTY", FileRules.nameProblem("  "))
        assertEquals("RESERVED", FileRules.nameProblem(".."))
        assertEquals("BAD_CHARACTER", FileRules.nameProblem("a/b"))
        assertEquals("SPACES", FileRules.nameProblem(" a"))
        assertEquals("TOO_LONG", FileRules.nameProblem("x".repeat(256)))
    }

    @Test fun pathsNeverLeaveTheStorage() {
        assertEquals("Download/a", FileRules.cleanPath("/Download//a/"))
        assertEquals("", FileRules.cleanPath("/"))
        assertNull(FileRules.cleanPath("Download/../../data"))
        assertNull(FileRules.cleanPath(".filem/papelera"))
        assertNull(FileRules.cleanPath("a\\b"))
        assertTrue(FileRules.isInside("a/b", "a")); assertFalse(FileRules.isInside("ab", "a"))
    }

    @Test fun foldersFirstAndNaturalOrder() {
        fun f(name: String, dir: Boolean = false, size: Long = 0, at: Long = 0) = FileItem(name, name, dir, size, at)
        val items = listOf(f("foto10.jpg", size = 5, at = 3), f("foto2.jpg", size = 9, at = 1), f("Música", true), f("apk.apk", size = 1, at = 2))
        assertEquals(listOf("Música", "apk.apk", "foto2.jpg", "foto10.jpg"), FileRules.sorted(items, Sort()).map { it.name })
        assertEquals(listOf("Música", "foto2.jpg", "foto10.jpg", "apk.apk"), FileRules.sorted(items, Sort(SortBy.SIZE, true)).map { it.name })
        assertEquals(listOf("Música", "foto10.jpg", "apk.apk", "foto2.jpg"), FileRules.sorted(items, Sort(SortBy.DATE, true)).map { it.name })
    }

    @Test fun categoriesSearchAndNames() {
        assertEquals(Category.IMAGES, FileRules.category("IMG_1.JPG"))
        assertEquals(Category.APPS, FileRules.category("Stone.apk"))
        assertNull(FileRules.category("README")); assertNull(FileRules.category(".nomedia"))
        assertTrue(FileRules.matches("Canción Final.mp3", "cancion final"))
        assertFalse(FileRules.matches("Canción.mp3", "otra"))
        val taken = setOf("foto.jpg", "foto (1).jpg")
        assertEquals("foto (2).jpg", FileRules.uniqueName("foto.jpg") { it in taken })
        assertEquals("Carpeta", FileRules.uniqueName("Carpeta") { false })
    }

    @Test fun sizesLookLikeTheFilesApp() {
        assertEquals("30 B", FileRules.size(30))
        assertEquals("1,2 GB", FileRules.size(1_200_000_000))
        assertEquals("641 kB", FileRules.size(641_000))
        assertEquals("1 MB", FileRules.size(1_000_000))
    }

    @Test fun zipsAndPlaces() {
        listOf("fotos.zip", "a.RAR", "b.7z", "c.tar", "d.tar.gz").forEach { assertEquals(it, Category.ZIPS, FileRules.category(it)) }
        assertTrue(Category.RECEIVED.isPlace && Category.DOWNLOADS.isPlace && !Category.ZIPS.isPlace)
        assertTrue("Download" in Category.RECEIVED.folders && "Bluetooth" in Category.RECEIVED.folders)
        assertEquals(Category.ZIPS, Category.from("zips")); assertEquals(Category.RECEIVED, Category.from("received"))
        assertEquals(listOf("Download", "Bluetooth"), FileRules.outermost(listOf("Download", "Download/Telegram", "Bluetooth", "Download")))
        assertEquals(listOf("Telegram", "Pictures/Telegram"), FileRules.outermost(listOf("Telegram", "Pictures/Telegram")))
    }

    @Test fun searchHistoryKeepsTheNewestTen() {
        var h = emptyList<String>()
        h = FileRules.addToHistory(h, " informe "); h = FileRules.addToHistory(h, "fotos"); h = FileRules.addToHistory(h, "INFORME")
        assertEquals(listOf("INFORME", "fotos"), h)
        assertEquals(h, FileRules.addToHistory(h, "   "))
        (1..15).forEach { h = FileRules.addToHistory(h, "q$it") }
        assertEquals(10, h.size); assertEquals("q15", h.first()); assertEquals("q6", h.last())
    }
}
