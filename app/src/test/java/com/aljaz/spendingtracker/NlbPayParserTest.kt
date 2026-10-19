package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.automation.parser.NlbPayParser
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Checks the NLB Pay notification parser against real notification texts. */
class NlbPayParserTest {

    private val parser = NlbPayParser()
    private val utc = ZoneOffset.UTC

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun posPurchase() {
        val result = parser.parse(
            "POS NAKUP 19.10.2026 11:52, kartica ***5802, znesek 10,96 EUR, " +
                    "CHUTIS LJUBLJANA SI. Info: 0038614772000. NLB",
            utc
        )
        assertNotNull(result)
        result!!
        assertEquals("POS NAKUP", result.operation)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(1096L, result.amountMinor)
        assertEquals("EUR", result.currency)
        assertEquals(millis(2026, 10, 19, 11, 52), result.timestamp)
        assertEquals("CHUTIS LJUBLJANA SI", result.merchant)
        assertEquals("5802", result.cardLast4)
    }

    @Test
    fun handlesLineBreaksLikeInTheNotification() {
        val result = parser.parse(
            "POS NAKUP 19.10.2026 11:52, kartica\n***5802, znesek 10,96 EUR, CHUTIS\n" +
                    "LJUBLJANA SI. Info: 0038614772000. NLB",
            utc
        )
        assertEquals("CHUTIS LJUBLJANA SI", result?.merchant)
    }

    @Test
    fun amountWithThousandsSeparatorAndMerchantWithDots() {
        val result = parser.parse(
            "SPLET/TEL NAKUP 1.9.2026 9:05, kartica ***5802, znesek 1.234,56 EUR, " +
                    "OTP BANKA D.D.. Info: 123. NLB",
            utc
        )
        assertEquals(123456L, result?.amountMinor)
        assertEquals("OTP BANKA D.D.", result?.merchant)
        assertEquals(millis(2026, 9, 1, 9, 5), result?.timestamp)
    }

    @Test
    fun unknownOperationIsNotGuessed() {
        assertNull(
            parser.parse(
                "PRILIV 19.10.2026 11:52, kartica ***5802, znesek 10,96 EUR, X. Info: 1. NLB",
                utc
            )
        )
    }

    @Test
    fun nonTransactionTextReturnsNull() {
        assertNull(parser.parse("Prijava v NLB Pay je bila uspešna.", utc))
    }

    @Test
    fun acceptsOnlyTheNlbPayPackage() {
        assertTrue(parser.acceptsSender("si.nlbpay.slovenija"))
        assertFalse(parser.acceptsSender("DH d.d."))
        assertFalse(parser.acceptsSender("com.google.android.gms"))
    }
}