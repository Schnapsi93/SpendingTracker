package com.aljaz.spendingtracker.domain.model

/**
 * Where a transaction came from. Useful for filtering, debugging
 * the automatic parsers, and showing a "needs review" badge.
 *
 * Stored in the database and CSV by name, so entries can be reordered
 * freely, but never renamed without a migration.
 *
 * @property isAutomatic true if the transaction was read from a bank
 *           message (it has a merchant name, so keyword rules apply)
 */
enum class TransactionSource(val isAutomatic: Boolean) {
    MANUAL(false),       // typed in by the user
    SMS(true),           // parsed from a bank SMS
    WALLET(true),        // parsed from a Google Wallet notification
    NOTIFICATION(true),  // parsed from another app's notification (e.g. NLB Pay)
    IMPORT(false),       // loaded from a CSV file
    RECURRING(false)     // created automatically by a repeating transaction
}