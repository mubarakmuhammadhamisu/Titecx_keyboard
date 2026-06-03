package com.gwani.keyboard

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.WindowManager

class GwaniIME : InputMethodService() {

    private lateinit var keyboardView: KeyboardView

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this)
        keyboardView.ime = this
        return keyboardView
    }

    // NEW — called every time the keyboard appears on screen
    // (onCreateInputView only runs once ever — this runs every time)
    // We use this to fix the window position on Android 14
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        // NEW — forces the keyboard window to anchor to the bottom of the screen
        // Without this Android 14 sometimes floats it in the wrong position
        // when the input field is in the middle of the screen
        window?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    // NEW — detects screen orientation and returns correct keyboard height
    // Called from KeyboardView.kt via ime?.getKeyboardHeight()
    fun getKeyboardHeight(): Int {
        val density = resources.displayMetrics.density

        // NEW — check if phone is currently in landscape mode
        val isLandscape = resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE

        // NEW — landscape uses 160dp (shorter) portrait uses 260dp (normal)
        // Landscape screen height is much smaller so we reduce keyboard height
        val heightDp = if (isLandscape) 170f else 260f

        return (heightDp * density).toInt()
    }
}
