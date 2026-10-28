package com.aljaz.spendingtracker.ui.shared

import com.aljaz.spendingtracker.domain.sharing.SharedSummary
import com.aljaz.spendingtracker.domain.sharing.SharedTotals
import com.aljaz.spendingtracker.domain.sharing.SyncStatus
import java.time.YearMonth

/**
 * Everything the shared summary screen shows at one moment.
 *
 * @property month the period shown
 * @property monthStartDay the day periods start on; 1 = calendar months
 * @property currentPeriod the period containing today, for the picker's "Today"
 * @property isLoggedIn false when no shared account is set up yet
 * @property partnerName the partner's display name, for labels
 * @property period totals and categories of the shown period
 * @property overall totals of everything ever shared; decides who pays whom
 * @property syncStatus progress and result of syncing
 * @property isLoading true until the first data has arrived
 */
data class SharedSummaryUiState(
    val month: YearMonth,
    val monthStartDay: Int = 1,
    val currentPeriod: YearMonth = month,
    val isLoggedIn: Boolean = true,
    val partnerName: String = "Partner",
    val period: SharedSummary = SharedSummary.EMPTY,
    val overall: SharedTotals = SharedTotals(),
    val syncStatus: SyncStatus = SyncStatus(),
    val isLoading: Boolean = true
)