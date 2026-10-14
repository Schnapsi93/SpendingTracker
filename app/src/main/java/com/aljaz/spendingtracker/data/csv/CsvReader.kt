package com.aljaz.spendingtracker.data.csv

/**
 * Splits CSV text into rows of fields.
 *
 * Follows the same rules as [escapeCsvField]: a field wrapped in quotes
 * may contain separators, line breaks and doubled quotes ("" = one ").
 * A leading BOM and empty lines are ignored.
 *
 * Example: `a;"b;c";"say ""hi"""` becomes ["a", "b;c", "say \"hi\""].
 */
internal fun parseCsv(
    text: String,
    separator: Char = CsvFormat.SEPARATOR
): List<List<String>> {
    val input = text.removePrefix(CsvFormat.BOM)
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < input.length) {
        val c = input[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < input.length && input[i + 1] == '"') {
                    field.append('"') // doubled quote = one literal quote
                    i++
                } else {
                    inQuotes = false // closing quote
                }
            } else {
                field.append(c) // anything goes inside quotes, even ; and line breaks
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                separator -> {
                    row.add(field.toString())
                    field.clear()
                }
                '\r' -> Unit // ignored; '\n' ends the row
                '\n' -> {
                    row.add(field.toString())
                    field.clear()
                    rows.add(row.toList())
                    row.clear()
                }
                else -> field.append(c)
            }
        }
        i++
    }

    // The last row, if the file doesn't end with a line break.
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row.toList())
    }

    // Skip empty lines (a row with a single blank field).
    return rows.filterNot { it.size == 1 && it[0].isBlank() }
}