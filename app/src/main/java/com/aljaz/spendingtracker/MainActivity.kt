package com.aljaz.spendingtracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.aljaz.spendingtracker.data.settings.OnboardingStore
import com.aljaz.spendingtracker.ui.navigation.AppNavHost
import com.aljaz.spendingtracker.ui.theme.SpendingTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The app's only activity; every screen is a Compose destination inside it.
 *
 * It decides whether the setup guide is shown at start, and handles the
 * "open the Review screen" request that comes with a tapped notification,
 * whether the app was closed or already open.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Filled in by Hilt; tells whether the setup guide still needs to be shown. */
    @Inject
    lateinit var onboardingStore: OnboardingStore

    /**
     * Counts "open Review" requests from notifications. The UI reacts to
     * every change, so tapping several notifications in a row works.
     */
    private var openReviewRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Hilt fills the @Inject fields here
        enableEdgeToEdge()

        // Only on a fresh start; after a screen rotation the request was already handled.
        if (savedInstanceState == null) handleIntent(intent)

        // Decided once per start: show the guide until it has been finished or skipped.
        val showOnboarding = !onboardingStore.isCompleted()

        setContent {
            SpendingTrackerTheme {
                AppNavHost(
                    openReviewRequests = openReviewRequests,
                    showOnboarding = showOnboarding
                )
            }
        }
    } // <- end of onCreate

    /** Called instead of onCreate when a notification is tapped while the app is open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_REVIEW, false) == true) {
            openReviewRequests++
        }
    }

    companion object {
        /** Intent extra set by notifications: "show the Review screen". */
        const val EXTRA_OPEN_REVIEW = "open_review"
    }
} // <- end of the class