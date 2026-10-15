package com.aljaz.spendingtracker.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/*
 * A "period" is the app's month. With a month start day of 1 it is the
 * normal calendar month. With e.g. 15, the period called October runs
 * from 15 October to 14 November: it is named after the month it starts in.
 */

/**
 * First day of the period named after this month.
 * If the start day doesn't exist in this month (e.g. 31 in February),
 * the period starts on the month's last day instead.
 */
fun YearMonth.periodStart(startDay: Int): LocalDate =
    atDay(minOf(startDay, lengthOfMonth()))

/** Last day of the period named after this month (the day before the next period starts). */
fun YearMonth.periodEndInclusive(startDay: Int): LocalDate =
    plusMonths(1).periodStart(startDay).minusDays(1)

/**
 * Converts the period named after this month into a UTC epoch-millisecond range.
 *
 * Boundaries are calculated in the given time zone (the phone's by default),
 * so a purchase at 23:30 on the last day still belongs to that period.
 *
 * @param startDay the day of the month periods start on; 1 = calendar months
 * @return a pair of (start inclusive, end exclusive)
 */
fun YearMonth.toEpochMillisRange(
    zone: ZoneId = ZoneId.systemDefault(),
    startDay: Int = 1
): Pair<Long, Long> {
    val start = periodStart(startDay).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = plusMonths(1).periodStart(startDay).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}

/**
 * The period a date belongs to. With start day 15, 10 October belongs to
 * the September period (15 Sep – 14 Oct); 20 October belongs to October.
 */
fun periodContaining(date: LocalDate, startDay: Int): YearMonth {
    val month = YearMonth.from(date)
    return if (date.isBefore(month.periodStart(startDay))) month.minusMonths(1) else month
}