package com.aljaz.spendingtracker.core

import java.math.BigDecimal

/**
 * Converts user-typed amounts into minor units (cents).
 */
object MoneyParser {

    /**
     * Parses text such as "12.50", "12,50" or "7" into minor units.
     *
     * Accepts either a dot or a comma as the decimal separator.
     * Thousands separators are not supported.
     *
     * @param input the raw text from the amount field
     * @return the amount in minor units (e.g. 1250), or null if the text
     *         is empty, not a number, zero or negative, or has more than
     *         two decimal places
     */
    fun parseToMinor(input: String): Long? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) return null

        val value = normalized.toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null   // must be greater than zero
        if (value.scale() > 2) return null     // no fractions of a cent

        return try {
            value.movePointRight(2).longValueExact()
        } catch (e: ArithmeticException) {
            null // number too large to store
        }
    }

    /**
     * Turns minor units back into plain text for an input field,
     * e.g. 1250 becomes "12.50". No currency symbol or grouping.
     */
    fun formatForInput(amountMinor: Long): String =
        BigDecimal.valueOf(amountMinor, 2).toPlainString()
}