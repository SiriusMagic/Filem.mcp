package com.zirakusa.filem.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** An expected failure the screens and the AI can read (code + message), never a crash. */
class FileFailure(val code: String, message: String) : Exception(message)

/** A file in the trash: where it came from and when it was removed (kept 30 days). */
data class TrashEntry(val id: String, val name: String, val original: String, val deletedAt: Long, val isDir: Boolean, val size: Long)

/**
 * Every file operation of Filem, on real files under [root] (the internal storage). Paths are relative and pass
 * [FileRules.cleanPath], so nothing outside [root] and nothing of Filem's own folder can be touched. Deleting moves to
 * `.filem/trash` on the same storage (a rename, instant); the trash is emptied after 30 days.
 */
class FileStore(val root: File, private val now: () -> Long = System::currentTimeMillis) {
    private val trashDir get() = File(root, "${FileRules.APP_DIR}/trash")
    private val trashIndex get() = File(root, "${FileRules.APP_DIR}/trash.json")
    var showHidden = false

    fun file(path: String): File {
        val clean = FileRules.cleanPath(path) ?: throw FileFailure("BAD_PATH", "The path «$path» is not allowed.")
        val f = if (clean.isEmpty()) root else File(root, clean)
        val base = root.canonicalPath
        if (f.canonicalPath != base && !f.canonicalPath.startsWith("$base/")) throw FileFailure("BAD_PATH", "The path «$path» is not allowed.")
        return f
    }

    private fun relative(f: File) = f.canonicalPath.removePrefix(root.canonicalPath).trimStart('/')
    fun item(f: File) = FileItem(relative(f), f.name, f.isDirectory, if (f.isDirectory) 0 else f.length(), f.lastModified(),
        if (f.isDirectory) visibleChildren(f).size else 0)
    private fun visibleChildren(dir: File) = dir.listFiles().orEmpty().filter {
        !(dir == root && it.name == FileRules.APP_DIR) && (showHidden || !it.name.startsWith("."))
    }

    fun info(path: String): FileItem = file(path).let { if (it.exists()) item(it) else throw notFound(path) }

    fun list(path: String, sort: Sort = Sort()): List<FileItem> {
        val dir = file(path)
        if (!dir.isDirectory) throw if (dir.exists()) FileFailure("NOT_A_FOLDER", "«$path» is a file.") else notFound(path)
        return FileRules.sorted(visibleChildren(dir).map(::item), sort)
    }

    /** Files (not folders) whose name has every word of [query], newest first; walks at most [maxVisited] entries. */
    fun search(query: String, category: Category? = null, under: String = "", limit: Int = 200, maxVisited: Int = 200_000): List<FileItem> {
        val found = mutableListOf<FileItem>(); var visited = 0
        walk(file(under)) { f ->
            visited++
            if (!f.isDirectory && (query.isBlank() || FileRules.matches(f.name, query)) &&
                (category == null || FileRules.category(f.name) == category)) found += item(f)
            visited < maxVisited
        }
        return found.sortedByDescending { it.modified }.take(limit)
    }

    /** Newest files anywhere (for «Recientes»). */
    fun recent(limit: Int = 20) = search("", limit = limit)

    /** Size and count of each category, plus «Descargas» (the Download folder). */
    fun categories(): Map<Category, Pair<Long, Int>> {
        val totals = Category.entries.associateWith { longArrayOf(0, 0) }
        walk(root) { f ->
            if (!f.isDirectory) FileRules.category(f.name)?.let { totals.getValue(it)[0] += f.length(); totals.getValue(it)[1]++ }
            true
        }
        val download = File(root, "Download")
        if (download.isDirectory) walk(download) { f ->
            if (!f.isDirectory) { totals.getValue(Category.DOWNLOADS)[0] += f.length(); totals.getValue(Category.DOWNLOADS)[1]++ }; true
        }
        return totals.mapValues { it.value[0] to it.value[1].toInt() }
    }

    /** Depth first, skipping Filem's folder, hidden entries (unless shown) and Android/data-like protected folders. */
    private fun walk(start: File, visit: (File) -> Boolean) {
        val stack = ArrayDeque(listOf(start))
        while (stack.isNotEmpty()) {
            val dir = stack.removeLast()
            for (child in visibleChildren(dir)) {
                if (!visit(child)) return
                if (child.isDirectory && !(dir == root && child.name == "Android")) stack.addLast(child)
            }
        }
    }

    fun createFolder(parent: String, name: String): FileItem {
        checkName(name)
        val dir = file(parent)
        if (!dir.isDirectory) throw notFound(parent)
        val target = File(dir, name)
        if (target.exists()) throw FileFailure("ALREADY_EXISTS", "«$name» already exists here.")
        if (!target.mkdir()) throw FileFailure("IO_ERROR", "Could not create «$name».")
        return item(target)
    }

    fun rename(path: String, newName: String): FileItem {
        checkName(newName)
        val f = existing(path)
        if (f == root) throw FileFailure("BAD_PATH", "The storage itself cannot be renamed.")
        val target = File(f.parentFile, newName)
        if (target.exists()) throw FileFailure("ALREADY_EXISTS", "«$newName» already exists here.")
        if (!f.renameTo(target)) throw FileFailure("IO_ERROR", "Could not rename «${f.name}».")
        return item(target)
    }

