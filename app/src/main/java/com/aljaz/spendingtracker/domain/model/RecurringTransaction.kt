package com.aljaz.spendingtracker.domain.model

import java.time.Instant
import java.time.ZoneId

/**
 * A template that creates the same transaction again and again,
 * e.g. rent every month or a gym membership every year.
 *
 * Occurrences are always counted from [startTimestamp] instead of from the
 * previous one. That keeps month ends stable: a series starting on 31 Jan
 * gives 28 Feb, then 31 Mar (not 28 Mar, which stepping one month at a
 * time from 28 Feb would give).
 *
 * @property id database id, or 0 for one that is not saved yet
 * @property startTimestamp the first occurrence, UTC epoch milliseconds
 * @property generatedCount how many occurrences have been created so far;
 *           occurrence number [generatedCount] is the next one due
 * @property createdAt when the user set it up
 */
data class RecurringTransaction(
    val id: Long = 0,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long? = null,
    val title: String,
    val note: String? = null,
    val frequency: RepeatFrequency,
    val startTimestamp: Long,
    val generatedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {

    /**
     * When occurrence number [index] happens (0 = the first one).
     *
     * Calculated in the phone's time zone, so "every month at 08:00"
     * stays at 08:00 across daylight saving changes.
     */
    fun occurrenceAt(index: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(startTimestamp)
            .atZone(zone)
            .plus(index.toLong(), frequency.unit)
            .toInstant()
            .toEpochMilli()

    /** When the next, not yet created occurrence happens. */
    fun nextOccurrence(zone: ZoneId = ZoneId.systemDefault()): Long =
        occurrenceAt(generatedCount, zone)

    /**
     * Index of the first occurrence after [moment], never less than 1
     * (occurrence 0 is the entry the series was started from).
     * Used when a series is changed, so only future entries are affected.
     */
    fun firstIndexAfter(moment: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        var index = 1
        while (occurrenceAt(index, zone) <= moment && index < MAX_INDEX) index++
        return index
    }

    /**
     * Builds the transaction for occurrence number [index], linked to this series.
     *
     * The fingerprint uses the exact time, so the same occurrence can never
     * be created twice, even if the series is changed or two runs overlap.
     */
    fun toTransaction(index: Int, zone: ZoneId = ZoneId.systemDefault()): Transaction {
        val time = occurrenceAt(index, zone)
        return Transaction(
            type = type,
            amountMinor = amountMinor,
            currency = currency,
            categoryId = categoryId,
            title = title,
            note = note,
            timestamp = time,
            source = TransactionSource.RECURRING,
            dedupeHash = "recurring:$id:$time",
            reviewed = true,
            recurringId = id
        )
    }

    private companion object {
        /** Safety stop for very old start dates. */
        const val MAX_INDEX = 100_000
    }
}