package com.zirakusa.filem

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import com.zirakusa.filem.data.Category
import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.FileRules
import com.zirakusa.filem.data.FileStore

/** The lines of Filem's card in Inicio: the newest documents (name and size), never the trash. Pure, with tests. */
object SummaryLines {
    const val MAX = 5
    const val DAYS = 30L
    fun lines(items: List<FileItem>): List<String> = items.asSequence()
        .filter { !it.isDir && it.category == Category.DOCUMENTS && !it.path.startsWith(".filem/") && !it.path.contains("/.filem/") }
        .sortedByDescending { it.modified }.take(MAX).map { "${it.name.take(60)} · ${FileRules.size(it.size)}" }.toList()
}

/**
 * The family summary (see Inicio's FAMILIA.md): the recent documents for Inicio's «Hoy». Only apps signed by the same
 * developer may ask (Android's signature permission and the caller's signature checked here); only names and sizes leave.
 */
class SummaryProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "summary") return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
            else context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val since = System.currentTimeMillis() - SummaryLines.DAYS * 86_400_000L
        val lines = if (!allowed) emptyList() else runCatching {
            SummaryLines.lines(FileStore(Environment.getExternalStorageDirectory())
                .search("", Category.DOCUMENTS, limit = 20, sinceMs = since, maxVisited = 20_000))
        }.getOrDefault(emptyList())
        return Bundle().apply {
            putString("title", context.getString(R.string.summary_title))
            putStringArrayList("lines", ArrayList(lines))
            putString("empty", context.getString(if (allowed) R.string.summary_empty else R.string.summary_no_access))
        }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
