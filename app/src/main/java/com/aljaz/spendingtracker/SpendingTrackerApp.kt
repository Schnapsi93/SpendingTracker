package com.aljaz.spendingtracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * The @HiltAndroidApp annotation tells Hilt to generate the dependency
 * container that lives as long as the app process does.
 */
@HiltAndroidApp
class SpendingTrackerApp : Application()