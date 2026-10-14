package com.aljaz.spendingtracker.data.csv

import java.time.format.DateTimeFormatter

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