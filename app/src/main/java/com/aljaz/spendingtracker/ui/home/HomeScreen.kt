package com.aljaz.spendingtracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Placeholder home screen. Later it will show the month overview
 * (income, expenses, balance) and the list of transactions.
 *
 * @param onAddTransactionClick called when the user wants to add a transaction
 */
@Composable
fun HomeScreen(
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home (month overview coming soon)")
        Button(onClick = onAddTransactionClick) {
            Text(text = "Add transaction")
        }
    }
}