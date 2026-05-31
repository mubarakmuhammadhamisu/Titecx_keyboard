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

// -----------------------------------------------------------
// KEYBOARD VIEW
// Draws keys on screen and handles all touch interactions.
// -----------------------------------------------------------

class KeyboardView(context: Context) : View(context) {

    var ime: GwaniIME? = null

    // -----------------------------------------------------------
    // PAINT OBJECTS — created once, reused every draw
    // -----------------------------------------------------------

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2E")
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4C4C4E")
    }

    // Shift active = highlighted color so user knows shift is ON
    private val shiftActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A84FF")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        textAlign = Paint.Align.CENTER
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    // -----------------------------------------------------------
    // STATE
    // -----------------------------------------------------------

    private val keyRects = mutableListOf<Pair<Key, RectF>>()
    private var pressedKey: Key? = null

    // Tracks whether shift is currently ON
    // When true, next letter typed will be uppercase, then shift turns off
    private var isShifted = false

    // -----------------------------------------------------------
    // LONG PRESS BACKSPACE
    // Handler runs on the main UI thread.
    // When backspace is held, we post a repeating delete action
    // every 50ms — same feel as Gboard continuous delete.
    // -----------------------------------------------------------

    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressing = false

    // This Runnable deletes one character then reschedules itself every 50ms
    private val deleteRunnable = object : Runnable {
        override fun run() {
            ime?.currentInputConnection?.deleteSurroundingText(1, 0)
            handler.postDelayed(this, 50)
        }
    }

    // -----------------------------------------------------------
    // onMeasure
    // Reports our desired height to Android so the IME window
    // wraps tightly around our keys and docks to bottom of screen.
    // 5 rows × 52dp = 260dp total height.
    // -----------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredHeight = (260f * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, desiredHeight)
    }

    // -----------------------------------------------------------
    // DRAWING
    // -----------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1C1C1E"))

        if (keyRects.isEmpty()) calculateKeyPositions()

        for ((key, rect) in keyRects) {

            // Choose which background brush to use for this key
            val brush = when {
                key == pressedKey              -> pressedPaint
                key.output == "shift" && isShifted -> shiftActivePaint
                else                           -> keyPaint
            }

            canvas.drawRoundRect(rect, 12f, 12f, brush)

            // When shift is ON, show letter keys as uppercase
            val displayLabel = when {
                isShifted && key.label.length == 1 && key.label[0].isLetter() ->
                    key.label.uppercase()
                else -> key.label
            }

            // Use smaller text for keys with longer labels like "?123"
            val paint = if (key.label.length > 2) smallTextPaint else textPaint

            val textY = rect.centerY() + (paint.textSize / 3f)
            canvas.drawText(displayLabel, rect.centerX(), textY, paint)
        }
    }

    // -----------------------------------------------------------
    // CALCULATE KEY POSITIONS
    // Runs once when keyboard size is known, or after resize.
    // -----------------------------------------------------------

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

    // -----------------------------------------------------------
    // TOUCH HANDLING
    // ACTION_DOWN = finger touched screen
    // ACTION_UP   = finger lifted
    // -----------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                val key = getKeyAt(event.x, event.y)
                pressedKey = key
                isLongPressing = false

                // If backspace is held down, start the repeating delete
                // after 400ms initial delay (same feel as standard keyboards)
                if (key?.output == "delete") {
                    handler.postDelayed({
                        isLongPressing = true
                        handler.post(deleteRunnable)
                    }, 400)
                }

                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                // Always cancel any running long press delete
                handler.removeCallbacks(deleteRunnable)

                val key = getKeyAt(event.x, event.y)

                // Only trigger a normal tap if we were NOT long pressing
                // (long press already handled the deletes via the handler)
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

    // -----------------------------------------------------------
    // GET KEY AT POSITION
    // -----------------------------------------------------------

    private fun getKeyAt(x: Float, y: Float): Key? {
        for ((key, rect) in keyRects) {
            if (rect.contains(x, y)) return key
        }
        return null
    }

    // -----------------------------------------------------------
    // HANDLE KEY PRESS
    // -----------------------------------------------------------

    private fun handleKeyPress(key: Key) {
        val ic = ime?.currentInputConnection ?: return

        when (key.output) {

            "delete" -> ic.deleteSurroundingText(1, 0)

            "enter" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }

            "space" -> ic.commitText(" ", 1)

            // Toggle shift ON/OFF and redraw so key colors update
            "shift" -> {
                isShifted = !isShifted
                invalidate()
            }

            // Number layer — coming in next phase
            "numbers" -> { /* Phase 2 */ }

            // All letter and symbol keys
            else -> {
                // If shift is ON, type uppercase. Then turn shift OFF.
                val output = if (isShifted) key.output.uppercase() else key.output
                ic.commitText(output, 1)

                if (isShifted) {
                    isShifted = false
                    invalidate()
                }
            }
        }
    }

    // -----------------------------------------------------------
    // SIZE CHANGED — recalculate key positions for new dimensions
    // -----------------------------------------------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        keyRects.clear()
        invalidate()
    }
}
l
