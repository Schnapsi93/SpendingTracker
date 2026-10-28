package com.aljaz.spendingtracker.ui.shared

import com.aljaz.spendingtracker.domain.sharing.Settlement
import com.aljaz.spendingtracker.domain.sharing.SharedSummary
import com.aljaz.spendingtracker.domain.sharing.SharedTotals
import com.aljaz.spendingtracker.domain.sharing.SyncStatus
import java.time.YearMonth

/**
 * The "Record a payment" dialog.
 *
 * @property partnerPaysMe true for "partner paid me", false for "I paid partner"
 * @property amountText the amount as typed, pre-filled with the open balance
 * @property errorMessage why saving failed, or null
 * @property isSaving true while the payment is sent to the server
 */
data class SettleDialogState(
    val partnerPaysMe: Boolean,
    val amountText: String,
    val note: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

/**
 * Everything the shared summary screen shows at one moment.
 *
 * @property month the period shown
 * @property monthStartDay the day periods start on; 1 = calendar months
 * @property currentPeriod the period containing today, for the picker's "Today"
 * @property isLoggedIn false when no shared account is set up yet
 * @property myUserId this user's server id, to tell payments apart
 * @property partnerId the partner's server id, or null if there is no partner yet
 * @property partnerName the partner's display name, for labels
 * @property period totals and categories of the shown period
 * @property overall totals of everything ever shared, including settle-up payments
 * @property settlements every settle-up payment, newest first
 * @property syncStatus progress and result of syncing
 * @property settleDialog the open "Record a payment" dialog, or null
 * @property pendingDeleteSettlement the payment waiting for "Undo" confirmation, or null
 * @property message an error from undoing a payment, or null
 * @property isLoading true until the first data has arrived
 */
data class SharedSummaryUiState(
    val month: YearMonth,
    val monthStartDay: Int = 1,
    val currentPeriod: YearMonth = month,
    val isLoggedIn: Boolean = true,
    val myUserId: String = "",
    val partnerId: String? = null,
    val partnerName: String = "Partner",
    val period: SharedSummary = SharedSummary.EMPTY,
    val overall: SharedTotals = SharedTotals(),
    val settlements: List<Settlement> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus(),
    val settleDialog: SettleDialogState? = null,
    val pendingDeleteSettlement: Settlement? = null,
    val message: String? = null,
    val isLoading: Boolean = true
) {
    /** Payments can only be recorded with a partner in the household. */
    val canSettle: Boolean
        get() = isLoggedIn && partnerId != null
}