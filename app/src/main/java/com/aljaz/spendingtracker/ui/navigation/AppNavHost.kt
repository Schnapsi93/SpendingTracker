package com.aljaz.spendingtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aljaz.spendingtracker.ui.home.HomeScreen
import com.aljaz.spendingtracker.ui.transactions.AddTransactionScreen

/**
 * The app's navigation graph: which screen is shown for which route,
 * and how screens move between each other.
 *
 * Screens never touch the NavController directly. They receive simple
 * callbacks (like onBackClick), which keeps them easy to test and reuse.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddTransactionClick = {
                    navController.navigate(Screen.AddTransaction.route)
                }
            )
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}