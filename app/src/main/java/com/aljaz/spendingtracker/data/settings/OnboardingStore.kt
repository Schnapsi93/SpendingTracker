package com.aljaz.spendingtracker.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers whether the user has finished (or skipped) the first-launch setup guide. */
@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private companion object {
        const val PREFS_NAME = "onboarding"
        const val KEY_COMPLETED = "completed"
    }
}