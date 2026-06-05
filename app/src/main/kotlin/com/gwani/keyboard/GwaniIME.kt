package com.gwani.keyboard

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.WindowManager

class GwaniIME : InputMethodService() {

    private lateinit var keyboardView: KeyboardView

    // Future top settings bar height in dp — set to 0 now, change this one number later
    // when the settings bar is added and the key area will shrink automatically
    private val topBarHeightDp = 0f

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this)
        keyboardView.ime = this
        return keyboardView
    }

    // Called every time the keyboard appears on screen
    // (onCreateInputView only runs once — this runs every time)
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        // Anchor keyboard window to bottom of screen
        window?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        // Auto-capitalize: start in shift state so first letter is capital
        // After the first character is typed, KeyboardView drops back to lowercase automatically
        keyboardView.shiftState = 1
        keyboardView.invalidate()
    }

    // NEW — detects screen orientation and returns correct keyboard height
    // Called from KeyboardView.kt via ime?.getKeyboardHeight()
    fun getKeyboardHeight(): Int {
        val density = resources.displayMetrics.density

        // NEW — check if phone is currently in landscape mode
        val isLandscape = resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE

        // Portrait: 270dp gives each row comfortable breathing room
        // Landscape: 165dp keeps keyboard usable without blocking too much screen
        // topBarHeightDp is subtracted so key area shrinks when settings bar is added
        val heightDp = (if (isLandscape) 165f else 270f) - topBarHeightDp

        return (heightDp * density).toInt()
    }
}
