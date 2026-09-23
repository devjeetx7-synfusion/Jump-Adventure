package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class CharacterPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var selectedCharacterId: String = "DEFAULT"
        set(value) {
            field = value
            invalidate()
        }

    private var animTime = 0f
    private val frameRate = 60f

    private val grassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#43A047") }
    private val grassLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#66BB6A") }
    private val soilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D4C41") }
    private val soilDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4E342E") }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#33000000") }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B2F")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val coinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD43B") }
    private val coinInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE066") }
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val rockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D6E63") }

    // Reusable objects for onDraw to avoid garbage allocation
    private val soilRect = RectF()
    private val grassRect = RectF()
    private val grassHighlightRect = RectF()
    private val charBounds = RectF()
    private val shadowOval = RectF()
    private val soilPath = Path()

    private val animRunnable = object : Runnable {
        override fun run() {
            animTime += 1f / frameRate
            invalidate()
            postDelayed(this, (1000 / frameRate).toLong())
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animRunnable)
        super.onDetachedFromWindow()
    }

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

        val calculatedHeight = (w / 1.14f).toInt()
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
        val platformY = h * 0.72f

        // 1. Floating Grassy Platform with Soil Depth
        val platformW = w * 0.82f
        val platformH = h * 0.22f
        soilRect.set(cx - platformW / 2f, platformY, cx + platformW / 2f, platformY + platformH)

        // Platform Bottom Shadow
        shadowOval.set(cx - platformW * 0.55f, platformY + platformH - 8f, cx + platformW * 0.55f, platformY + platformH + 18f)
        canvas.drawOval(shadowOval, shadowPaint)

        // Soil Depth Body Path
        soilPath.reset()
        soilPath.moveTo(soilRect.left + 24f, soilRect.top)
        soilPath.lineTo(soilRect.right - 24f, soilRect.top)
        soilPath.quadTo(soilRect.right, soilRect.top, soilRect.right - 12f, soilRect.top + 30f)
        soilPath.lineTo(soilRect.right - 30f, soilRect.bottom)
        soilPath.quadTo(soilRect.centerX(), soilRect.bottom + 16f, soilRect.left + 30f, soilRect.bottom)
        soilPath.lineTo(soilRect.left + 12f, soilRect.top + 30f)
        soilPath.quadTo(soilRect.left, soilRect.top, soilRect.left + 24f, soilRect.top)
        soilPath.close()

        soilPaint.shader = LinearGradient(soilRect.left, soilRect.top, soilRect.left, soilRect.bottom, soilPaint.color, soilDarkPaint.color, Shader.TileMode.CLAMP)
        canvas.drawPath(soilPath, soilPaint)
        canvas.drawPath(soilPath, outlinePaint)

        // Soil Details (Rocks)
        canvas.drawCircle(cx - platformW * 0.25f, platformY + platformH * 0.5f, 8f, rockPaint)
        canvas.drawCircle(cx + platformW * 0.2f, platformY + platformH * 0.6f, 12f, rockPaint)
        canvas.drawCircle(cx - 10f, platformY + platformH * 0.7f, 6f, rockPaint)

        // Grass Top Cap
        val grassH = platformH * 0.38f
        grassRect.set(soilRect.left - 6f, platformY - 8f, soilRect.right + 6f, platformY + grassH)
        canvas.drawRoundRect(grassRect, 18f, 18f, grassPaint)
        canvas.drawRoundRect(grassRect, 18f, 18f, outlinePaint)

        // Grass Top Highlight Strip
        grassHighlightRect.set(grassRect.left + 10f, grassRect.top + 4f, grassRect.right - 10f, grassRect.top + 14f)
        canvas.drawRoundRect(grassHighlightRect, 8f, 8f, grassLightPaint)

        // 2. Character Foot Shadow on Platform
        shadowOval.set(cx - w * 0.22f, platformY + 2f, cx + w * 0.22f, platformY + 18f)
        canvas.drawOval(shadowOval, shadowPaint)

        // 3. Floating Coins & Sparkles
        for (i in 0..3) {
            val floatOffset = (sin((animTime * 3.5f + i * 1.8).toDouble()) * 10.0).toFloat()
            val coinX = if (i % 2 == 0) cx - w * (0.34f + i * 0.04f) else cx + w * (0.34f + (i - 1) * 0.04f)
            val coinY = h * 0.22f + (i * 32f) + floatOffset

            canvas.drawCircle(coinX, coinY, 14f, coinPaint)
            canvas.drawCircle(coinX, coinY, 9f, coinInnerPaint)
            canvas.drawCircle(coinX, coinY, 14f, outlinePaint)

            val sparkleX = coinX + 16f
            val sparkleY = coinY - 16f
            val sparkleSize = (6f + sin((animTime * 6f + i).toDouble()) * 2f).toFloat()
            canvas.drawCircle(sparkleX, sparkleY, sparkleSize, sparklePaint)
        }

        // 4. Draw Character Hero
        val charW = w * 0.52f
        val charH = h * 0.65f
        charBounds.set(
            cx - charW / 2f,
            platformY - charH + 14f,
            cx + charW / 2f,
            platformY + 14f
        )

        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = charBounds,
            characterId = selectedCharacterId,
            facingRight = true,
            animState = CharacterRenderer.AnimState.IDLE,
            animTime = animTime
        )
    }
}
