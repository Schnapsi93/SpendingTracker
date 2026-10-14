package com.aljaz.spendingtracker.core

import java.time.YearMonth
import java.time.ZoneId

/**
 * Converts a calendar month into a UTC epoch-millisecond range.
 *
 * Month boundaries are calculated in the given time zone (the phone's
 * by default), so a purchase at 23:30 on the last day still belongs to
 * that month for the user.
 *
 * @return a pair of (start inclusive, end exclusive)
 */
fun YearMonth.toEpochMillisRange(
    zone: ZoneId = ZoneId.systemDefault()
): Pair<Long, Long> {
    val start = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}