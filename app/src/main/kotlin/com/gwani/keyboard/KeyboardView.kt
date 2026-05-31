package com.gwani.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

// -----------------------------------------------------------
// KEYBOARD VIEW
// This file does two things only:
//   1. Draw the keys on screen
//   2. Respond to finger touches
// -----------------------------------------------------------

class KeyboardView(context: Context) : View(context) {

    // The IME service. We need this to actually type characters into apps.
    // It is set after this view is created (see GwaniIME.kt)
    var ime: GwaniIME? = null

    // -----------------------------------------------------------
    // PAINT OBJECTS
    // Paint = a brush. Each brush has its own color and settings.
    // We create them once here so we don't recreate them on every draw.
    // -----------------------------------------------------------

    // Brush for drawing normal key backgrounds
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2E")
    }

    // Brush for drawing a key that is currently being pressed
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4C4C4E")
    }

    // Brush for writing text on keys
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        textAlign = Paint.Align.CENTER
    }

    // -----------------------------------------------------------
    // STATE
    // -----------------------------------------------------------

    // Stores the rectangle (position + size) for every key.
    // We calculate this once when the keyboard size is known.
    private val keyRects = mutableListOf<Pair<Key, RectF>>()

    // The key the finger is currently pressing (null if no finger on screen)
    private var pressedKey: Key? = null

    // -----------------------------------------------------------
    // DRAWING
    // Android calls onDraw() whenever the keyboard needs to be redrawn.
    // -----------------------------------------------------------

    override fun onDraw(canvas: Canvas) {

        // Fill the entire keyboard background with dark color
        canvas.drawColor(Color.parseColor("#1C1C1E"))

        // If key positions haven't been calculated yet, calculate them now
        if (keyRects.isEmpty()) {
            calculateKeyPositions()
        }

        // Loop through every key and draw it
        for ((key, rect) in keyRects) {

            // Use pressed brush if this key is being touched, normal brush otherwise
            val brush = if (key == pressedKey) pressedPaint else keyPaint

            // Draw the key background as a rounded rectangle
            // 12f, 12f = corner radius (how rounded the corners are)
            canvas.drawRoundRect(rect, 12f, 12f, brush)

            // Draw the key label text centered in the key
            // textSize / 3 adjusts for how Android measures text height
            val textY = rect.centerY() + (textPaint.textSize / 3f)
            canvas.drawText(key.label, rect.centerX(), textY, textPaint)
        }
    }

    // -----------------------------------------------------------
    // CALCULATE KEY POSITIONS
    // This figures out where every key sits on screen.
    // It runs once after we know the keyboard width and height.
    // -----------------------------------------------------------

    private fun calculateKeyPositions() {
        keyRects.clear()

        val gap = 8f  // space between keys in pixels

        // -----------------------------------------------------------
        // KEY ROW HEIGHT
        // Instead of dividing the full view height (which causes oversized keys),
        // we use a fixed height per row based on screen density.
        //
        // density = pixels per dp on this screen (varies per phone)
        // 52f     = 52dp per row — standard keyboard row height like Gboard
        // Multiplying dp × density converts dp to real pixels for this screen.
        // -----------------------------------------------------------
        val density = resources.displayMetrics.density
        val rowHeight = 52f * density

        for ((rowIndex, row) in BaseLayer.rows.withIndex()) {

            // Y position = which row × fixed row height
            val rowTop    = rowIndex * rowHeight + gap / 2f
            val rowBottom = rowTop + rowHeight - gap

            // Add up all width units in this row
            val totalUnits = row.sumOf { it.width.toDouble() }.toFloat()

            // One unit of width in pixels
            val unitWidth = width / totalUnits

            var xCursor = 0f

            for (key in row) {
                val keyWidth = key.width * unitWidth

                val left  = xCursor + gap / 2f
                val right = xCursor + keyWidth - gap / 2f

                keyRects.add(Pair(key, RectF(left, rowTop, right, rowBottom)))

                xCursor += keyWidth
            }
        }
    }

    // -----------------------------------------------------------
    // TOUCH HANDLING
    // Android calls onTouchEvent() every time the finger moves.
    // ACTION_DOWN = finger just touched screen
    // ACTION_UP   = finger lifted off screen
    // -----------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                // Find which key was touched and mark it as pressed
                pressedKey = getKeyAt(event.x, event.y)
                invalidate() // tell Android to redraw so pressed color shows
            }

            MotionEvent.ACTION_UP -> {
                // Find which key the finger lifted from and trigger it
                val key = getKeyAt(event.x, event.y)
                if (key != null) handleKeyPress(key)

                pressedKey = null
                invalidate() // redraw to remove pressed color
            }
        }
        return true // true = we handled the event
    }

    // -----------------------------------------------------------
    // GET KEY AT POSITION
    // Given an x,y coordinate, find which key is there.
    // Returns null if no key found at that position.
    // -----------------------------------------------------------

    private fun getKeyAt(x: Float, y: Float): Key? {
        for ((key, rect) in keyRects) {
            if (rect.contains(x, y)) return key
        }
        return null
    }

    // -----------------------------------------------------------
    // HANDLE KEY PRESS
    // Called when a key is tapped. Decides what to do.
    // currentInputConnection = the active text field in any app.
    // -----------------------------------------------------------

    private fun handleKeyPress(key: Key) {
        val ic = ime?.currentInputConnection ?: return

        when (key.output) {

            // Delete one character behind the cursor
            "delete" -> ic.deleteSurroundingText(1, 0)

            // Send an Enter / newline
            "enter" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }

            // Type a space
            "space" -> ic.commitText(" ", 1)

            // Shift and switch — handled in future phases
            "shift"  -> { /* Phase 2 */ }
            "switch" -> { /* Phase 2 */ }

            // Every other key — just type its output character
            else -> ic.commitText(key.output, 1)
        }
    }

    // -----------------------------------------------------------
    // SIZE CHANGED
    // Called when the keyboard's width or height is finalized.
    // We clear keyRects so positions get recalculated for the new size.
    // -----------------------------------------------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        keyRects.clear()
        invalidate()
    }
}
    // -----------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                // Find which key was touched and mark it as pressed
                pressedKey = getKeyAt(event.x, event.y)
                invalidate() // tell Android to redraw so pressed color shows
            }

            MotionEvent.ACTION_UP -> {
                // Find which key the finger lifted from and trigger it
                val key = getKeyAt(event.x, event.y)
                if (key != null) handleKeyPress(key)

                pressedKey = null
                invalidate() // redraw to remove pressed color
            }
        }
        return true // true = we handled the event
    }

    // -----------------------------------------------------------
    // GET KEY AT POSITION
    // Given an x,y coordinate, find which key is there.
    // Returns null if no key found at that position.
    // -----------------------------------------------------------

    private fun getKeyAt(x: Float, y: Float): Key? {
        for ((key, rect) in keyRects) {
            if (rect.contains(x, y)) return key
        }
        return null
    }

    // -----------------------------------------------------------
    // HANDLE KEY PRESS
    // Called when a key is tapped. Decides what to do.
    // currentInputConnection = the active text field in any app.
    // -----------------------------------------------------------

    private fun handleKeyPress(key: Key) {
        val ic = ime?.currentInputConnection ?: return

        when (key.output) {

            // Delete one character behind the cursor
            "delete" -> ic.deleteSurroundingText(1, 0)

            // Send an Enter / newline
            "enter" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }

            // Type a space
            "space" -> ic.commitText(" ", 1)

            // Shift and switch — handled in future phases
            "shift"  -> { /* Phase 2 */ }
            "switch" -> { /* Phase 2 */ }

            // Every other key — just type its output character
            else -> ic.commitText(key.output, 1)
        }
    }

    // -----------------------------------------------------------
    // SIZE CHANGED
    // Called when the keyboard's width or height is finalized.
    // We clear keyRects so positions get recalculated for the new size.
    // -----------------------------------------------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        keyRects.clear()
        invalidate()
    }
}
