package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class RewardSummaryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var coins: Int = 0
        set(value) { field = value; invalidate() }

    var stars: Int = 0
        set(value) { field = value; invalidate() }

    var timeSeconds: Float = 0f
        set(value) { field = value; invalidate() }

    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2A38")
        style = Paint.Style.FILL
    }
    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C4257")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val cardHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15FFFFFF")
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#90A4AE")
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#FFFDF7")
        textAlign = Paint.Align.CENTER
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val cardRect = RectF()
    private val starPath = Path()

    fun setRewardData(coinsEarned: Int, starsEarned: Int, timeSec: Float) {
        coins = coinsEarned
        stars = starsEarned
        timeSeconds = timeSec
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredWidth = (300 * density).toInt()
        val desiredHeight = (68 * density).toInt()
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val w = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }
        val h = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val cardGap = 8f * density
        val cardCount = 3
        val cardW = (w - (cardCount - 1) * cardGap) / cardCount
        val cardH = h
        val cornerRadius = 14f * density

        val labelSize = (cardH * 0.17f / density).coerceIn(9f, 12f) * density
        val valueSize = (cardH * 0.26f / density).coerceIn(12f, 16f) * density
        val iconSize = (cardH * 0.28f).coerceIn(14f, 24f)

        labelPaint.textSize = labelSize
        valuePaint.textSize = valueSize

        val items = listOf(
            Triple("COINS", "+$coins", ::drawCoinIcon),
            Triple("STARS", "+$stars", ::drawStarIcon),
            Triple("TIME", String.format("%.1fs", timeSeconds), ::drawTimerIcon)
        )

        for (i in items.indices) {
            val (label, value, drawIcon) = items[i]
            val left = i * (cardW + cardGap)
            cardRect.set(left, 0f, left + cardW, cardH)

            // Draw Card background & border
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardBgPaint)
            val highlightRect = RectF(cardRect.left + 4f, cardRect.top + 2f, cardRect.right - 4f, cardRect.top + cardH * 0.4f)
            canvas.drawRoundRect(highlightRect, cornerRadius * 0.8f, cornerRadius * 0.8f, cardHighlightPaint)
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardBorderPaint)

            val centerX = cardRect.centerX()
            val iconY = cardRect.top + cardH * 0.28f

            // Draw custom icon
            drawIcon(canvas, centerX, iconY, iconSize)

            // Draw Label and Value
            val labelY = iconY + iconSize * 0.95f + labelSize * 0.6f
            val valueY = labelY + valueSize * 1.15f

            canvas.drawText(label, centerX, labelY, labelPaint)
            canvas.drawText(value, centerX, valueY, valuePaint)
        }
    }

    private fun drawCoinIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val r = size * 0.85f
        iconPaint.shader = RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.4f,
            intArrayOf(Color.parseColor("#FFF59D"), Color.parseColor("#FFC107"), Color.parseColor("#FF8F00")),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, iconPaint)
        iconStrokePaint.color = Color.parseColor("#B36200")
        canvas.drawCircle(cx, cy, r, iconStrokePaint)
        canvas.drawCircle(cx, cy, r * 0.65f, iconStrokePaint)
        iconPaint.shader = null
    }

    private fun drawStarIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val r = size * 0.85f
        starPath.reset()
        for (p in 0 until 10) {
            val angle = Math.toRadians(-90.0 + p * 36.0)
            val radius = if (p % 2 == 0) r else r * 0.45f
            val px = cx + cos(angle).toFloat() * radius
            val py = cy + sin(angle).toFloat() * radius
            if (p == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()

        iconPaint.shader = LinearGradient(
            cx - r, cy - r, cx + r, cy + r,
            Color.parseColor("#FFF176"), Color.parseColor("#FFB300"), Shader.TileMode.CLAMP
        )
        canvas.drawPath(starPath, iconPaint)
        iconStrokePaint.color = Color.parseColor("#8F4C00")
        canvas.drawPath(starPath, iconStrokePaint)
        iconPaint.shader = null
    }

    private fun drawTimerIcon(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val r = size * 0.75f
        iconPaint.shader = RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            Color.parseColor("#81D4FA"), Color.parseColor("#0288D1"), Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, iconPaint)
        iconStrokePaint.color = Color.parseColor("#014570")
        canvas.drawCircle(cx, cy, r, iconStrokePaint)

        // Top button notch
        canvas.drawRect(cx - r * 0.25f, cy - r * 1.25f, cx + r * 0.25f, cy - r * 0.85f, iconStrokePaint)

        // Clock hands
        val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, cy, cx, cy - r * 0.55f, handPaint)
        canvas.drawLine(cx, cy, cx + r * 0.4f, cy, handPaint)
        iconPaint.shader = null
    }
}
