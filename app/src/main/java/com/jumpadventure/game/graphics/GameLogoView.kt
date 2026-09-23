package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GameLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val jumpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val adventurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        color = Color.parseColor("#0F172A")
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#168AF0")
    }

    private val bannerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Reusable RectF objects for onDraw
    private val bannerRect = RectF()
    private val shadowRect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredWidth = (320 * density).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val w = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val calculatedHeight = (w / 2.46f).toInt()
        val h = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(calculatedHeight, heightSize)
            else -> calculatedHeight
        }

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f

        // 1. "JUMP" Title (Yellow/Orange Glossy with 3D Shadow)
        val jumpTextSize = h * 0.40f
        jumpPaint.textSize = jumpTextSize
        outlinePaint.textSize = jumpTextSize
        outlinePaint.strokeWidth = jumpTextSize * 0.16f

        val jumpY = h * 0.38f

        // 3D Shadow Layers for "JUMP"
        for (i in 8 downTo 1) {
            jumpPaint.color = Color.parseColor("#C86A00")
            canvas.drawText("JUMP", cx, jumpY + i * 2f, jumpPaint)
        }

        // Dark Outline
        canvas.drawText("JUMP", cx, jumpY, outlinePaint)

        // Main Yellow Gradient Body
        jumpPaint.shader = LinearGradient(cx, jumpY - jumpTextSize, cx, jumpY, Color.parseColor("#FFD43B"), Color.parseColor("#FF9F1C"), Shader.TileMode.CLAMP)
        canvas.drawText("JUMP", cx, jumpY, jumpPaint)
        jumpPaint.shader = null

        // 2. "ADVENTURE" Subtitle (Cyan/Blue 3D Text)
        val advTextSize = h * 0.28f
        adventurePaint.textSize = advTextSize
        outlinePaint.textSize = advTextSize
        outlinePaint.strokeWidth = advTextSize * 0.16f

        val advY = h * 0.68f

        // 3D Shadow Layers for "ADVENTURE"
        for (i in 6 downTo 1) {
            adventurePaint.color = Color.parseColor("#0C5DAA")
            canvas.drawText("ADVENTURE", cx, advY + i * 2f, adventurePaint)
        }

        // Dark Outline
        canvas.drawText("ADVENTURE", cx, advY, outlinePaint)

        // Blue Gradient Body
        adventurePaint.shader = LinearGradient(cx, advY - advTextSize, cx, advY, Color.parseColor("#38BDF8"), Color.parseColor("#168AF0"), Shader.TileMode.CLAMP)
        canvas.drawText("ADVENTURE", cx, advY, adventurePaint)
        adventurePaint.shader = null

        // 3. "PREMIUM PLATFORMER" Ribbon Banner
        val bannerW = w * 0.72f
        val bannerH = h * 0.18f
        val bannerY = h * 0.78f
        bannerRect.set(cx - bannerW / 2f, bannerY, cx + bannerW / 2f, bannerY + bannerH)

        // Banner Drop Shadow
        shadowRect.set(bannerRect.left, bannerRect.top + 4f, bannerRect.right, bannerRect.bottom + 4f)
        canvas.drawRoundRect(shadowRect, 16f, 16f, outlinePaint)

        // Banner Body
        bannerBgPaint.shader = LinearGradient(bannerRect.left, bannerRect.top, bannerRect.right, bannerRect.bottom, Color.parseColor("#168AF0"), Color.parseColor("#0C5DAA"), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(bannerRect, 16f, 16f, bannerBgPaint)
        canvas.drawRoundRect(bannerRect, 16f, 16f, bannerBorderPaint)

        bannerTextPaint.textSize = bannerH * 0.55f
        canvas.drawText("PREMIUM PLATFORMER", cx, bannerRect.centerY() + bannerTextPaint.textSize * 0.35f, bannerTextPaint)
    }
}
