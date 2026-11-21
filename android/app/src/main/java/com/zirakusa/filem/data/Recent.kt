package com.zirakusa.filem.data

import java.time.Instant
import java.time.ZoneId

/** The groups of «Reciente», newest first. Older than 30 days is not shown. */
enum class RecentGroup { TODAY, YESTERDAY, LAST_7_DAYS, LAST_30_DAYS }

/** Pure rules of «Reciente»: calendar days in the phone's zone. Unit tested. */
object RecentRules {
    const val DAYS = 30

    private fun dayStart(now: Long, zone: ZoneId, daysAgo: Long) =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays(daysAgo).atStartOfDay(zone).toInstant().toEpochMilli()

    /** The first moment «Reciente» shows: the start of the day 29 days ago (today counts as one of the 30). */
    fun since(now: Long, zone: ZoneId = ZoneId.systemDefault()) = dayStart(now, zone, DAYS - 1L)

    fun groupOf(modified: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): RecentGroup? = when {
        modified >= dayStart(now, zone, 0) -> RecentGroup.TODAY
        modified >= dayStart(now, zone, 1) -> RecentGroup.YESTERDAY
        modified >= dayStart(now, zone, 6) -> RecentGroup.LAST_7_DAYS
        modified >= since(now, zone) -> RecentGroup.LAST_30_DAYS
        else -> null
    }

    /** Only the groups that have files, in [RecentGroup] order, each newest first. Folders are left out. */
    fun group(items: List<FileItem>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Map<RecentGroup, List<FileItem>> {
        val byGroup = items.filter { !it.isDir }.sortedByDescending { it.modified }.groupBy { groupOf(it.modified, now, zone) }
        return RecentGroup.entries.mapNotNull { g -> byGroup[g]?.let { g to it } }.toMap()
    }
}
