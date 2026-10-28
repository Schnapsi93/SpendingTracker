package com.aljaz.spendingtracker.domain.sharing

/**
 * The account this phone is logged in with on the shared expenses server.
 *
 * Secrets (login token, password) are deliberately not part of this class,
 * so they can't end up on screen or in logs by accident.
 *
 * @property serverUrl e.g. "https://spender.aljaz.top", without a trailing slash
 * @property userId this user's record id on the server
 * @property householdId the household whose expenses are shared
 * @property partnerId the other member of the household, or null if there is none yet
 */
data class SharedAccount(
    val serverUrl: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val householdId: String,
    val householdName: String,
    val partnerId: String?,
    val partnerName: String?
) {
    /** True when someone else is in the household to split with. */
    val hasPartner: Boolean
        get() = partnerId != null

    companion object {
        /** Pre-filled in the login form. */
        const val DEFAULT_SERVER_URL = "https://spender.aljaz.top"
    }
}