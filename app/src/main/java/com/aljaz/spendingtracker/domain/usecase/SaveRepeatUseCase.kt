package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.RecurringTransaction
import com.aljaz.spendingtracker.domain.model.RepeatFrequency
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.repository.RecurringTransactionRepository
import com.aljaz.spendingtracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Applies the "Repeating transaction" box of the form to a saved transaction.
 *
 * - unticked, and it belonged to a series: the series stops (entries stay)
 * - ticked, no series yet: a series starts from this entry; missed
 *   occurrences up to today are added
 * - ticked, already in a series: the series takes over this entry's values
 *   and frequency, counted from its date; only future entries change
 */
class SaveRepeatUseCase @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val generateDue: GenerateDueRecurringTransactionsUseCase
) {

    /**
     * @param transaction the transaction as saved (with its real id and cleaned values)
     * @param repeats whether the box is ticked
     * @param frequency the chosen frequency (ignored when not repeating)
     * @param now current moment, UTC epoch millis (a parameter so tests can fix it)
     */
    suspend operator fun invoke(
        transaction: Transaction,
        repeats: Boolean,
        frequency: RepeatFrequency,
        now: Long = System.currentTimeMillis()
    ) {
        val series = transaction.recurringId?.let { recurringRepository.getById(it) }

        when {
            !repeats -> series?.let { recurringRepository.delete(it) }
            series == null -> start(transaction, frequency)
            else -> change(series, transaction, frequency, now)
        }
    }

    /** Starts a new series with [transaction] as its first occurrence. */
    private suspend fun start(transaction: Transaction, frequency: RepeatFrequency) {
        val template = RecurringTransaction(
            type = transaction.type,
            amountMinor = transaction.amountMinor,
            currency = transaction.currency,
            frequency = frequency,
            startTimestamp = transaction.timestamp,
            title = transaction.title,
            generatedCount = 1 // occurrence 0 is this transaction
        ).withValuesOf(transaction)

        val seriesId = recurringRepository.add(template)
        transactionRepository.update(transaction.copy(recurringId = seriesId))
        generateDue() // catch up if the date lies in the past
    }

    /**
     * Updates an existing series. It restarts from this entry's date, but
     * skips everything up to now, so nothing already added is created again.
     */
    private suspend fun change(
        series: RecurringTransaction,
        transaction: Transaction,
        frequency: RepeatFrequency,
        now: Long
    ) {
        val changed = series.withValuesOf(transaction).copy(
            frequency = frequency,
            startTimestamp = transaction.timestamp
        )
        recurringRepository.update(changed.copy(generatedCount = changed.firstIndexAfter(now)))
    }

    /** Copies what future repeats should look like from [transaction]. */
    private fun RecurringTransaction.withValuesOf(transaction: Transaction) = copy(
        type = transaction.type,
        amountMinor = transaction.amountMinor,
        currency = transaction.currency,
        categoryId = transaction.categoryId,
        title = transaction.title,
        note = transaction.note
    )
}