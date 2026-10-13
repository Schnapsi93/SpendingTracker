package com.aljaz.spendingtracker.domain.model

/**
 * Where a transaction came from. Useful for filtering, debugging
 * the automatic parsers, and showing a "needs review" badge.
 */
enum class TransactionSource {
    MANUAL,   // typed in by the user
    SMS,      // parsed from a bank SMS
    WALLET,   // parsed from a Google Wallet notification
    IMPORT    // loaded from a CSV file
}