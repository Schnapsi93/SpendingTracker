package com.aljaz.spendingtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.Transaction
import com.aljaz.spendingtracker.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * One transaction in a list: color dot, title, category and date on the
 * left, amount on the right. Used by Home and the Review screen.
 *
 * @param category the transaction's category, or null if uncategorized
 * @param onClick called when the row is tapped (usually opens the edit form)
 * @param showNewBadge whether unreviewed transactions get a "NEW" label
 * @param trailingContent optional extra content at the far right, e.g. a button
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showNewBadge: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "-"
    val amountColor =
        if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val date = Instant.ofEpochMilli(transaction.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    // Uncategorized transactions get a neutral grey dot.
    val dotColor = category?.colorArgb
        ?: MaterialTheme.colorScheme.outlineVariant.toArgb()
    val categoryName = category?.name ?: "Uncategorized"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorDot(colorArgb = dotColor)
        Spacer(modifier = Modifier.width(12.dp))

        // Takes all remaining width, so long titles never push the amount off screen.
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // fill = false: the title only takes the space it needs,
                    // so the badge sits right after it.
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (showNewBadge && !transaction.reviewed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "$categoryName · $date",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = sign + MoneyFormatter.format(transaction.amountMinor, transaction.currency),
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge
        )
        trailingContent?.invoke()
    }
}