package com.aljaz.spendingtracker.data.settings

import android.content.Context
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the period settings in private app preferences, and exposes them
 * as a live value so every screen updates the moment they change.
 */
@Singleton
class PeriodSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) : PeriodSettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _monthStartDay = MutableStateFlow(
        prefs.getInt(KEY_MONTH_START_DAY, 1).coerceIn(1, 31)
    )
    override val monthStartDay: StateFlow<Int> = _monthStartDay.asStateFlow()

    override fun setMonthStartDay(day: Int) {
        val value = day.coerceIn(1, 31)
        prefs.edit().putInt(KEY_MONTH_START_DAY, value).apply()
        _monthStartDay.value = value // everyone observing updates right away
    }

    private companion object {
        const val PREFS_NAME = "period_settings"
        const val KEY_MONTH_START_DAY = "month_start_day"
    }
}