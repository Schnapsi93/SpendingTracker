package com.aljaz.spendingtracker

import com.aljaz.spendingtracker.data.csv.parseCsv
import com.aljaz.spendingtracker.data.csv.splitSections
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks how a complete backup file is split into its sections. */
class BackupFileTest {

    @Test
    fun splitsAtSectionMarkers() {
        val text = "[Transactions]\r\n" +
                "Date;Type;Amount;Currency;Category;Title;Note;Source\r\n" +
                "2026-10-01 10:00;Expense;5,00;EUR;;Test;;MANUAL\r\n" +
                "\r\n" +
                "[Categories]\r\n" +
                "Name;Type;Color\r\n" +
                "Groceries;Expense;#4CAF50\r\n" +
                "\r\n" +
                "[Rules]\r\n" +
                "Keyword;Looks at;Category;Category type\r\n"

        val sections = splitSections(parseCsv(text))

        assertEquals(3, sections.size)
        assertEquals("Date", sections[0][0][0])   // each section starts with its header
        assertEquals(2, sections[0].size)          // header + 1 transaction
        assertEquals("Name", sections[1][0][0])
        assertEquals("Keyword", sections[2][0][0])
        assertEquals(1, sections[2].size)          // header only: no rules
    }

    @Test
    fun treatsAFileWithoutMarkersAsOneSection() {
        val text = "Date;Type;Amount;Currency;Category;Title;Note;Source\r\n" +
                "2026-10-01 10:00;Expense;5,00;EUR;;Test;;MANUAL\r\n"

        val sections = splitSections(parseCsv(text))

        assertEquals(1, sections.size)
        assertEquals(2, sections[0].size)
    }
}