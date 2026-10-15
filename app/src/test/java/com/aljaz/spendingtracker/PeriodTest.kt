package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.core.periodContaining
import com.aljaz.spendingtracker.core.periodEndInclusive
import com.aljaz.spendingtracker.core.periodStart
import com.aljaz.spendingtracker.core.toEpochMillisRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** Checks how months are divided into periods. */
class PeriodTest {

    private val utc = ZoneOffset.UTC
    private val october = YearMonth.of(2026, 10)

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(utc).toInstant().toEpochMilli()

    @Test
    fun startDayOneIsTheCalendarMonth() {
        val (start, end) = october.toEpochMillisRange(utc, startDay = 1)
        assertEquals(millis(LocalDate.of(2026, 10, 1)), start)
        assertEquals(millis(LocalDate.of(2026, 11, 1)), end)
    }

    @Test
    fun startDayFifteenRunsToTheFourteenthOfNextMonth() {
        val (start, end) = october.toEpochMillisRange(utc, startDay = 15)
        assertEquals(millis(LocalDate.of(2026, 10, 15)), start)
        assertEquals(millis(LocalDate.of(2026, 11, 15)), end) // exclusive
        assertEquals(LocalDate.of(2026, 11, 14), october.periodEndInclusive(15))
    }

    @Test
    fun startDayIsClampedInShortMonths() {
        val february = YearMonth.of(2026, 2)
        assertEquals(LocalDate.of(2026, 2, 28), february.periodStart(31))
        // January's period ends the day before February's starts.
        assertEquals(LocalDate.of(2026, 2, 27), YearMonth.of(2026, 1).periodEndInclusive(31))
    }

    @Test
    fun findsThePeriodContainingADate() {
        assertEquals(YearMonth.of(2026, 9), periodContaining(LocalDate.of(2026, 10, 10), 15))
        assertEquals(october, periodContaining(LocalDate.of(2026, 10, 15), 15))
        assertEquals(october, periodContaining(LocalDate.of(2026, 11, 14), 15))
        assertEquals(october, periodContaining(LocalDate.of(2026, 10, 1), 1))
    }
}