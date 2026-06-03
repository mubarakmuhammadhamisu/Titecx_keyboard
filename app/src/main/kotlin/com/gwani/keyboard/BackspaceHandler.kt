 package com.gwani.keyboard

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

// -----------------------------------------------------------
// BACKSPACE HANDLER
// Two scenarios:
//   1. Quick tap  — deletes one character immediately on touch down
//   2. Long press — after 400ms hold, deletes continuously every 50ms
//
// Safety mechanism:
// deleteRunnable checks BOTH isLongPressing flag AND view.isPressed
// This catches cases where Android misses the ACTION_UP event
// If the button is no longer physically pressed, deletion stops
// -----------------------------------------------------------

class BackspaceHandler(
    private val onDelete: () -> Unit
) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressing = false

    // Continuous deletion loop
    // Runs every 50ms while long pressing
    // Double checks both the flag AND the physical pressed state
    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isLongPressing && currentView?.isPressed == true) {
                onDelete()
                handler.postDelayed(this, 50)
            } else {
                // Finger lifted but Android missed the UP event
                // Force stop here as a safety net
                stopDeletion()
            }
        }
    }

    // Scheduled after 400ms of holding
    // Only activates long press if button is still physically pressed
    private val longPressRunnable = Runnable {
        if (currentView?.isPressed == true) {
            isLongPressing = true
            handler.post(deleteRunnable)
        }
    }

    // Holds reference to the view so deleteRunnable can check isPressed
    private var currentView: View? = null

    // Cancels everything and resets state cleanly
    private fun stopDeletion() {
        isLongPressing = false
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(deleteRunnable)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        currentView = view

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                // Explicitly mark view as pressed
                // Android sometimes delays this so we set it manually
                view.isPressed = true
                isLongPressing = false

                // Delete one character immediately on first touch
                onDelete()

                // Start 400ms countdown to long press
                handler.postDelayed(longPressRunnable, 400)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Clear pressed state first
                // deleteRunnable checks this — setting false stops the loop
                view.isPressed = false
                stopDeletion()
                view.performClick()
                return true
            }
        }
        return false
    }
}
