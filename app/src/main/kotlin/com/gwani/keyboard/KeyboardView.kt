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

    // Pixel density — used to convert dp to pixels throughout
    private val density = context.resources.displayMetrics.density

    // -------------------------------------------------------
    // ENGINES AND MANAGERS
    // -------------------------------------------------------

    // Detects swipe direction from touch start/end points
    private val gestureEngine = GestureEngine(context)

    // Decides when to show gesture hints to the user
    private val hintManager = GestureHintManager(context)

    // Handles backspace tap vs long press — lives in BackspaceHandler.kt
    private val backspaceHandler = BackspaceHandler {
        ime?.currentInputConnection?.deleteSurroundingText(1, 0)
    }

    // -------------------------------------------------------
    // PAINT OBJECTS — created once, reused every draw
    // -------------------------------------------------------

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2E")
    }

    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Slightly lighter than normal keys for shift, delete, space, enter
        color = Color.parseColor("#3A3A3C")
    }

    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4C4C4E")
    }

    private val shiftActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A84FF") // blue — shift on
    }

    private val capsActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9F0A") // orange — caps lock
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = density * 15f
        textAlign = Paint.Align.CENTER
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = density * 13f
        textAlign = Paint.Align.CENTER
    }

    // Small indicator at top-left of each key showing swipeUp character
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA") // grey — subtle, not distracting
        textSize = density * 9f
        textAlign = Paint.Align.LEFT
    }

    // Hint bubble background — semi-transparent dark
    private val hintBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC1C1C1E")
    }

    // Hint bubble text
    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = density * 13f
        textAlign = Paint.Align.CENTER
    }

    // -------------------------------------------------------
    // STATE
    // -------------------------------------------------------

    private var keyboardHeight = 0
    private val keyRects = mutableListOf<Pair<Key, RectF>>()
    private var pressedKey: Key? = null

    // Shift states: 0=normal  1=shift(one cap)  2=caps lock
    var shiftState = 0

    // Hint state — which key is currently showing its hint bubble
    private var hintKey: Key? = null
    private var hintKeyRect: RectF? = null
    private val hintHandler = Handler(Looper.getMainLooper())

    // Runnable that dismisses the hint after the set duration
    private val dismissHintRunnable = Runnable {
        hintKey = null
        hintKeyRect = null
        invalidate()
    }

    // Long press handler for non-backspace keys (shows hint)
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressTarget: Pair<Key, RectF>? = null

    private val longPressRunnable = Runnable {
        longPressTarget?.let { (key, rect) ->
            if (key.swipeUp.isNotEmpty() || key.output == "space") {
                showHint(key, rect)
            }
        }
    }

    // -------------------------------------------------------
    // MEASURE — tells Android our exact desired height
    // -------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        keyboardHeight = ime?.getKeyboardHeight() ?: (260f * density).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, keyboardHeight)
    }

    // -------------------------------------------------------
    // DRAW
    // -------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#1C1C1E"))

        if (keyRects.isEmpty()) calculateKeyPositions()

        for ((key, rect) in keyRects) {

            // Choose background color for this key
            val brush = when {
                key == pressedKey                        -> pressedPaint
                key.output == "shift" && shiftState == 1 -> shiftActivePaint
                key.output == "shift" && shiftState == 2 -> capsActivePaint
                key.output in setOf(
                    "shift","delete","numbers",
                    "enter","space","emoji"
                )                                        -> specialKeyPaint
                else                                     -> keyPaint
            }

            canvas.drawRoundRect(rect, 10f, 10f, brush)

            // Show uppercase when shift or caps is active
            val displayLabel = when {
                shiftState > 0
                && key.label.length == 1
                && key.label[0].isLetter() -> key.label.uppercase()
                else -> key.label
            }

            // Draw main key label — centered
            val paint = if (key.label.length > 2) smallTextPaint else textPaint
            val textY = rect.centerY() + (paint.textSize / 3f)
            canvas.drawText(displayLabel, rect.centerX(), textY, paint)

            // Draw swipeUp indicator at top-left corner of key
            // Only for keys that have a gesture
            if (key.swipeUp.isNotEmpty()) {
                val indicatorX = rect.left + density * 4f
                val indicatorY = rect.top + density * 11f

                // If caps active, show uppercase version of Hausa chars
                val indicatorLabel = if (shiftState > 0) {
                    key.swipeUp.uppercase()
                } else {
                    key.swipeUp
                }

                canvas.drawText(indicatorLabel, indicatorX, indicatorY, indicatorPaint)
            }
        }

        // Draw hint bubble on top of everything else if active
        hintKey?.let { key ->
            hintKeyRect?.let { rect ->
                drawHintBubble(canvas, key, rect)
            }
        }
    }

    // Draws the floating hint bubble above a key
    private fun drawHintBubble(canvas: Canvas, key: Key, keyRect: RectF) {
        val bubbleText = when {
            key.output == "space" -> "← Flow    Base →"
            key.swipeUp.isNotEmpty() -> {
                val char = if (shiftState > 0) key.swipeUp.uppercase() else key.swipeUp
                "↑  $char"
            }
            else -> return
        }

        val padding = density * 10f
        val bubbleHeight = density * 36f

        // Measure text width to size bubble correctly
        val textWidth = hintTextPaint.measureText(bubbleText)
        val bubbleWidth = textWidth + padding * 2f

        // Position bubble centered above the key
        var bubbleLeft = keyRect.centerX() - bubbleWidth / 2f
        var bubbleTop  = keyRect.top - bubbleHeight - density * 6f

        // Keep bubble inside screen horizontally
        if (bubbleLeft < 0f) bubbleLeft = 0f
        if (bubbleLeft + bubbleWidth > width) bubbleLeft = width - bubbleWidth

        val bubbleRect = RectF(
            bubbleLeft,
            bubbleTop,
            bubbleLeft + bubbleWidth,
            bubbleTop + bubbleHeight
        )

        canvas.drawRoundRect(bubbleRect, density * 8f, density * 8f, hintBgPaint)

        val textY = bubbleRect.centerY() + hintTextPaint.textSize / 3f
        canvas.drawText(bubbleText, bubbleRect.centerX(), textY, hintTextPaint)
    }

    // Shows hint for a key and auto-dismisses after hintDurationMs
    private fun showHint(key: Key, rect: RectF) {
        hintHandler.removeCallbacks(dismissHintRunnable)
        hintKey = key
        hintKeyRect = rect
        invalidate()
        hintHandler.postDelayed(dismissHintRunnable, hintManager.hintDurationMs)
    }

    // -------------------------------------------------------
    // KEY POSITION CALCULATION
    // -------------------------------------------------------

    private fun calculateKeyPositions() {
        keyRects.clear()

        val gap = density * 6f

        // Row weights control relative height of each row
        // 0 = number row (thin)
        // 1,2,3 = letter rows (full height)
        // 4 = bottom row (full height)
        val rowWeights = listOf(0.65f, 1.0f, 1.0f, 1.0f, 1.0f)

        val totalWeight = rowWeights.sum()
        val unitHeight  = keyboardHeight / totalWeight
        var yCursor     = 0f

        for ((rowIndex, row) in BaseLayer.rows.withIndex()) {
            val rowHeight = rowWeights[rowIndex] * unitHeight
            val rowTop    = yCursor + gap / 2f
            val rowBottom = yCursor + rowHeight - gap / 2f

            val totalUnits = row.sumOf { it.width.toDouble() }.toFloat()
            val unitWidth  = width / totalUnits
            var xCursor    = 0f

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

    // -------------------------------------------------------
    // TOUCH HANDLING
    // -------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val key = getKeyAt(event.x, event.y)

        // Backspace has its own dedicated handler
        if (key?.output == "delete") {
            pressedKey = if (event.action == MotionEvent.ACTION_DOWN) key else null
            invalidate()
            return backspaceHandler.onTouch(this, event)
        }

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                pressedKey = key
                gestureEngine.onTouchDown(event.x, event.y)

                // Schedule long press hint after 500ms
                if (key != null) {
                    val rect = getRectForKey(key)
                    if (rect != null && (key.swipeUp.isNotEmpty() || key.output == "space")) {
                        longPressTarget = Pair(key, rect)
                        longPressHandler.postDelayed(longPressRunnable, 500)
                    }
                }

                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                // Cancel any pending long press
                longPressHandler.removeCallbacks(longPressRunnable)
                longPressTarget = null

                val direction = gestureEngine.onTouchUp(event.x, event.y)

                if (key != null) {
                    when (direction) {

                        GestureDirection.NONE -> {
                            // Normal tap
                            handleKeyPress(key)

                            // Show hint automatically for new users
                            if (hintManager.onKeyTapped()) {
                                val rect = getRectForKey(key)
                                if (rect != null &&
                                    (key.swipeUp.isNotEmpty() || key.output == "space")) {
                                    showHint(key, rect)
                                }
                            }
                        }

                        GestureDirection.UP -> {
                            // Swipe up — type the secondary character
                            handleSwipeUp(key)
                        }

                        GestureDirection.LEFT -> {
                            if (key.output == "space") {
                                // TODO: Switch to Flow Layer (Phase 3)
                            }
                        }

                        GestureDirection.RIGHT -> {
                            if (key.output == "space") {
                                // TODO: Switch back to Base Layer (Phase 3)
                            }
                        }

                        GestureDirection.DOWN -> {
                            // Reserved for future use
                        }
                    }
                }

                pressedKey = null
                invalidate()
            }
        }

        return true
    }

    // -------------------------------------------------------
    // KEY LOOKUP HELPERS
    // -------------------------------------------------------

    private fun getKeyAt(x: Float, y: Float): Key? {
        for ((key, rect) in keyRects) {
            if (rect.contains(x, y)) return key
        }
        return null
    }

    private fun getRectForKey(target: Key): RectF? {
        for ((key, rect) in keyRects) {
            if (key === target) return rect
        }
        return null
    }

    // -------------------------------------------------------
    // KEY PRESS HANDLERS
    // -------------------------------------------------------

    // Normal tap handler
    private fun handleKeyPress(key: Key) {
        val ic = ime?.currentInputConnection ?: return

        when (key.output) {

            "enter" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_ENTER))
            }

            "space" -> ic.commitText(" ", 1)

            "shift" -> {
                shiftState = when (shiftState) {
                    0    -> 1
                    1    -> 2
                    else -> 0
                }
                invalidate()
            }

            "numbers" -> { /* Phase 3 — layer switching */ }
            "emoji"   -> { /* Future */ }

            else -> {
                val output = if (shiftState > 0) key.output.uppercase() else key.output
                ic.commitText(output, 1)
                if (shiftState == 1) {
                    shiftState = 0
                    invalidate()
                }
            }
        }
    }

    // Swipe-up handler — types the secondary character
    private fun handleSwipeUp(key: Key) {
        if (key.swipeUp.isEmpty()) return
        val ic = ime?.currentInputConnection ?: return

        // Hausa characters respect shift state
        // Symbols stay as-is (@ is always @, # is always #)
        val output = when {
            key.swipeUp.length == 1 && key.swipeUp[0].isLetter() -> {
                // It's a Hausa character — apply shift state
                if (shiftState > 0) key.swipeUp.uppercase() else key.swipeUp
            }
            else -> key.swipeUp // symbol — no case change
        }

        ic.commitText(output, 1)

        // Shift (state 1) turns off after one character
        if (shiftState == 1) {
            shiftState = 0
            invalidate()
        }
    }

    // -------------------------------------------------------
    // SIZE CHANGED — recalculate on rotation or resize
    // -------------------------------------------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        keyRects.clear()
        invalidate()
    }
}
