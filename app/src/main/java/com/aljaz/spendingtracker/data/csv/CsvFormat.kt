package com.aljaz.spendingtracker.data.csv

import java.time.format.DateTimeFormatter
import com.aljaz.spendingtracker.domain.model.TransactionType

/**
 * The rules of the app's CSV files, in one place, so export and
 * import always agree on the format.
 *
 * The format targets spreadsheet programs with European settings
 * (Excel, LibreOffice): semicolons between columns and a decimal comma.
 */
object CsvFormat {

    /** Column separator. */
    const val SEPARATOR = ';'

    /** Windows-style line ending; handled correctly by every common program. */
    const val LINE_END = "\r\n"

    /**
     * Invisible "byte order mark" written at the very start of the file.
     * It tells Excel the file is UTF-8, so č, š and ž display correctly.
     */
    const val BOM = "\uFEFF"

    /** Column titles, in order. */
    val HEADER: List<String> = listOf(
        "Date", "Type", "Amount", "Currency", "Category", "Title", "Note", "Source"
    )

    /** How dates are written, e.g. "2026-10-11 19:38". */
    val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")


    /**
     * Date formats accepted when importing. The first is the app's own;
     * the others cover files re-saved by Excel or LibreOffice with
     * Slovenian/European settings.
     */
    val IMPORT_DATE_TIME_FORMATS: List<DateTimeFormatter> = listOf(
        DATE_TIME_FORMAT,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("d.M.yyyy H:mm"),
        DateTimeFormatter.ofPattern("d.M.yyyy H:mm:ss"),
        DateTimeFormatter.ofPattern("d. M. yyyy H:mm")
    )

    /** Text used in the Type column. */
    const val TYPE_EXPENSE = "Expense"
    const val TYPE_INCOME = "Income"


    /** Section titles in a complete backup file; each starts its own section. */
    const val SECTION_TRANSACTIONS = "[Transactions]"
    const val SECTION_CATEGORIES = "[Categories]"
    const val SECTION_RULES = "[Rules]"

    /** Column titles of the categories section. */
    val CATEGORY_HEADER: List<String> = listOf("Name", "Type", "Color")

    /** Column titles of the keyword rules section. */
    val RULE_HEADER: List<String> = listOf("Keyword", "Looks at", "Category", "Category type")

    /** Text used in the "Looks at" column of the rules section. */
    const val FIELD_MERCHANT = "Merchant"
    const val FIELD_OPERATION = "Operation"

    /** Expense/Income label for a type. */
    fun formatType(type: TransactionType): String = when (type) {
        TransactionType.EXPENSE -> TYPE_EXPENSE
        TransactionType.INCOME -> TYPE_INCOME
    }

    /** Reads an Expense/Income label, ignoring case; null if unknown. */
    fun parseType(text: String): TransactionType? = when (text.trim().lowercase()) {
        TYPE_EXPENSE.lowercase() -> TransactionType.EXPENSE
        TYPE_INCOME.lowercase() -> TransactionType.INCOME
        else -> null
    }

    /** 0xFF4CAF50 becomes "#4CAF50". Colors are always fully opaque, so alpha is left out. */
    fun formatColor(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    /** "#4CAF50" becomes a fully opaque ARGB value; null if the text isn't a valid color. */
    fun parseColor(text: String): Int? =
        text.trim()
            .removePrefix("#")
            .takeIf { it.length == 6 }
            ?.toLongOrNull(16)
            ?.let { (it or 0xFF000000).toInt() }
}

/**
 * Prepares one value for a CSV cell.
 *
 * Values containing the separator, a quote or a line break are wrapped
 * in quotes, and quotes inside them are doubled. This is the standard
 * CSV rule, understood by Excel, LibreOffice and other programs.
 *
 * Example: `Bar; "Pri Joži"` becomes `"Bar; ""Pri Joži"""`.
 */
internal fun escapeCsvField(value: String): String {
    val needsQuotes = value.any {
        it == CsvFormat.SEPARATOR || it == '"' || it == '\n' || it == '\r'
    }
    if (!needsQuotes) return value
    return "\"" + value.replace("\"", "\"\"") + "\""
}


/** One complete CSV line: escaped values joined by the separator, plus a line end. */
internal fun csvRow(fields: List<String>): String =
    fields.joinToString(CsvFormat.SEPARATOR.toString()) { escapeCsvField(it) } + CsvFormat.LINE_END

/** True for a row like "[Categories]" that starts a new section in a backup file. */
internal fun isSectionMarker(row: List<String>): Boolean =
    row.size == 1 && row[0].trim().let { it.startsWith("[") && it.endsWith("]") }

/**
 * Splits a table into sections at marker rows like "[Categories]".
 * A file without markers (e.g. an older transactions-only export) is one
 * section. The marker rows themselves are dropped, so each section starts
 * with its header row.
 */
internal fun splitSections(table: List<List<String>>): List<List<List<String>>> {
    val sections = mutableListOf<List<List<String>>>()
    var current = mutableListOf<List<String>>()
    for (row in table) {
        if (isSectionMarker(row)) {
            if (current.isNotEmpty()) sections.add(current)
            current = mutableListOf()
        } else {
            current.add(row)
        }
    }
    if (current.isNotEmpty()) sections.add(current)
    return sections
}