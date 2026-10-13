package com.aljaz.spendingtracker.ui.navigation

/**
 * Every screen in the app, with its navigation route.
 *
 * To add a new screen: add an object here, then register it in
 * [AppNavHost]. Keeping all routes in one place avoids typos in
 * route strings scattered around the code.
 */
sealed class Screen(val route: String) {

    /** Month overview: totals and the list of transactions. */
    data object Home : Screen("home")

    /** Form for adding a transaction manually. */
    data object AddTransaction : Screen("add_transaction")
}