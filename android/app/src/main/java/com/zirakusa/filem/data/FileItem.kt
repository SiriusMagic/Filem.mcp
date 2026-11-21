package com.zirakusa.filem.data

/** One file or folder as the screens and the MCP tools see it. [path] is relative to the storage root ("" = root). */
data class FileItem(val path: String, val name: String, val isDir: Boolean, val size: Long, val modified: Long,
    val children: Int = 0) {
    val category: Category? get() = if (isDir) null else FileRules.category(name)
    val extension: String get() = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
}

/** The home screen's «Categorías». Matched by extension, so they work the same for MCP, search and the phone. */
enum class Category(val key: String, val extensions: Set<String>) {
    DOWNLOADS("downloads", emptySet()),
    IMAGES("images", setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "svg", "dng", "raw")),
    VIDEOS("videos", setOf("mp4", "mkv", "webm", "3gp", "mov", "avi", "m4v", "ts", "flv", "wmv")),
    AUDIO("audio", setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "amr", "mid", "wma")),
    DOCUMENTS("documents", setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt",
        "md", "csv", "rtf", "epub", "json", "xml", "html", "htm", "log", "kt", "py", "js", "ts")),
    APPS("apps", setOf("apk", "apks", "xapk", "aab"));

    companion object { fun from(key: String?) = entries.firstOrNull { it.key == key } }
}

/** «Ordenar por», as in the phone's Files: name, date, size or type, either way. */
enum class SortBy(val key: String) {
    NAME("name"), DATE("date"), SIZE("size"), TYPE("type");
    companion object { fun from(key: String?) = entries.firstOrNull { it.key == key } ?: NAME }
}

data class Sort(val by: SortBy = SortBy.NAME, val descending: Boolean = false)
