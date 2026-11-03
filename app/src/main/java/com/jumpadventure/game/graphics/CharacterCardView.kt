package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Renders a single character portrait inside a shop / heroes grid cell.
 *
 * Draws through [CharacterRenderer] so the portrait always matches the in-game
 * sprite for the same `characterId`. The idle animation is driven by a
 * self-rescheduling frame callback that is cancelled on detach, so repeatedly
 * opening the Heroes / Shop screens cannot leak runnables.
 */
class CharacterCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var characterId: String = "DEFAULT"
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var skinId: String = "DEFAULT"
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** When true the portrait is desaturated and a lock glyph is drawn over it. */
    var isLocked: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    private val characterBounds = RectF()
    private val lockBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2B21B")
    }
    private val lockStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B2F")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val lockShacklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F2B21B")
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
    }
    private val lockPlatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 15, 23, 42)
    }
    private val keyholePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B2F")
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private var animTime = 0f
    private var isAnimating = false

    private val animRunnable = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            animTime += 1f / 60f
            invalidate()
            postDelayed(this, FRAME_MS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    private fun startAnimation() {
        if (isAnimating) return
        isAnimating = true
        postDelayed(animRunnable, FRAME_MS)
    }

    private fun stopAnimation() {
        isAnimating = false
        removeCallbacks(animRunnable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredWidth = (110 * density).toInt()
        val desiredHeight = (120 * density).toInt()

        val w = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredWidth, MeasureSpec.getSize(widthMeasureSpec))
            else -> desiredWidth
        }
        val h = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredHeight
        }
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Reserve the bottom strip for the lock caption when locked.
        val captionHeight = if (isLocked) h * 0.22f else 0f
        val usableHeight = h - captionHeight

        val portraitHeight = usableHeight * 0.94f
        val portraitWidth = portraitHeight * (92f / 124f)
        val safeWidth = minOf(portraitWidth, w * 0.86f)
        val centerX = w / 2f
        val bottom = usableHeight * 0.98f

        characterBounds.set(
            centerX - safeWidth / 2f,
            bottom - portraitHeight,
            centerX + safeWidth / 2f,
            bottom
        )

        if (isLocked) {
            canvas.save()
            canvas.saveLayerAlpha(characterBounds, 110)
            drawPortrait(canvas)
            canvas.restore()
            drawLock(canvas, w, usableHeight)
        } else {
            drawPortrait(canvas)
        }

        if (isLocked) {
            labelPaint.textSize = captionHeight * 0.50f
            canvas.drawText(
                "LOCKED",
                centerX,
                h - captionHeight * 0.28f,
                labelPaint
            )
        }
    }

    private fun drawPortrait(canvas: Canvas) {
        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = characterBounds,
            characterId = characterId,
            skinId = skinId,
            facingRight = true,
            animState = CharacterRenderer.AnimState.IDLE,
            animTime = animTime
        )
    }

    private fun drawLock(canvas: Canvas, w: Float, usableHeight: Float) {
        val cx = w / 2f
        val cy = usableHeight / 2f
        val bodyW = minOf(w * 0.46f, usableHeight * 0.42f)
        val bodyH = bodyW * 0.78f
        val plateRadius = bodyW * 0.85f

        // Dark disc behind the lock so the glyph reads over any costume colour.
        canvas.drawCircle(cx, cy, plateRadius, lockPlatePaint)

        val bodyTop = cy - bodyH * 0.18f
        val bodyRect = RectF(cx - bodyW / 2f, bodyTop, cx + bodyW / 2f, bodyTop + bodyH)
        canvas.drawRoundRect(bodyRect, bodyW * 0.18f, bodyW * 0.18f, lockBodyPaint)
        canvas.drawRoundRect(bodyRect, bodyW * 0.18f, bodyW * 0.18f, lockStrokePaint)

        val shackleRect = RectF(
            cx - bodyW * 0.30f,
            bodyTop - bodyH * 0.62f,
            cx + bodyW * 0.30f,
            bodyTop + bodyH * 0.10f
        )
        canvas.drawArc(shackleRect, 180f, 180f, false, lockShacklePaint)

        keyholePaint.alpha = 255
        canvas.drawCircle(cx, bodyTop + bodyH * 0.42f, bodyW * 0.11f, keyholePaint)
        canvas.drawRect(
            cx - bodyW * 0.045f,
            bodyTop + bodyH * 0.42f,
            cx + bodyW * 0.045f,
            bodyTop + bodyH * 0.72f,
            keyholePaint
        )
    }

    private companion object {
        const val FRAME_MS = 33L
    }
}
