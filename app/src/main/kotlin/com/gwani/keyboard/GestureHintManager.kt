package com.gwani.keyboard

import android.content.Context

// -----------------------------------------------------------
// GESTURE HINT MANAGER
// Controls when the hint bubble appears above a key.
//
// Two situations show a hint:
//   1. First 20 taps on a gesture key — onboarding only, then stops forever
//   2. Long press — always shows, reminds user it's a swipe not a press
//
// Tap count is saved to SharedPreferences so it persists across sessions.
// Once the user hits 20 gesture-key taps, auto-hints are gone for good.
// -----------------------------------------------------------

class GestureHintManager(context: Context) {

    // SharedPreferences = permanent simple storage that survives app restarts
    private val prefs = context.getSharedPreferences(
        "gwani_hints",
        Context.MODE_PRIVATE
    )

    // Stop auto-hinting after this many taps on gesture keys
    // 20 is enough for the user to notice the pattern without being annoying
    private val onboardingLimit = 20

    // Load saved tap count from storage (0 if first time)
    private var gestureTapCount = prefs.getInt("gesture_tap_count", 0)

    // Call this every time a gesture key is tapped (keys that have swipeUp)
    // Returns true = show the hint, false = don't show
    fun onGestureKeyTapped(): Boolean {
        // Once past the limit, never auto-show again
        if (gestureTapCount >= onboardingLimit) return false

        gestureTapCount++

        // Save to storage every 5 taps to avoid writing too often
        if (gestureTapCount % 5 == 0) {
            prefs.edit().putInt("gesture_tap_count", gestureTapCount).apply()
        }

        return true
    }

    // Long press always shows the hint — no condition
    // This reminds the user: "this is a swipe, not a hold"
    fun onLongPress(): Boolean = true

    // How long the hint bubble stays on screen in milliseconds
    val hintDurationMs = 1000L
}
