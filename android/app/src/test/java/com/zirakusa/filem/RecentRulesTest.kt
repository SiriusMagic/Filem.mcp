package com.zirakusa.filem

import com.zirakusa.filem.data.FileItem
import com.zirakusa.filem.data.RecentGroup
import com.zirakusa.filem.data.RecentRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** «Reciente»: calendar days in the phone's zone (Hoy, Ayer, Últimos 7 días, Últimos 30 días), nothing older. */
class RecentRulesTest {
    private val zone = ZoneId.of("America/Guayaquil")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) = ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone).toInstant().toEpochMilli()
    private val now = at(2026, 11, 21, 0, 30) // just after midnight: an hour ago is already «Ayer»

    @Test fun calendarDaysNotHours() {
        assertEquals(RecentGroup.TODAY, RecentRules.groupOf(at(2026, 11, 21, 0, 1), now, zone))
        assertEquals(RecentGroup.TODAY, RecentRules.groupOf(now + 60_000, now, zone)) // a clock a bit ahead
        assertEquals(RecentGroup.YESTERDAY, RecentRules.groupOf(at(2026, 11, 20, 23, 30), now, zone))
        assertEquals(RecentGroup.YESTERDAY, RecentRules.groupOf(at(2026, 11, 20, 0, 0), now, zone))
        assertEquals(RecentGroup.LAST_7_DAYS, RecentRules.groupOf(at(2026, 11, 19, 23, 59), now, zone))
        assertEquals(RecentGroup.LAST_7_DAYS, RecentRules.groupOf(at(2026, 11, 15, 0, 0), now, zone))
        assertEquals(RecentGroup.LAST_30_DAYS, RecentRules.groupOf(at(2026, 11, 14, 23, 59), now, zone))
        assertEquals(RecentGroup.LAST_30_DAYS, RecentRules.groupOf(at(2026, 10, 23, 0, 0), now, zone))
        assertNull(RecentRules.groupOf(at(2026, 10, 22, 23, 59), now, zone))
        assertEquals(at(2026, 10, 23, 0, 0), RecentRules.since(now, zone))
    }

    @Test fun groupsInOrderNewestFirstWithoutFoldersOrEmptyGroups() {
        fun f(name: String, ms: Long, dir: Boolean = false) = FileItem(name, name, dir, 1, ms)
        val items = listOf(f("viejo.pdf", at(2026, 9, 1, 10)), f("mes.pdf", at(2026, 11, 1, 10)), f("hoy1.jpg", at(2026, 11, 21, 0, 5)),
            f("hoy2.jpg", at(2026, 11, 21, 0, 20)), f("Carpeta", at(2026, 11, 21, 0, 10), dir = true))
        val groups = RecentRules.group(items, now, zone)
        assertEquals(listOf(RecentGroup.TODAY, RecentGroup.LAST_30_DAYS), groups.keys.toList())
        assertEquals(listOf("hoy2.jpg", "hoy1.jpg"), groups.getValue(RecentGroup.TODAY).map { it.name })
        assertEquals(listOf("mes.pdf"), groups.getValue(RecentGroup.LAST_30_DAYS).map { it.name })
    }
}
