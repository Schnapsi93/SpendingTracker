package com.aljaz.spendingtracker.ui.navigation

/** Name of the navigation argument that carries a transaction id. */
const val TRANSACTION_ID_ARG = "transactionId"

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

    /** Empty form for adding a new transaction. */
    data object AddTransaction : Screen("add_transaction")

    /** The same form, pre-filled with an existing transaction. */
    data object EditTransaction : Screen("edit_transaction/{$TRANSACTION_ID_ARG}") {

        /** Builds the real route for one transaction, e.g. "edit_transaction/5". */
        fun createRoute(id: Long) = "edit_transaction/$id"
    }

    /** List of categories, where they can be added, edited and deleted. */
    data object Categories : Screen("categories")


    /** App settings: export, and later the cloud backup. */
    data object Settings : Screen("settings")

    /** Automatic transactions waiting for the user's confirmation. */
    data object Review : Screen("review")

    /** Keyword rules for automatic categorization. */
    data object Rules : Screen("rules")


    /** Search across all transactions. */
    data object Search : Screen("search")
}


