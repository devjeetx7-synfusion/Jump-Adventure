package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

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

    private val textPath = Path()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        color = Color.parseColor("#4A1D00")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#3B1200")
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        color = Color.parseColor("#80FFFFFF")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredWidth = (300 * density).toInt()
        val desiredHeight = (56 * density).toInt()
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
        if (w <= 0f || h <= 0f || titleText.isBlank()) return

        val density = resources.displayMetrics.density
        // Dynamic text sizing based on height and width
        val textSizeSp = (h * 0.48f / density).coerceIn(18f, 26f)
        val textSizePx = textSizeSp * density

        textPaint.textSize = textSizePx
        strokePaint.textSize = textSizePx
        strokePaint.strokeWidth = textSizePx * 0.18f
        shadowPaint.textSize = textSizePx
        highlightPaint.textSize = textSizePx
        highlightPaint.strokeWidth = textSizePx * 0.05f

        // Upward arc path: starts left, arches slightly downward/upward to give subtle game logo curve
        val arcRadius = w * 1.5f
        val arcHeight = h * 0.22f

        textPath.reset()
        // Draw a gentle upward arch path across the center
        val startX = w * 0.05f
        val endX = w * 0.95f
        val startY = h * 0.72f
        val controlY = h * 0.72f - arcHeight
        textPath.moveTo(startX, startY)
        textPath.quadTo(w * 0.5f, controlY, endX, startY)

        // Gradient shader for golden/orange text fill
        textPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.parseColor("#FFF7A0"),
                Color.parseColor("#FFD43B"),
                Color.parseColor("#FF9F1C"),
                Color.parseColor("#E65100")
            ),
            floatArrayOf(0f, 0.35f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )

        // Draw dark shadow/extrusion slightly offset downward
        canvas.save()
        canvas.translate(0f, 4f * density)
        canvas.drawTextOnPath(titleText, textPath, 0f, 0f, shadowPaint)
        canvas.restore()

        // Draw dark stroke outline
        canvas.drawTextOnPath(titleText, textPath, 0f, 0f, strokePaint)

        // Draw main golden text
        canvas.drawTextOnPath(titleText, textPath, 0f, 0f, textPaint)

        // Draw top subtle highlight
        canvas.save()
        canvas.translate(0f, -1.5f * density)
        canvas.drawTextOnPath(titleText, textPath, 0f, 0f, highlightPaint)
        canvas.restore()
    }
}
