package com.zirakusa.filem.mcp

import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileFailure
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileStore
import com.zirakusa.filem.data.Sort
import com.zirakusa.filem.data.SortBy
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Filem's MCP tools. READ tokens list, search, describe and read small text files; WRITE tokens also create folders,
 * rename, copy, move and send to the trash (restorable on the phone). Every path goes through FileStore, which keeps
 * it inside the storage and away from Filem's own folder. Arguments are checked one by one; nothing runs as code.
 */
object FileTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    const val PATH_MAX = 1000
    const val QUERY_MAX = 200
    const val TEXT_MAX = 256 * 1024
    const val BATCH_MAX = 50

    private fun schema(vararg props: Pair<String, JSONObject>, required: List<String> = emptyList()) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun str(d: String) = JSONObject().put("type", "string").put("description", d)
    private fun int(d: String) = JSONObject().put("type", "integer").put("description", d)
    private fun strings(d: String) = JSONObject().put("type", "array").put("items", JSONObject().put("type", "string")).put("description", d)

    private val PATH = "path" to str("Path relative to the storage root, e.g. 'Download/a.pdf'. '' is the root.")
    private val LIMIT = "limit" to int("How many, 1–200 (default 50).")
    private val OFFSET = "offset" to int("Skip this many (default 0), to page.")
    private val CATEGORY = "category" to str("One of: " + Category.entries.joinToString { it.key } + ".")

    val tools = listOf(
        Tool("files_list", "List a folder: folders first, then files. sort: name, date, size or type; descending true/false.", false,
            schema(PATH, "sort" to str("name, date, size or type (default name)."),
                "descending" to JSONObject().put("type", "boolean").put("description", "Reverse the order."), LIMIT, OFFSET)),
        Tool("files_search", "Find files whose name has every word of the query (no case, no accents), newest first. " +
            "Optional category and folder to search under.", false,
            schema("query" to str("Words of the name; may be empty when a category is given."), CATEGORY,
                "under" to str("Only inside this folder (default: everywhere)."), LIMIT)),
        Tool("files_info", "Describe one file or folder: name, kind, size, items inside, date and category.", false, schema(PATH, required = listOf("path"))),
        Tool("files_recent", "The newest files of the phone.", false, schema(LIMIT)),
        Tool("files_storage", "Free and total space, and the size and count of each category.", false, schema()),
        Tool("files_read_text", "Read a text file of up to 256 KB (UTF-8).", false, schema(PATH, required = listOf("path"))),
        Tool("files_create_folder", "Create a folder named «name» inside «path».", true,
            schema(PATH, "name" to str("New folder name."), required = listOf("path", "name"))),
        Tool("files_rename", "Rename a file or folder (same place).", true,
            schema(PATH, "new_name" to str("The new name."), required = listOf("path", "new_name"))),
        Tool("files_copy", "Copy files or folders into folder «to». A taken name gets ' (1)'.", true,
            schema("paths" to strings("What to copy (up to 50)."), "to" to str("Destination folder."), required = listOf("paths", "to"))),
        Tool("files_move", "Move files or folders into folder «to». A taken name gets ' (1)'.", true,
            schema("paths" to strings("What to move (up to 50)."), "to" to str("Destination folder."), required = listOf("paths", "to"))),
        Tool("files_trash", "Send a file or folder to Filem's trash (the user can restore it for 30 days).", true,
            schema(PATH, required = listOf("path"))),
    )

    fun available(access: Access) = tools.filter { access == Access.WRITE || !it.write }

    fun call(name: String, args: JSONObject, access: Access, store: FileStore): JSONObject {
        val tool = tools.firstOrNull { it.name == name } ?: throw FileFailure("UNKNOWN_TOOL", "No tool called «$name».")
        if (tool.write && access != Access.WRITE) throw FileFailure("READ_ONLY", "This token is read-only.")
        val allowed = tool.schema.getJSONObject("properties").keys().asSequence().toSet()
        args.keys().forEach { if (it !in allowed) throw FileFailure("BAD_ARGUMENT", "Unknown argument «$it».") }
        return when (name) {
            "files_list" -> {
                val by = text(args, "sort", 10)?.let { key -> SortBy.entries.firstOrNull { it.key == key }
                    ?: throw FileFailure("BAD_ARGUMENT", "«sort» must be name, date, size or type.") } ?: SortBy.NAME
                val desc = if (args.has("descending")) args.opt("descending") as? Boolean
                    ?: throw FileFailure("BAD_ARGUMENT", "«descending» must be true or false.") else false
                page(store.list(text(args, "path", PATH_MAX).orEmpty(), Sort(by, desc)), args)
            }
            "files_search" -> {
                val query = text(args, "query", QUERY_MAX).orEmpty()
                val category = text(args, "category", 20)?.let { Category.from(it) ?: throw FileFailure("BAD_ARGUMENT", "Unknown category «$it».") }
                if (query.isBlank() && category == null) throw FileFailure("BAD_ARGUMENT", "Give a query or a category.")
                val limit = limit(args)
                JSONObject().put("files", JSONArray(store.search(query, category, text(args, "under", PATH_MAX).orEmpty(), limit).map(::json)))
            }
            "files_info" -> json(store.info(text(args, "path", PATH_MAX, required = true)!!))
            "files_recent" -> JSONObject().put("files", JSONArray(store.recent(limit(args)).map(::json)))
            "files_storage" -> {
                val (free, total) = store.space()
                JSONObject().put("freeBytes", free).put("totalBytes", total).put("categories", JSONObject().apply {
                    store.categories().forEach { (c, v) -> put(c.key, JSONObject().put("bytes", v.first).put("files", v.second)) }
                })
            }
            "files_read_text" -> {
                val path = text(args, "path", PATH_MAX, required = true)!!
                JSONObject().put("path", path).put("text", store.readText(path, TEXT_MAX))
            }
            "files_create_folder" -> json(store.createFolder(text(args, "path", PATH_MAX, required = true)!!, text(args, "name", 255, required = true)!!))
            "files_rename" -> json(store.rename(text(args, "path", PATH_MAX, required = true)!!, text(args, "new_name", 255, required = true)!!))
            "files_copy", "files_move" -> {
                val paths = paths(args)
                val to = text(args, "to", PATH_MAX, required = true)!!
                JSONObject().put("files", JSONArray(store.transfer(paths, to, move = name == "files_move").map(::json)))
            }
            "files_trash" -> store.trash(text(args, "path", PATH_MAX, required = true)!!).let {
                JSONObject().put("trashed", it.original).put("restorable", true)
            }
            else -> throw FileFailure("UNKNOWN_TOOL", "No tool called «$name».")
        }
    }

    fun json(item: FileItem): JSONObject = JSONObject().put("path", item.path).put("name", item.name)
        .put("kind", if (item.isDir) "folder" else "file").put("modified", Instant.ofEpochMilli(item.modified).toString()).apply {
            if (item.isDir) put("items", item.children) else { put("size", item.size); item.category?.let { put("category", it.key) } }
        }

    private fun page(items: List<FileItem>, args: JSONObject): JSONObject {
        val offset = if (args.has("offset")) (args.opt("offset") as? Int)?.takeIf { it >= 0 }
            ?: throw FileFailure("BAD_ARGUMENT", "«offset» must be 0 or more.") else 0
        val limit = limit(args)
        return JSONObject().put("total", items.size).put("items", JSONArray(items.drop(offset).take(limit).map(::json)))
    }

    private fun limit(args: JSONObject): Int = if (args.has("limit")) (args.opt("limit") as? Int)?.takeIf { it in 1..200 }
        ?: throw FileFailure("BAD_ARGUMENT", "«limit» must be 1–200.") else 50

    private fun paths(args: JSONObject): List<String> {
        val array = args.opt("paths") as? JSONArray ?: throw FileFailure("BAD_ARGUMENT", "«paths» must be a list of paths.")
        if (array.length() !in 1..BATCH_MAX) throw FileFailure("BAD_ARGUMENT", "«paths» needs 1–$BATCH_MAX paths.")
        return (0 until array.length()).map { (array.opt(it) as? String)?.takeIf { p -> p.length <= PATH_MAX }
            ?: throw FileFailure("BAD_ARGUMENT", "Every path must be text.") }
    }

    private fun text(args: JSONObject, key: String, max: Int, required: Boolean = false): String? {
        if (!args.has(key) || args.isNull(key)) { if (required) throw FileFailure("BAD_ARGUMENT", "«$key» is required."); return null }
        val v = args.get(key) as? String ?: throw FileFailure("BAD_ARGUMENT", "«$key» must be text.")
        if (v.length > max) throw FileFailure("TOO_LONG", "«$key» passes $max characters.")
        return v
    }
}
