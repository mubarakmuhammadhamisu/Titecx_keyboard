package com.gwani.keyboard

import android.content.Context

// -----------------------------------------------------------
// GESTURE HINT MANAGER
// Decides when to show the hint bubble above a key.
//
// Rules:
//   - New users: hint shows automatically on every key tap
//   - After 100 total taps: auto-hints stop
//   - Long press: hint always shows regardless of tap count
//
// "New user" is tracked in SharedPreferences so it
// persists across keyboard sessions.
// -----------------------------------------------------------

class GestureHintManager(context: Context) {

    // SharedPreferences = simple key-value storage that survives app restarts
    // We store total tap count here
    private val prefs = context.getSharedPreferences(
        "gwani_hints",      // file name
        Context.MODE_PRIVATE // only our app can read this
    )

    // How many taps before auto-hints stop
    private val newUserTapLimit = 100

    // Current tap count loaded from storage
    private var tapCount = prefs.getInt("tap_count", 0)

    // Call this every time a key is tapped
    // Returns true if we should show the hint automatically
    fun onKeyTapped(): Boolean {
        tapCount++

        // Save updated count to storage every 10 taps
        // (saving every tap would be too slow)
        if (tapCount % 10 == 0) {
            prefs.edit().putInt("tap_count", tapCount).apply()
        }

        // Show auto-hint only if user is still in the new-user phase
        return tapCount <= newUserTapLimit
    }

    // Long press always shows hint — no condition needed
    fun onLongPress(): Boolean = true

    // How long the hint stays visible in milliseconds
    val hintDurationMs = 1000L
}
