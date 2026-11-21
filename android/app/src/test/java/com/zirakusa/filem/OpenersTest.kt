package com.zirakusa.filem

import com.zirakusa.filem.data.Opener
import com.zirakusa.filem.data.Openers
import org.junit.Assert.assertEquals
import org.junit.Test

/** «Abrir con…»: our apps first by type, then the rest by name; Filem never offers itself. */
class OpenersTest {
    private val apps = listOf(Opener("org.videolan.vlc", "Main", "VLC"), Opener(Openers.STONE, "Main", "Stone"),
        Opener("com.zirakusa.filem", "Main", "Filem"), Opener(Openers.STONE_VIDEO, "Main", "Stone Video"),
        Opener("com.google.photos", "Main", "Fotos"), Opener(Openers.LUMA, "Main", "Luma"), Opener(Openers.NOTAS, "Main", "Notas"),
        Opener("org.videolan.vlc", "Main", "VLC"))

    private fun order(mime: String) = Openers.order(apps, mime, "com.zirakusa.filem").map { it.label }

    @Test fun ourAppsFirstByType() {
        assertEquals(listOf("Stone Video", "Stone", "Fotos", "Luma", "Notas", "VLC"), order("video/mp4"))
        assertEquals(listOf("Stone", "Stone Video", "Fotos", "Luma", "Notas", "VLC"), order("audio/mpeg"))
        assertEquals(listOf("Luma", "Fotos", "Notas", "Stone", "Stone Video", "VLC"), order("image/jpeg"))
        assertEquals(listOf("Notas", "Fotos", "Luma", "Stone", "Stone Video", "VLC"), order("text/markdown"))
        assertEquals(listOf("Fotos", "Luma", "Notas", "Stone", "Stone Video", "VLC"), order("application/pdf"))
        assertEquals(emptyList<String>(), Openers.ours("application/zip"))
    }

    @Test fun typesAndroidMayNotKnow() {
        assertEquals("video/mp4", Openers.mime("mp4", "video/mp4"))
        assertEquals("text/markdown", Openers.mime("MD", null))
        assertEquals(Openers.APK_MIME, Openers.mime("apk", null))
        assertEquals("*/*", Openers.mime("xyz", null))
        assertEquals("*/*", Openers.mime("", ""))
    }

    @Test fun pickingMatchesTheTypeAsked() {
        assertEquals(true, Openers.matches("audio/mpeg", "audio/*"))
        assertEquals(true, Openers.matches("audio/ogg", "AUDIO/*"))
        assertEquals(false, Openers.matches("video/mp4", "audio/*"))
        assertEquals(true, Openers.matches("image/png", "*/*"))
        assertEquals(true, Openers.matches("text/plain", null))
        assertEquals(true, Openers.matches("text/plain; charset=utf-8", "text/plain"))
        assertEquals(false, Openers.matches("audiobook/x", "audio/*"))
    }
}
