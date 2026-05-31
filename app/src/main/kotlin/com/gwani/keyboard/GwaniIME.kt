package com.gwani.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.FrameLayout

// -----------------------------------------------------------
// GWANI IME
// IME = Input Method Editor. This is the official Android term
// for a keyboard app.
//
// This is the entry point of the entire keyboard.
// Android calls this class when our keyboard needs to appear.
//
// InputMethodService is Android's built-in base class for keyboards.
// We extend it (inherit from it) so we get all the keyboard
// connection behavior for free, and we only write our own UI.
// -----------------------------------------------------------

class GwaniIME : InputMethodService() {

    // This is where we store our keyboard view so we can reference it later
    private lateinit var keyboardView: KeyboardView

    // -----------------------------------------------------------
    // onCreateInputView
    // Android calls this method when it needs to show our keyboard.
    // Whatever View we return here becomes the keyboard on screen.
    // This is called once and the view is reused after that.
    // -----------------------------------------------------------

    override fun onCreateInputView(): View {

        // Create our custom keyboard view
        keyboardView = KeyboardView(this)

        // Give the view a reference back to this service.
        // The view needs this to call currentInputConnection
        // which is how we actually type characters into apps.
        keyboardView.ime = this

        // -----------------------------------------------------------
        // FIX KEYBOARD HEIGHT
        // Without this, Android stretches the keyboard to full screen.
        // We calculate 40% of the screen height — same as Gboard/SwiftKey.
        //
        // displayMetrics = information about the screen (size, density)
        // heightPixels   = full screen height in pixels
        // * 0.40         = 40% of that
        // .toInt()       = convert to a whole number (no decimals)
        // -----------------------------------------------------------
        val screenHeight = resources.displayMetrics.heightPixels
        val keyboardHeight = (screenHeight * 0.40).toInt()

        // FrameLayout.LayoutParams sets the width and height of our view.
        // MATCH_PARENT for width = stretch full screen width (correct)
        // keyboardHeight for height = exactly 40% of screen (correct)
        keyboardView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            keyboardHeight
        )

        // Return the view. Android will display it as the keyboard.
        return keyboardView
    }
}
