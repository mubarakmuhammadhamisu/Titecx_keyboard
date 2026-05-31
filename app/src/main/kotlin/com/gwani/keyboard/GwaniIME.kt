package com.gwani.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View

class GwaniIME : InputMethodService() {

    private lateinit var keyboardView: KeyboardView

    // -----------------------------------------------------------
    // onCreateInputView
    // Android calls this when it needs to show our keyboard.
    // InputMethodService automatically docks the returned view
    // to the BOTTOM of the screen.
    // The height of the window = whatever height our view reports
    // through onMeasure — so we let KeyboardView handle its own height.
    // -----------------------------------------------------------
    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this)
        keyboardView.ime = this
        return keyboardView
    }
}
