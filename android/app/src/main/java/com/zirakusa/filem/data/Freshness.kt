package com.zirakusa.filem.data

/**
 * When to read the files again after Android said something changed (a download, a photo, another app). Never while
 * Filem is in the background (it only remembers), and at most once every [gap] ms while open, because a download
 * writes many times. Pure; unit tested.
 */
class Freshness(private val gap: Long = GAP_MS) {
    var dirty = false; private set
    private var last = Long.MIN_VALUE / 2

    fun changed() { dirty = true }

    /** How long to wait before reading again (0 = now), or null when there is nothing to do. */
    fun next(visible: Boolean, now: Long): Long? = if (!dirty || !visible) null else (last + gap - now).coerceAtLeast(0)

    fun refreshed(now: Long) { dirty = false; last = now }

    companion object { const val GAP_MS = 2_000L }
}
