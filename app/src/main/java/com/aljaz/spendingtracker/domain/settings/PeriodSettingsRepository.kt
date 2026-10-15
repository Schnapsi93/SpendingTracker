package com.aljaz.spendingtracker.domain.settings

import kotlinx.coroutines.flow.StateFlow

/** Settings that decide how the app divides time into periods. */
interface PeriodSettingsRepository {

    /** Day of the month periods start on (1–31); 1 means calendar months. Updates live. */
    val monthStartDay: StateFlow<Int>

    /** Saves a new start day; values outside 1–31 are clamped. */
    fun setMonthStartDay(day: Int)


    /** Whether each period starts with the previous periods' leftover balance. Updates live. */
    val carryOver: StateFlow<Boolean>

    /** Turns carry-over on or off. */
    fun setCarryOver(enabled: Boolean)
}