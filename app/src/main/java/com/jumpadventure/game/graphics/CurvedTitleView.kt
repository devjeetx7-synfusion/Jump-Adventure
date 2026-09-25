package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class CurvedTitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var titleText: String = "LEVEL COMPLETE!"
        set(value) {
            field = value
            invalidate()
        }

    private val path = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        color = Color.parseColor("#6B2700")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#4A1600")
    }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        color = Color.argb(110, 255, 255, 255)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredW = (300f * density).toInt()
        val desiredH = (56f * density).toInt()
        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredW, MeasureSpec.getSize(widthMeasureSpec))
            else -> desiredW
        }
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredH, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredH
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || titleText.isBlank()) return

        val density = resources.displayMetrics.density
        val maxText = max(16f, w * 0.82f)
        var textSize = (h * 0.50f).coerceAtLeast(14f)
        fillPaint.textSize = textSize
        while (fillPaint.measureText(titleText) > maxText && textSize > 12f) {
            textSize -= 0.5f
            fillPaint.textSize = textSize
        }
        outlinePaint.textSize = textSize
        outlinePaint.strokeWidth = (textSize * 0.15f).coerceIn(2f * density, 5f * density)
        shadowPaint.textSize = textSize
        glossPaint.textSize = textSize
        glossPaint.strokeWidth = (textSize * 0.035f).coerceAtLeast(1f)

        // Subtle upward arch that follows the blue ribbon.
        val left = w * 0.10f
        val right = w * 0.90f
        val baseline = h * 0.74f
        val curve = (h * 0.16f).coerceAtMost(12f * density)

        path.reset()
        path.moveTo(left, baseline)
        path.quadTo(w * 0.50f, baseline - curve, right, baseline)

        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            Color.parseColor("#FFF59D"),
            Color.parseColor("#FFC107"),
            Shader.TileMode.CLAMP
        )

        // 3D/extruded shadow
        canvas.save()
        canvas.translate(0f, 3.5f * density)
        canvas.drawTextOnPath(titleText, path, 0f, 0f, shadowPaint)
        canvas.restore()

        canvas.drawTextOnPath(titleText, path, 0f, 0f, outlinePaint)
        canvas.drawTextOnPath(titleText, path, 0f, 0f, fillPaint)

        canvas.save()
        canvas.translate(0f, -1f * density)
        canvas.drawTextOnPath(titleText, path, 0f, 0f, glossPaint)
        canvas.restore()

        fillPaint.shader = null
    }
}
