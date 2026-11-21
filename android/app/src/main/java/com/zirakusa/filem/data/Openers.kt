package com.zirakusa.filem.data

import android.graphics.drawable.Drawable

/** An app that can open a file («Abrir con…»). */
data class Opener(val packageName: String, val activity: String, val label: String, val icon: Drawable? = null)

/**
 * Pure rules of «Abrir con…»: our own apps first, by file type (videos → Stone Video, then Stone; music → Stone, then
 * Stone Video; photos → Luma; text → Notas), then the rest by name. Filem itself is never listed. Unit tested.
 */
object Openers {
    const val STONE = "com.zirakusa.stone.kotlin.preview"
    const val STONE_VIDEO = "com.zirakusa.stone.video"
    const val LUMA = "app.luma.gallery"
    const val NOTAS = "com.zirakusa.notas"
    const val APK_MIME = "application/vnd.android.package-archive"

    /** Types Android's table may not know. */
    private val known = mapOf("md" to "text/markdown", "markdown" to "text/markdown", "txt" to "text/plain", "log" to "text/plain",
        "csv" to "text/csv", "json" to "application/json", "kt" to "text/plain", "py" to "text/plain", "apk" to APK_MIME,
        "opus" to "audio/ogg", "mkv" to "video/x-matroska", "heic" to "image/heic")

    /** The MIME type of a file: Android's answer ([system]) when it has one, then ours, then any. */
    fun mime(extension: String, system: String?): String = system?.takeIf { it.isNotBlank() } ?: known[extension.lowercase()] ?: "*/*"

    /** Our apps for that type, in the order they are offered. */
    fun ours(mime: String): List<String> = when {
        mime.startsWith("video/") -> listOf(STONE_VIDEO, STONE)
        mime.startsWith("audio/") -> listOf(STONE, STONE_VIDEO)
        mime.startsWith("image/") -> listOf(LUMA)
        mime.startsWith("text/") || mime == "application/json" -> listOf(NOTAS)
        else -> emptyList()
    }

    fun order(apps: List<Opener>, mime: String, self: String): List<Opener> {
        val ours = ours(mime)
        return apps.filter { it.packageName != self }.distinctBy { it.packageName to it.activity }
            .sortedWith(compareBy<Opener> { ours.indexOf(it.packageName).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                .thenBy { it.label.lowercase() })
    }
}
