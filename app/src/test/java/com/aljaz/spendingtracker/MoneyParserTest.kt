package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.core.MoneyParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Checks that typed amounts are converted to cents correctly. */
class MoneyParserTest {

    @Test
    fun formatForInputRoundTrips() {
        assertEquals("12.50", MoneyParser.formatForInput(1250))
        assertEquals(1250L, MoneyParser.parseToMinor(MoneyParser.formatForInput(1250)))
    }

    fun parsesDotAndCommaDecimals() {
        assertEquals(1250L, MoneyParser.parseToMinor("12.50"))
        assertEquals(1250L, MoneyParser.parseToMinor("12,50"))
    }

    @Test
    fun parsesWholeNumbersAndShortDecimals() {
        assertEquals(700L, MoneyParser.parseToMinor("7"))
        assertEquals(750L, MoneyParser.parseToMinor("7.5"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(MoneyParser.parseToMinor(""))
        assertNull(MoneyParser.parseToMinor("abc"))
        assertNull(MoneyParser.parseToMinor("0"))
        assertNull(MoneyParser.parseToMinor("-5"))
        assertNull(MoneyParser.parseToMinor("1.234"))
    }
}