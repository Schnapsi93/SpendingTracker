package com.aljaz.spendingtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aljaz.spendingtracker.ui.categories.CategoriesScreen
import com.aljaz.spendingtracker.ui.charts.ChartsScreen
import com.aljaz.spendingtracker.ui.home.HomeScreen
import com.aljaz.spendingtracker.ui.onboarding.OnboardingScreen
import com.aljaz.spendingtracker.ui.review.ReviewScreen
import com.aljaz.spendingtracker.ui.rules.RulesScreen
import com.aljaz.spendingtracker.ui.search.SearchScreen
import com.aljaz.spendingtracker.ui.settings.SettingsScreen
import com.aljaz.spendingtracker.ui.shared.SharedSummaryScreen
import com.aljaz.spendingtracker.ui.sharing.SharedAccountScreen
import com.aljaz.spendingtracker.ui.transactions.AddTransactionScreen

/**
 * The app's navigation graph: which screen is shown for which route,
 * and how screens move between each other.
 *
 * Screens never touch the NavController directly. They receive simple
 * callbacks (like onBackClick), which keeps them easy to test and reuse.
 * @param openReviewRequests changes whenever a notification asks to show the Review screen
 * @param showOnboarding true on first launch: the app starts with the setup guide
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    openReviewRequests: Int = 0,
    showOnboarding: Boolean = false
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Screen.Onboarding.route else Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddTransactionClick = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onTransactionClick = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                },
                onCategoriesClick = {
                    navController.navigate(Screen.Categories.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onReviewClick = {
                    navController.navigate(Screen.Review.route)
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                },
                onChartsClick = { month ->
                    navController.navigate(Screen.Charts.createRoute(month))
                },
                onSharedClick = { month ->
                    navController.navigate(Screen.SharedSummary.createRoute(month))
                }
            )
        }

        // Add mode: no id argument, so the form starts empty.
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(onBackClick = { navController.popBackStack() })
        }

        // Edit mode: the id argument tells the ViewModel what to load.
        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument(TRANSACTION_ID_ARG) { type = NavType.LongType }
            )
        ) {
            AddTransactionScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onBackClick = { navController.popBackStack() },
                onRulesClick = { navController.navigate(Screen.Rules.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onShowSetupGuideClick = { navController.navigate(Screen.Onboarding.route) },
                onSharedAccountClick = { navController.navigate(Screen.SharedAccount.route) }
            )
        }

        composable(Screen.SharedAccount.route) {
            SharedAccountScreen(onBackClick = { navController.popBackStack() })
        }

        // The month argument is optional: "shared_summary" alone opens the current period.
        composable(
            route = Screen.SharedSummary.route,
            arguments = listOf(
                navArgument(SHARED_MONTH_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            SharedSummaryScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Review.route) {
            ReviewScreen(
                onBackClick = { navController.popBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                }
            )
        }

        composable(Screen.Rules.route) {
            RulesScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(Screen.EditTransaction.createRoute(id))
                }
            )
        }

        // The month argument is optional: "charts" alone opens the current period.
        composable(
            route = Screen.Charts.route,
            arguments = listOf(
                navArgument(CHARTS_MONTH_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ChartsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = { openSettings ->
                    // Start fresh on Home, with the guide removed from the back stack,
                    // so the back button never returns into it.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    if (openSettings) navController.navigate(Screen.Settings.route)
                }
            )
        }
    }

    // A tapped notification asks for the Review screen.
    LaunchedEffect(openReviewRequests) {
        if (openReviewRequests > 0) {
            navController.navigate(Screen.Review.route) {
                launchSingleTop = true // never stack two Review screens
            }
        }
    }
}