package com.gwani.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

class KeyboardView(context: Context) : View(context) {

    var ime: GwaniIME? = null

    // Paint objects — created once, reused every draw call
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#252538")
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A4A6A")
    }

    // Special keys: shift, delete, space, enter, ?123
    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E2E45")
    }

    // Blue — shift active (one capital coming)
    private val shiftActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A84FF")
    }

    // Orange — caps lock active (all capitals locked)
    private val capsActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9F0A")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = resources.displayMetrics.density * 13f
        textAlign = Paint.Align.CENTER
    }

    // Smaller text for keys with longer labels like ?123
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = resources.displayMetrics.density * 11f
        textAlign = Paint.Align.CENTER
    }

    // Stores current keyboard height in pixels
    // Set in onMeasure, read in calculateKeyPositions
    // Both functions stay in sync this way
    private var keyboardHeight = 0

    // List of every key paired with its rectangle (position + size)
    private val keyRects = mutableListOf<Pair<Key, RectF>>()

    // The key currently being pressed by the finger
    private var pressedKey: Key? = null

    // Shift has three states:
    // 0 = normal    — lowercase
    // 1 = shift     — next letter typed is capital, then back to normal
    // 2 = caps lock — all letters stay capital until shift tapped again
    private var shiftState = 0

    // BackspaceHandler lives in its own file BackspaceHandler.kt
    // We pass it a lambda that deletes one character
    // BackspaceHandler handles all tap vs long press logic internally
    private val backspaceHandler = BackspaceHandler {
        ime?.currentInputConnection?.deleteSurroundingText(1, 0)
    }

    // onMeasure tells Android exactly how tall we want to be
    // 5 rows x 52dp = 260dp — the IME window wraps this and docks to bottom
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        keyboardHeight = ime?.getKeyboardHeight()
            ?: (260f * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, keyboardHeight)
    }

    // onDraw is called by Android whenever the keyboard needs to be redrawn
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        if (keyRects.isEmpty()) calculateKeyPositions()

        for ((key, rect) in keyRects) {

            // Pick the correct background color for this key
            val brush = when {
                key == pressedKey                                                    -> pressedPaint
                key.output == "shift" && shiftState == 1                             -> shiftActivePaint
                key.output == "shift" && shiftState == 2                             -> capsActivePaint
                key.output in setOf("shift", "delete", "numbers", "enter", "space") -> specialKeyPaint
                else                                                                 -> keyPaint
            }

            val cornerRadius = resources.displayMetrics.density * 5f
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, brush)

            // Show uppercase labels when shift or caps lock is ON
            val displayLabel = when {
                shiftState > 0 && key.label.length == 1 && key.label[0].isLetter() ->
                    key.label.uppercase()
                else -> key.label
            }

            // Keys with labels longer than 2 chars use smaller text
            val paint = if (key.label.length > 2) smallTextPaint else textPaint
            val textY = rect.centerY() + (paint.textSize / 3f)
            canvas.drawText(displayLabel, rect.centerX(), textY, paint)
        }
    }

    // Calculates the pixel position and size of every key
    // Runs once when keyboard dimensions are first known
    private fun calculateKeyPositions() {
        keyRects.clear()

        // Gap between keys, scaled to screen density so it looks the same on all screens
        val gap = resources.displayMetrics.density * 6f

        // Row 1 (QWERTY) is the reference width — 10 keys of equal width = 10 units
        // Row 2 (ASDFGHJKL) has 9 keys = 9 units, so we indent it by 0.5 units each side
        // This matches the centered appearance of Gboard and SwiftKey
        val referenceRowUnits = BaseLayer.rows[1].sumOf { it.width.toDouble() }.toFloat()

        // Each row has a weight that controls its height relative to others.
        // 0.7 = thin row (numbers and symbols)
        // 1.0 = normal row (letters and bottom row)
        // Row order matches BaseLayer.rows exactly:
        // 0=numbers, 1=qwerty, 2=asdfg, 3=shift/zxcv, 4=symbols, 5=bottom
        val rowWeights = listOf(0.7f, 1.0f, 1.0f, 1.0f, 1.0f)

        // Add up all weights to know the total
        val totalWeight = rowWeights.sum()

        // One unit of height in pixels
        // All row heights are calculated from this single unit
        val unitHeight = keyboardHeight / totalWeight

        // Track vertical position as we place rows top to bottom
        var yCursor = 0f

        for ((rowIndex, row) in BaseLayer.rows.withIndex()) {

            // This row's height = its weight × one unit of height
            val rowHeight = rowWeights[rowIndex] * unitHeight

            val rowTop    = yCursor + gap / 2f
            val rowBottom = yCursor + rowHeight - gap / 2f

            val totalUnits = row.sumOf { it.width.toDouble() }.toFloat()
            val unitWidth  = width / referenceRowUnits

            // Center rows that are narrower than the reference row (QWERTY)
            // ASDFGHJKL has 9 units vs 10 — offset = 0.5 * unitWidth each side
            val rowIndent = ((referenceRowUnits - totalUnits) / 2f) * unitWidth

            var xCursor = rowIndent

            for (key in row) {
                val keyWidth = key.width * unitWidth
                val left  = xCursor + gap / 2f
                val right = xCursor + keyWidth - gap / 2f
                keyRects.add(Pair(key, RectF(left, rowTop, right, rowBottom)))
                xCursor += keyWidth
            }

            yCursor += rowHeight
        }
    }

    // onTouchEvent is called on every finger movement
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val key = getKeyAt(event.x, event.y)

        // Backspace is handled entirely by BackspaceHandler
        // We pass the event directly to it and let it manage
        // tap vs long press logic internally
        if (key?.output == "delete") {
            pressedKey = if (event.action == MotionEvent.ACTION_DOWN) key else null
            invalidate()
            return backspaceHandler.onTouch(this, event)
        }

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                pressedKey = key
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (key != null) handleKeyPress(key)
                pressedKey = null
                invalidate()
            }
        }
        return true
    }

    // Returns whichever key the finger is currently over
    private fun getKeyAt(x: Float, y: Float): Key? {
        for ((key, rect) in keyRects) {
            if (rect.contains(x, y)) return key
        }
        return null
    }

    // Decides what to do when a key is tapped
    private fun handleKeyPress(key: Key) {
        val ic = ime?.currentInputConnection ?: return

        when (key.output) {

            "enter" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }

            "space" -> ic.commitText(" ", 1)

            "shift" -> {
                // Cycle: normal(0) → shift(1) → caps lock(2) → normal(0)
                shiftState = when (shiftState) {
                    0    -> 1
                    1    -> 2
                    else -> 0
                }
                invalidate()
            }

            // Number layer wired up in next phase
            "numbers" -> { }

            else -> {
                val output = if (shiftState > 0) key.output.uppercase() else key.output
                ic.commitText(output, 1)
                // After typing one letter in shift mode, go back to normal
                // Caps lock (state 2) stays on until shift is tapped
                if (shiftState == 1) {
                    shiftState = 0
                    invalidate()
                }
            }
        }
    }

    // Called when keyboard dimensions change — forces key positions to recalculate
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        keyRects.clear()
        invalidate()
    }
}
