package com.aljaz.spendingtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Calendar-like grid of days 1–31 for choosing the month start day.
 *
 * @param selected the day currently set; drawn as a filled button
 * @param onDaySelected called with the tapped day
 * @param onDismiss called when the dialog is cancelled
 */
@Composable
fun DayPickerDialog(
    selected: Int,
    onDaySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Month starts on day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..31).chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            val padding = PaddingValues(0.dp)
                            if (day == selected) {
                                Button(
                                    onClick = { onDaySelected(day) },
                                    contentPadding = padding,
                                    modifier = Modifier.weight(1f)
                                ) { Text(day.toString()) }
                            } else {
                                TextButton(
                                    onClick = { onDaySelected(day) },
                                    contentPadding = padding,
                                    modifier = Modifier.weight(1f)
                                ) { Text(day.toString()) }
                            }
                        }
                        // Keep the last, shorter row aligned with the others.
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                Text(
                    text = "Days 29–31: in shorter months the period starts on the last day.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}