    /** Copies or moves into folder [to]; a taken name gets " (1)". Returns the new items. */
    fun transfer(paths: List<String>, to: String, move: Boolean): List<FileItem> {
        val dest = file(to)
        if (!dest.isDirectory) throw notFound(to)
        return paths.map { path ->
            val source = existing(path)
            if (source == root) throw FileFailure("BAD_PATH", "The storage itself cannot be copied or moved.")
            if (source.isDirectory && FileRules.isInside(relative(dest), relative(source)))
                throw FileFailure("INTO_ITSELF", "A folder cannot go inside itself.")
            if (move && source.parentFile?.canonicalPath == dest.canonicalPath) return@map item(source)
            val target = File(dest, FileRules.uniqueName(source.name) { File(dest, it).exists() })
            try {
                if (move) { if (!source.renameTo(target)) { source.copyRecursively(target); source.deleteRecursively() } }
                else source.copyRecursively(target)
            } catch (e: IOException) { throw FileFailure("IO_ERROR", e.message ?: "Could not copy «${source.name}».") }
            item(target)
        }
    }

    // ---- Trash ----

    fun trash(path: String): TrashEntry {
        val f = existing(path)
        if (f == root) throw FileFailure("BAD_PATH", "The storage itself cannot be deleted.")
        trashDir.mkdirs()
        val id = "${now()}-${(1000..9999).random()}"
        val entry = TrashEntry(id, f.name, relative(f), now(), f.isDirectory, if (f.isDirectory) 0 else f.length())
        if (!f.renameTo(File(trashDir, id))) throw FileFailure("IO_ERROR", "Could not move «${f.name}» to the trash.")
        saveTrash(trashed() + entry)
        return entry
    }

    fun trashed(): List<TrashEntry> = runCatching {
        val array = JSONArray(trashIndex.readText())
        (0 until array.length()).map { array.getJSONObject(it) }.map {
            TrashEntry(it.getString("id"), it.getString("name"), it.getString("original"), it.getLong("deletedAt"),
                it.optBoolean("isDir"), it.optLong("size"))
        }.filter { File(trashDir, it.id).exists() }
    }.getOrDefault(emptyList())

    fun restore(id: String): FileItem {
        val entry = trashed().firstOrNull { it.id == id } ?: throw FileFailure("NOT_FOUND", "Nothing in the trash with id $id.")
        val parent = file(FileRules.parent(entry.original)).also { it.mkdirs() }
        val target = File(parent, FileRules.uniqueName(entry.name) { File(parent, it).exists() })
        if (!File(trashDir, id).renameTo(target)) throw FileFailure("IO_ERROR", "Could not restore «${entry.name}».")
        saveTrash(trashed().filterNot { it.id == id })
        return item(target)
    }

    fun deleteForever(id: String) {
        File(trashDir, id).deleteRecursively()
        saveTrash(trashed().filterNot { it.id == id })
    }

    /** Empties everything, or only what is older than [olderThanDays]. Returns how many were removed. */
    fun emptyTrash(olderThanDays: Int = 0): Int {
        val limit = now() - olderThanDays * 86_400_000L
        val gone = trashed().filter { olderThanDays == 0 || it.deletedAt < limit }
        gone.forEach { File(trashDir, it.id).deleteRecursively() }
        saveTrash(trashed().filterNot { e -> gone.any { it.id == e.id } })
        return gone.size
    }

    fun trashSize() = trashed().sumOf { if (it.isDir) File(trashDir, it.id).walk().sumOf { f -> f.length() } else it.size }

    private fun saveTrash(list: List<TrashEntry>) {
        trashIndex.parentFile?.mkdirs()
        trashIndex.writeText(JSONArray(list.map {
            JSONObject().put("id", it.id).put("name", it.name).put("original", it.original).put("deletedAt", it.deletedAt)
                .put("isDir", it.isDir).put("size", it.size)
        }).toString())
    }

    // ---- Helpers ----

    fun readText(path: String, max: Int): String {
        val f = existing(path)
        if (f.isDirectory) throw FileFailure("NOT_A_FILE", "«$path» is a folder.")
        if (f.length() > max) throw FileFailure("TOO_LARGE", "«${f.name}» passes $max bytes.")
        val bytes = f.readBytes()
        if (bytes.any { it == 0.toByte() }) throw FileFailure("NOT_TEXT", "«${f.name}» is not a text file.")
        return String(bytes, Charsets.UTF_8)
    }

    fun space() = root.freeSpace to root.totalSpace

    private fun existing(path: String) = file(path).also { if (!it.exists()) throw notFound(path) }
    private fun notFound(path: String) = FileFailure("NOT_FOUND", "Nothing at «$path».")
    private fun checkName(name: String) = FileRules.nameProblem(name)?.let {
        throw FileFailure("BAD_NAME", "The name «${name.take(40)}» is not valid ($it).")
    }
}
