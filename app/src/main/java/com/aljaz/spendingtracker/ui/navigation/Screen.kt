package com.aljaz.spendingtracker.ui.navigation

import java.time.YearMonth


/** Name of the navigation argument that carries a transaction id. */
const val TRANSACTION_ID_ARG = "transactionId"


/** Name of the optional navigation argument with the period to show, e.g. "2026-10". */
const val CHARTS_MONTH_ARG = "month"

/** Name of the optional navigation argument with the period the shared summary opens on. */
const val SHARED_MONTH_ARG = "month"

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


    /** Charts for one period; opens on the period passed in, or the current one. */
    data object Charts : Screen("charts?$CHARTS_MONTH_ARG={$CHARTS_MONTH_ARG}") {

        /** Builds the route for a period, e.g. "charts?month=2026-10". */
        fun createRoute(month: YearMonth) = "charts?$CHARTS_MONTH_ARG=$month"
    }

    /** First-launch setup guide; can be reopened from Settings. */
    data object Onboarding : Screen("onboarding")

    /** Login to the shared expenses server; opened from Settings. */
    data object SharedAccount : Screen("shared_account")

    /** Shared expenses of one period and who owes whom; opens on the period passed in. */
    data object SharedSummary : Screen("shared_summary?$SHARED_MONTH_ARG={$SHARED_MONTH_ARG}") {

        /** Builds the route for a period, e.g. "shared_summary?month=2026-10". */
        fun createRoute(month: YearMonth) = "shared_summary?$SHARED_MONTH_ARG=$month"
    }
}