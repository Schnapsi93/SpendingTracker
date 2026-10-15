package com.aljaz.spendingtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aljaz.spendingtracker.core.periodEndInclusive
import com.aljaz.spendingtracker.core.periodStart
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Period title with previous / next arrows, used on Home and Charts.
 * With a month start day other than 1, the exact dates are shown under
 * the name (e.g. "15 Oct – 14 Nov"). Tapping the title opens [MonthPickerDialog].
 *
 * @param currentPeriod the period containing today, used by the picker's "Today"
 */
@Composable
fun PeriodHeader(
    month: YearMonth,
    monthStartDay: Int,
    currentPeriod: YearMonth,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    // Whether the picker dialog is open. Purely visual state, so it lives here.
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPreviousClick) { Text("<") }

        // The period name takes all the free space and opens the picker.
        TextButton(
            onClick = { showPicker = true },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy")),
                    style = MaterialTheme.typography.titleLarge
                )
                if (monthStartDay != 1) {
                    val dayFormat = DateTimeFormatter.ofPattern("d MMM")
                    Text(
                        text = "${month.periodStart(monthStartDay).format(dayFormat)} – " +
                                month.periodEndInclusive(monthStartDay).format(dayFormat),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        TextButton(onClick = onNextClick) { Text(">") }
    }

    if (showPicker) {
        MonthPickerDialog(
            initialMonth = month,
            today = currentPeriod,
            onMonthSelected = { picked ->
                showPicker = false
                onMonthSelected(picked)
            },
            onDismiss = { showPicker = false }
        )
    }
}