package com.aljaz.spendingtracker.domain.rules

import java.util.Locale

/** Text simplification shared by rules and the texts they are matched against. */
object RuleText {

    /**
     * Uppercases the text, turns everything that isn't a letter or digit
     * into a space, and collapses repeated spaces.
     *
     * Examples:
     * - "eBay O*19-15244-76499" becomes "EBAY O 19 15244 76499"
     * - "  bs   logo " becomes "BS LOGO"
     *
     * Letters like č, š and ž are kept.
     */
    fun normalize(text: String): String =
        text.uppercase(Locale.ROOT)
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}