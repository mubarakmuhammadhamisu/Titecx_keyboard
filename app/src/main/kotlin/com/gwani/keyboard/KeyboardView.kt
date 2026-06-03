package com.gwani.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

class KeyboardView(context: Context) : View(context) {

    var ime: GwaniIME? = null

    // Paint objects — created once, reused every draw call
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2E")
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4C4C4E")
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
        textSize = 44f
        textAlign = Paint.Align.CENTER
    }

    // Smaller text for keys with longer labels like ?123
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    // List of every key paired with its rectangle (position + size)
    private val keyRects = mutableListOf<Pair<Key, RectF>>()

    // The key currently being pressed by the finger
    private var pressedKey: Key? = null

    // Shift has three states:
    // 0 = normal    — lowercase
    // 1 = shift     — next letter typed is capital, then back to normal
    // 2 = caps lock — all letters stay capital until shift tapped again
    private var shiftState = 0

    // Handler runs on main UI thread — used for backspace long press
    private val handler = Handler(Looper.getMainLooper())

    // Tracks if we are currently in a long press state
    private var isLongPressing = false

    // Runnable that deletes one character and reschedules itself every 50ms
    // This creates the continuous delete effect while backspace is held
    private val deleteRunnable = object : Runnable {
        override fun run() {
            ime?.currentInputConnection?.deleteSurroundingText(1, 0)
            handler.postDelayed(this, 50)
        }
    }

    // onMeasure tells Android exactly how tall we want to be
    // 5 rows x 52dp = 260dp — the IME window wraps this and docks to bottom
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        // Ask GwaniIME for correct height based on current orientation
        // Falls back to 260dp if ime is not yet connected
        val desiredHeight = ime?.getKeyboardHeight()
            ?: (260f * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, desiredHeight)
    }

    // onDraw is called by Android whenever the keyboard needs to be redrawn
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1C1C1E"))

        if (keyRects.isEmpty()) calculateKeyPositions()

        for ((key, rect) in keyRects) {

            // Pick the correct background color for this key
            val brush = when {
                key == pressedKey                     -> pressedPaint
                key.output == "shift" && shiftState == 1 -> shiftActivePaint
                key.output == "shift" && shiftState == 2 -> capsActivePaint
                else                                  -> keyPaint
            }

            canvas.drawRoundRect(rect, 12f, 12f, brush)

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

        val gap = 8f
        val density = resources.displayMetrics.density
        val rowHeight = 52f * density

        for ((rowIndex, row) in BaseLayer.rows.withIndex()) {
            val rowTop    = rowIndex * rowHeight + gap / 2f
            val rowBottom = rowTop + rowHeight - gap

            val totalUnits = row.sumOf { it.width.toDouble() }.toFloat()
            val unitWidth  = width / totalUnits

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

    // onTouchEvent is called on every finger movement
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                val key = getKeyAt(event.x, event.y)
                pressedKey = key
                isLongPressing = false

                // Start long press delete after 400ms if backspace is held
                if (key?.output == "delete") {
                    handler.postDelayed({
                        isLongPressing = true
                        handler.post(deleteRunnable)
                    }, 400)
                }

                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                // Cancel any ongoing long press delete immediately
                handler.removeCallbacks(deleteRunnable)

                val key = getKeyAt(event.x, event.y)

                // Only fire normal tap if this was not a long press
                if (key != null && !isLongPressing) {
                    handleKeyPress(key)
                }

                isLongPressing = false
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

            "delete" -> ic.deleteSurroundingText(1, 0)

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
