package com.aljaz.spendingtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Dialog for jumping straight to any month and year.
 *
 * The user switches years with the arrows, then taps a month.
 * Changing the year alone selects nothing; only tapping a month does.
 *
 * @param initialMonth the month currently shown; highlighted in the grid
 * @param onMonthSelected called with the chosen month (also used by "Today")
 * @param onDismiss called when the dialog is cancelled
 */
@Composable
fun MonthPickerDialog(
    initialMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    // The year being browsed. Survives screen rotation thanks to rememberSaveable.
    var year by rememberSaveable { mutableIntStateOf(initialMonth.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { year-- }) { Text("<") }
                Text(text = year.toString(), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { year++ }) { Text(">") }
            }
        },
        text = {
            // The 12 months as a grid of 4 rows with 3 months each.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..12).chunked(3).forEach { monthsInRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthsInRow.forEach { monthNumber ->
                            val candidate = YearMonth.of(year, monthNumber)
                            MonthCell(
                                label = Month.of(monthNumber)
                                    .getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                selected = candidate == initialMonth,
                                onClick = { onMonthSelected(candidate) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onMonthSelected(YearMonth.now()) }) { Text("Today") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * One month button in the grid. The currently shown month is drawn as
 * a filled button so the user can see where they are.
 */
@Composable
private fun MonthCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val padding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = padding) {
            Text(text = label, maxLines = 1)
        }
    } else {
        TextButton(onClick = onClick, modifier = modifier, contentPadding = padding) {
            Text(text = label, maxLines = 1)
        }
    }
}