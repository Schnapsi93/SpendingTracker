package com.aljaz.spendingtracker.core

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats stored amounts (minor units) for display.
 */
object MoneyFormatter {

    /**
     * Formats an amount using the phone's language and number conventions.
     *
     * @param amountMinor amount in minor units, e.g. 1250
     * @param currencyCode ISO 4217 code, e.g. "EUR"
     * @return text such as "€12.50" (the exact look depends on the locale)
     */
    fun format(
        amountMinor: Long,
        currencyCode: String,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(currencyCode)
        // valueOf(1250, 2) means 1250 with 2 decimal places = 12.50
        return formatter.format(BigDecimal.valueOf(amountMinor, 2))
    }
}