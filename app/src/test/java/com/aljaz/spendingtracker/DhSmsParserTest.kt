package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.automation.parser.DhSmsParser
import com.aljaz.spendingtracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Checks the DH SMS parser against real message formats. */
class DhSmsParserTest {

    private val parser = DhSmsParser()
    private val utc = ZoneOffset.UTC

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    /** Parses [text] and checks every extracted field. */
    private fun assertParsed(
        text: String,
        operation: String,
        amountMinor: Long,
        merchant: String,
        timestamp: Long
    ) {
        val result = parser.parse(text, utc)
        assertNotNull("Message was not recognized: $text", result)
        result!!
        assertEquals(operation, result.operation)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(amountMinor, result.amountMinor)
        assertEquals("EUR", result.currency)
        assertEquals(merchant, result.merchant)
        assertEquals(timestamp, result.timestamp)
        assertEquals("1754", result.cardLast4)
    }

    @Test
    fun onlinePurchaseWithTwoStarCardMask() = assertParsed(
        "Obvestilo o transakciji: SPLET/TEL NAKUP, 14.10.2026 17:18, kartica **1754, znesek 21,96 EUR, ANTHROPIC CLAUDE SUB. DH d.d.",
        "SPLET/TEL NAKUP", 2196, "ANTHROPIC CLAUDE SUB", millis(2026, 10, 14, 17, 18)
    )

    @Test
    fun posPurchaseAtFuelStation() = assertParsed(
        "Obvestilo o transakciji: POS NAKUP, 13.10.2026 15:37, kartica ***1754, znesek 161,77 EUR, BS LOGO BREZOVICA. DH d.d.",
        "POS NAKUP", 16177, "BS LOGO BREZOVICA", millis(2026, 10, 13, 15, 37)
    )

    @Test
    fun removesTrailingDashFromMerchant() = assertParsed(
        "Obvestilo o transakciji: POS NAKUP, 11.10.2026 09:22, kartica ***1754, znesek 30,15 EUR, PETROL BS LJUBLJANA -. DH d.d.",
        "POS NAKUP", 3015, "PETROL BS LJUBLJANA", millis(2026, 10, 11, 9, 22)
    )

    @Test
    fun keepsOrderNumbersInMerchant() = assertParsed(
        "Obvestilo o transakciji: SPLET/TEL NAKUP, 06.10.2026 20:05, kartica ***1754, znesek 6,49 EUR, eBay O*19-15244-76499. DH d.d.",
        "SPLET/TEL NAKUP", 649, "eBay O*19-15244-76499", millis(2026, 10, 6, 20, 5)
    )

    @Test
    fun atmWithdrawalKeepsDotsInMerchant() = assertParsed(
        "Obvestilo o transakciji: BA DVIG, 17.09.2026 15:46, kartica ***1754, znesek 600,00 EUR, OTP BANKA D.D.. DH d.d.",
        "BA DVIG", 60000, "OTP BANKA D.D.", millis(2026, 9, 17, 15, 46)
    )

    @Test
    fun foreignOnlineShop() = assertParsed(
        "Obvestilo o transakciji: SPLET/TEL NAKUP, 13.09.2026 22:19, kartica ***1754, znesek 191,93 EUR, Bike24 GmbH. DH d.d.",
        "SPLET/TEL NAKUP", 19193, "Bike24 GmbH", millis(2026, 9, 13, 22, 19)
    )

    @Test
    fun smallOnlineAmount() = assertParsed(
        "Obvestilo o transakciji: SPLET/TEL NAKUP, 02.09.2026 10:03, kartica ***1754, znesek 1,72 EUR, EasyPark SI. DH d.d.",
        "SPLET/TEL NAKUP", 172, "EasyPark SI", millis(2026, 9, 2, 10, 3)
    )

    @Test
    fun supermarketPurchase() = assertParsed(
        "Obvestilo o transakciji: POS NAKUP, 31.07.2026 20:41, kartica ***1754, znesek 9,14 EUR, SUPERMARKET BREZOVICA. DH d.d.",
        "POS NAKUP", 914, "SUPERMARKET BREZOVICA", millis(2026, 7, 31, 20, 41)
    )

    @Test
    fun shortMerchantName() = assertParsed(
        "Obvestilo o transakciji: POS NAKUP, 04.07.2026 10:16, kartica ***1754, znesek 24,00 EUR, ZET. DH d.d.",
        "POS NAKUP", 2400, "ZET", millis(2026, 7, 4, 10, 16)
    )

    @Test
    fun handlesThousandsSeparator() {
        val result = parser.parse(
            "Obvestilo o transakciji: POS NAKUP, 01.10.2026 12:00, kartica ***1754, znesek 1.234,56 EUR, TEST SHOP. DH d.d.",
            utc
        )
        assertEquals(123456L, result?.amountMinor)
    }

    @Test
    fun ignoresMessagesThatAreNotTransactions() {
        assertNull(parser.parse("Vaša enkratna koda za prijavo je 123456. DH d.d.", utc))
        assertNull(parser.parse("Hello, how are you?", utc))
    }

    @Test
    fun ignoresUnknownOperations() {
        assertNull(
            parser.parse(
                "Obvestilo o transakciji: NEKAJ NOVEGA, 01.10.2026 12:00, kartica ***1754, znesek 5,00 EUR, SHOP. DH d.d.",
                utc
            )
        )
    }
}