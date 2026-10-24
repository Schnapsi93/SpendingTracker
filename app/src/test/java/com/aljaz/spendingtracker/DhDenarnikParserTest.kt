package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.automation.parser.DhDenarnikParser
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Checks the DH Denarnik notification parser against real notification texts. */
class DhDenarnikParserTest {

    private val parser = DhDenarnikParser()
    private val utc = ZoneOffset.UTC

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun incomingTransferWithSpaceBeforeEuroSign() {
        val result = parser.parse("23.10.2026 - Prejeli ste 5.00 € od Sabina P.", utc)
        assertNotNull(result)
        result!!
        assertEquals("PREJELI STE", result.operation)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(500L, result.amountMinor)
        assertEquals("EUR", result.currency)
        assertEquals(millis(2026, 10, 23, 0, 0), result.timestamp) // no time: start of day
        assertEquals("Sabina P.", result.merchant)
        assertNull(result.cardLast4)
    }

    @Test
    fun incomingTransferWithoutSpaceBeforeEuroSign() {
        val result = parser.parse("23.10.2026 - Prejeli ste 31.05€ od Sabina P.", utc)
        assertEquals(3105L, result?.amountMinor)
        assertEquals("Sabina P.", result?.merchant)
    }

    @Test
    fun cardPaymentTextIsNotHandledHere() {
        // DH card payments come by SMS; this parser only knows transfers.
        assertNull(
            parser.parse(
                "POS NAKUP 19.10.2026 11:52, kartica ***5802, znesek 10,96 EUR, X. Info: 1. NLB",
                utc
            )
        )
    }

    @Test
    fun acceptsOnlyTheDhDenarnikPackage() {
        assertTrue(parser.acceptsSender("si.delavskahranilnica.dhdenarnik"))
        assertFalse(parser.acceptsSender("si.nlbpay.slovenija"))
        assertFalse(parser.acceptsSender("DH d.d."))
    }
}