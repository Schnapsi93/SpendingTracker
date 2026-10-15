package com.aljaz.spendingtracker.ui.onboarding

import androidx.lifecycle.ViewModel
import com.aljaz.spendingtracker.data.settings.OnboardingStore
import com.aljaz.spendingtracker.domain.settings.PeriodSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Logic behind the setup guide. Settings chosen here are saved right away,
 * in the same place as the Settings screen, so both always agree.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val periodSettings: PeriodSettingsRepository,
    private val onboardingStore: OnboardingStore
) : ViewModel() {

    val monthStartDay: StateFlow<Int> = periodSettings.monthStartDay

    val carryOver: StateFlow<Boolean> = periodSettings.carryOver

    fun onMonthStartDayChange(day: Int) = periodSettings.setMonthStartDay(day)

    fun onCarryOverToggle(enabled: Boolean) = periodSettings.setCarryOver(enabled)

    /** Called when the guide is finished or skipped; it won't appear again on its own. */
    fun onFinish() = onboardingStore.markCompleted()
}