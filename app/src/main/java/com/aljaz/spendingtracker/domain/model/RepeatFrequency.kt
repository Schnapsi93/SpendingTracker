package com.aljaz.spendingtracker.domain.model

import java.time.temporal.ChronoUnit

/**
 * How often a recurring transaction happens.
 *
 * Stored in the database by name, so entries can be reordered freely,
 * but never renamed without a migration.
 *
 * @property unit the calendar step between two occurrences
 * @property label text shown in the UI
 */
enum class RepeatFrequency(val unit: ChronoUnit, val label: String) {
    WEEKLY(ChronoUnit.WEEKS, "Weekly"),
    MONTHLY(ChronoUnit.MONTHS, "Monthly"),
    YEARLY(ChronoUnit.YEARS, "Yearly");

    companion object {
        /** Preselected in the form; most bills (rent, subscriptions) are monthly. */
        val DEFAULT = MONTHLY
    }
}