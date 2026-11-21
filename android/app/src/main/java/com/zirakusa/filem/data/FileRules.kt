package com.zirakusa.filem.data

import java.text.Normalizer
import java.util.Locale

/** Pure rules, no Android: names, safe paths, order, search and sizes. Unit tested. */
object FileRules {
    const val NAME_MAX = 255
    /** Filem's own hidden folder on the storage (the trash lives here); never listed, never reachable by MCP. */
    const val APP_DIR = ".filem"

    /** Why a new name is refused, or null when it is fine. */
    fun nameProblem(name: String): String? = when {
        name.isBlank() -> "EMPTY"
        name.length > NAME_MAX -> "TOO_LONG"
        name == "." || name == ".." -> "RESERVED"
        name.any { it == '/' || it == '\\' || it == '\u0000' || it.code < 32 } -> "BAD_CHARACTER"
        name.trim() != name -> "SPACES"
        else -> null
    }

    /**
     * A relative path asked by the user or an AI, cleaned ("a//b/" → "a/b"), or null when it would leave the root
     * ("..", absolute tricks) or touch Filem's own folder.
     */
    fun cleanPath(path: String): String? {
        if ('\u0000' in path || '\\' in path) return null
        val parts = path.split('/').filter { it.isNotEmpty() && it != "." }
        if (parts.any { it == ".." }) return null
        if (parts.firstOrNull() == APP_DIR) return null
        return parts.joinToString("/")
    }

    fun parent(path: String) = path.substringBeforeLast('/', "")
    fun join(dir: String, name: String) = if (dir.isEmpty()) name else "$dir/$name"

    fun category(name: String): Category? {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty() || ext == name.lowercase()) return null
        return Category.entries.firstOrNull { ext in it.extensions }
    }

    /** Folders first, then the chosen order; names compare naturally ("2" before "10") and without case. */
    fun sorted(items: List<FileItem>, sort: Sort): List<FileItem> {
        val byName = Comparator<FileItem> { a, b -> natural(a.name, b.name) }
        val chosen: Comparator<FileItem> = when (sort.by) {
            SortBy.NAME -> byName
            SortBy.DATE -> compareBy<FileItem> { it.modified }.then(byName)
            SortBy.SIZE -> compareBy<FileItem> { if (it.isDir) it.children.toLong() else it.size }.then(byName)
            SortBy.TYPE -> compareBy<FileItem> { it.extension }.then(byName)
        }
        val directed = if (sort.descending) chosen.reversed() else chosen
        return items.sortedWith(compareByDescending<FileItem> { it.isDir }.then(directed))
    }

    fun natural(a: String, b: String): Int {
        val ra = Regex("\\d+|\\D+").findAll(a.lowercase()).map { it.value }.toList()
        val rb = Regex("\\d+|\\D+").findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(ra.size, rb.size)) {
            val x = ra[i]; val y = rb[i]
            val c = if (x[0].isDigit() && y[0].isDigit()) x.trimStart('0').length.compareTo(y.trimStart('0').length)
                .takeIf { it != 0 } ?: x.trimStart('0').compareTo(y.trimStart('0')) else x.compareTo(y)
            if (c != 0) return c
        }
        return ra.size.compareTo(rb.size)
    }

    /** Lower case and without accents, so «cancion» finds «Canción». */
    fun fold(text: String): String = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    /** Every word of the query is in the name. */
    fun matches(name: String, query: String): Boolean {
        val words = fold(query).split(' ').filter { it.isNotBlank() }
        val target = fold(name)
        return words.isNotEmpty() && words.all { it in target }
    }

    /** "foto.jpg" taken → "foto (1).jpg", then "foto (2).jpg"… */
    fun uniqueName(name: String, taken: (String) -> Boolean): String {
        if (!taken(name)) return name
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val base = name.substring(0, dot); val ext = name.substring(dot)
        return generateSequence(1) { it + 1 }.map { "$base ($it)$ext" }.first { !taken(it) }
    }

    /** "1,2 GB", "641 kB", "30 B": the same style as the phone's Files (decimal units, comma in Spanish). */
    fun size(bytes: Long, locale: Locale = Locale.forLanguageTag("es")): String {
        if (bytes < 1000) return "$bytes B"
        val units = listOf("kB", "MB", "GB", "TB")
        var value = bytes / 1000.0; var unit = 0
        while (value >= 1000 && unit < units.lastIndex) { value /= 1000; unit++ }
        val text = if (value >= 100) "%.0f".format(locale, value) else "%.1f".format(locale, value).removeSuffix(",0").removeSuffix(".0")
        return "$text ${units[unit]}"
    }

    /** True when [child] is the same as, or inside, [folder] (both relative): a folder cannot go into itself. */
    fun isInside(child: String, folder: String) = child == folder || child.startsWith("$folder/")
}
