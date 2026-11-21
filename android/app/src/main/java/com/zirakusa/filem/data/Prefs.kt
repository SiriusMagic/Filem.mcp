package com.zirakusa.filem.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

/** «Favoritos», «Buscar historial» and the screen preferences (view, order, hidden files), on this phone only. */
class Prefs(context: Context) {
    private val p = context.getSharedPreferences("filem", Context.MODE_PRIVATE)

    fun starred(): Set<String> = p.getStringSet("starred", emptySet()).orEmpty()
    fun setStarred(path: String, on: Boolean) = p.edit { putStringSet("starred", if (on) starred() + path else starred() - path) }

    var grid: Boolean
        get() = p.getBoolean("grid", false)
        set(v) = p.edit { putBoolean("grid", v) }
    var sort: Sort
        get() = Sort(SortBy.from(p.getString("sort", null)), p.getBoolean("desc", false))
        set(v) = p.edit { putString("sort", v.by.key); putBoolean("desc", v.descending) }
    var showHidden: Boolean
        get() = p.getBoolean("hidden", false)
        set(v) = p.edit { putBoolean("hidden", v) }
    /** Past searches, newest first (at most [FileRules.HISTORY_MAX]). */
    var history: List<String>
        get() = runCatching { JSONArray(p.getString("history", "[]")).let { a -> (0 until a.length()).map { a.getString(it) } } }
            .getOrDefault(emptyList())
        set(v) = p.edit { putString("history", JSONArray(v.take(FileRules.HISTORY_MAX)).toString()) }
}
