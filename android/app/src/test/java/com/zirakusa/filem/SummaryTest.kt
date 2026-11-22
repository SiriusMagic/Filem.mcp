package com.zirakusa.filem

import com.zirakusa.filem.data.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Filem's card in Inicio: the newest documents with their size; never folders, other files or the trash. */
class SummaryTest {
    @Test fun newestDocuments() {
        val items = listOf(FileItem("Download/a.pdf", "a.pdf", false, 1_234_567, 10), FileItem("Docs/notas.txt", "notas.txt", false, 900, 30),
            FileItem("DCIM/foto.jpg", "foto.jpg", false, 5, 40), FileItem(".filem/trash/b.pdf", "b.pdf", false, 5, 50),
            FileItem("Docs", "Docs", true, 0, 60))
        val lines = SummaryLines.lines(items)
        assertEquals(2, lines.size); assertTrue(lines[0].startsWith("notas.txt · ")); assertTrue(lines[1].startsWith("a.pdf · 1"))
        assertTrue(SummaryLines.lines(emptyList()).isEmpty())
        assertEquals(SummaryLines.MAX, SummaryLines.lines(List(9) { FileItem("d$it.pdf", "d$it.pdf", false, 1, it.toLong()) }).size)
    }
}
