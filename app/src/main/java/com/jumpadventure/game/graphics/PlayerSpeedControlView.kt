package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jumpadventure.game.graphics.CharacterRenderer
import com.jumpadventure.game.model.GameSaveData
import java.util.Locale

class PlayerSpeedControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var speedMultiplier: Float = 1.15f
        set(value) {
            val clamped = value.coerceIn(0.75f, 1.50f)
            if (field != clamped) {
                field = clamped
                invalidate()
                onSpeedChangedListener?.invoke(clamped)
            }
        }

    var selectedCharacterId: String = "DEFAULT"
        set(value) {
            field = value
            invalidate()
        }

    var onSpeedChangedListener: ((Float) -> Unit)? = null

    private val density = resources.displayMetrics.density

    // Animation state for live preview
    private var animTime = 0f
    private var lastFrameTimeNanos = SystemClock.elapsedRealtimeNanos()

    // Rectangles for touch & drawing
    private val previewRect = RectF()
    private val sliderTrackRect = RectF()
    private val thumbRect = RectF()

    private val btnSlowRect = RectF()
    private val btnNormalRect = RectF()
    private val btnFastRect = RectF()

    private var isDraggingThumb = false

    // Paints
    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        style = Paint.Style.FILL
    }
    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.FILL
    }
    private val trackFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD43B")
        style = Paint.Style.FILL
    }
    private val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    private val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 15f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 12f * density
        typeface = Typeface.DEFAULT_BOLD
    }
    private val multiplierTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD43B")
        textSize = 18f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = (280 * density).toInt()
        val minHeight = (280 * density).toInt()
        val width = resolveSize(minWidth, widthMeasureSpec)
        val height = resolveSize(minHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // Delta time for running animation in preview
        val now = SystemClock.elapsedRealtimeNanos()
        val dt = ((now - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        lastFrameTimeNanos = now

        animTime += dt * speedMultiplier * 1.2f

        val pad = 12f * density

        // 1. Overall Background Card
        val cardRect = RectF(pad, pad, w - pad, h - pad)
        canvas.drawRoundRect(cardRect, 16f * density, 16f * density, cardBgPaint)
        canvas.drawRoundRect(cardRect, 16f * density, 16f * density, cardBorderPaint)

        var currentY = cardRect.top + 16f * density

        // Header: "PLAYER SPEED"
        canvas.drawText("PLAYER SPEED", cardRect.centerX(), currentY + 14f * density, titleTextPaint)
        currentY += 28f * density

        // 2. Live Character Preview Box
        val previewH = 90f * density
        val previewW = cardRect.width() - 24f * density
        previewRect.set(cardRect.centerX() - previewW / 2f, currentY, cardRect.centerX() + previewW / 2f, currentY + previewH)

        val previewBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(previewRect, 12f * density, 12f * density, previewBgPaint)
        canvas.drawRoundRect(previewRect, 12f * density, 12f * density, cardBorderPaint)

        // Draw ground platform inside preview
        val groundY = previewRect.bottom - 12f * density
        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.FILL
        }
        canvas.drawRect(previewRect.left + 8f * density, groundY, previewRect.right - 8f * density, groundY + 4f * density, groundPaint)

        // Draw running character in center of preview
        val charW = 48f * density
        val charH = 64f * density
        val charX = previewRect.centerX() - charW / 2f
        val charY = groundY - charH
        val charBounds = RectF(charX, charY, charX + charW, charY + charH)

        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = charBounds,
            characterId = selectedCharacterId,
            facingRight = true,
            animState = CharacterRenderer.AnimState.RUN,
            animTime = animTime
        )

        currentY = previewRect.bottom + 16f * density

        // 3. Slider Track
        val trackH = 10f * density
        val trackPadding = 24f * density
        sliderTrackRect.set(cardRect.left + trackPadding, currentY + 12f * density, cardRect.right - trackPadding, currentY + 12f * density + trackH)

        // SLOW & FAST Labels
        labelTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("SLOW", sliderTrackRect.left, currentY + 4f * density, labelTextPaint)
        labelTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("FAST", sliderTrackRect.right, currentY + 4f * density, labelTextPaint)

        currentY += 18f * density

        // Track Background
        canvas.drawRoundRect(sliderTrackRect, trackH / 2f, trackH / 2f, trackPaint)

        // Track Active Fill
        val fraction = (speedMultiplier - 0.75f) / (1.50f - 0.75f)
        val thumbX = sliderTrackRect.left + fraction * sliderTrackRect.width()
        val fillRect = RectF(sliderTrackRect.left, sliderTrackRect.top, thumbX, sliderTrackRect.bottom)
        canvas.drawRoundRect(fillRect, trackH / 2f, trackH / 2f, trackFillPaint)

        // Thumb
        val thumbRadius = 13f * density
        thumbRect.set(thumbX - thumbRadius, sliderTrackRect.centerY() - thumbRadius, thumbX + thumbRadius, sliderTrackRect.centerY() + thumbRadius)
        canvas.drawCircle(thumbX, sliderTrackRect.centerY(), thumbRadius, thumbPaint)
        canvas.drawCircle(thumbX, sliderTrackRect.centerY(), thumbRadius, thumbBorderPaint)

        currentY = sliderTrackRect.bottom + 16f * density

        // 4. Multiplier Display ("1.15×")
        val speedStr = String.format(Locale.US, "%.2f×", speedMultiplier)
        canvas.drawText(speedStr, cardRect.centerX(), currentY + 14f * density, multiplierTextPaint)

        currentY += 28f * density

        // 5. Speed Presets Buttons (SLOW=0.85x [blue], NORMAL=1.15x [green], FAST=1.40x [orange])
        val btnMargin = 6f * density
        val totalBtnW = cardRect.width() - 24f * density
        val btnW = (totalBtnW - btnMargin * 2f) / 3f
        val btnH = 36f * density

        var btnX = cardRect.left + 12f * density
        btnSlowRect.set(btnX, currentY, btnX + btnW, currentY + btnH)
        btnX += btnW + btnMargin
        btnNormalRect.set(btnX, currentY, btnX + btnW, currentY + btnH)
        btnX += btnW + btnMargin
        btnFastRect.set(btnX, currentY, btnX + btnW, currentY + btnH)

        drawPresetButton(canvas, btnSlowRect, "SLOW", Color.parseColor("#0284C7"), Math.abs(speedMultiplier - 0.85f) < 0.03f)
        drawPresetButton(canvas, btnNormalRect, "NORMAL", Color.parseColor("#16A34A"), Math.abs(speedMultiplier - 1.15f) < 0.03f)
        drawPresetButton(canvas, btnFastRect, "FAST", Color.parseColor("#EA580C"), Math.abs(speedMultiplier - 1.40f) < 0.03f)

        // Keep loop running for live preview animation
        postInvalidateOnAnimation()
    }

    private fun drawPresetButton(canvas: Canvas, rect: RectF, label: String, baseColor: Int, isSelected: Boolean) {
        val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseColor
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isSelected) Color.WHITE else Color.parseColor("#64748B")
            style = Paint.Style.STROKE
            strokeWidth = if (isSelected) 3f * density else 1.5f * density
        }
        canvas.drawRoundRect(rect, 8f * density, 8f * density, btnPaint)
        canvas.drawRoundRect(rect, 8f * density, 8f * density, border)

        val textY = rect.centerY() + btnTextPaint.textSize * 0.35f
        canvas.drawText(label, rect.centerX(), textY, btnTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (btnSlowRect.contains(x, y)) {
                    speedMultiplier = 0.85f
                    return true
                }
                if (btnNormalRect.contains(x, y)) {
                    speedMultiplier = 1.15f
                    return true
                }
                if (btnFastRect.contains(x, y)) {
                    speedMultiplier = 1.40f
                    return true
                }

                val expandedTrack = RectF(sliderTrackRect.left, sliderTrackRect.top - 20f * density, sliderTrackRect.right, sliderTrackRect.bottom + 20f * density)
                if (expandedTrack.contains(x, y) || thumbRect.contains(x, y)) {
                    isDraggingThumb = true
                    updateSpeedFromX(x)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingThumb) {
                    updateSpeedFromX(x)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingThumb = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateSpeedFromX(touchX: Float) {
        val left = sliderTrackRect.left
        val right = sliderTrackRect.right
        val width = right - left
        if (width <= 0) return
        val frac = ((touchX - left) / width).coerceIn(0f, 1f)
        val value = 0.75f + frac * (1.50f - 0.75f)
        // Round to nearest 0.01
        speedMultiplier = Math.round(value * 100f) / 100f
    }
}
