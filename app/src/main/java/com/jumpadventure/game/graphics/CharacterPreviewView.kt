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

    private val platformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
    }
    private val platformBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#795548")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#30000000")
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B2F")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val coinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
    }

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0 || height <= 0) return

        val cx = width / 2f
        val platformY = height * 0.88f

        // 1. Draw Pedestal Platform
        val platformWidth = width * 0.7f
        val platformHeight = height * 0.18f
        val platformRect = RectF(
            cx - platformWidth / 2f,
            platformY,
            cx + platformWidth / 2f,
            platformY + platformHeight
        )

        // Drop shadow under platform
        canvas.drawOval(
            RectF(cx - platformWidth * 0.6f, platformY + platformHeight - 10f, cx + platformWidth * 0.6f, platformY + platformHeight + 20f),
            shadowPaint
        )

        // Platform Dirt Body
        canvas.drawRoundRect(platformRect, 24f, 24f, platformBodyPaint)
        canvas.drawRoundRect(platformRect, 24f, 24f, outlinePaint)

        // Platform Grass Top Cap
        val grassRect = RectF(
            platformRect.left,
            platformRect.top,
            platformRect.right,
            platformRect.top + platformHeight * 0.35f
        )
        canvas.drawRoundRect(grassRect, 20f, 20f, platformPaint)
        canvas.drawRoundRect(grassRect, 20f, 20f, outlinePaint)

        // Character Shadow on Platform
        val charShadowY = platformY + 6f
        canvas.drawOval(
            RectF(cx - width * 0.22f, charShadowY - 8f, cx + width * 0.22f, charShadowY + 8f),
            shadowPaint
        )

        // 2. Floating Animated Particles/Coins around hero
        for (i in 0..2) {
            val floatOffset = (sin((animTime * 3f + i * 2.0).toDouble()) * 8.0).toFloat()
            val coinX = cx + if (i % 2 == 0) -width * 0.28f else width * 0.28f
            val coinY = height * 0.25f + (i * 25f) + floatOffset

            canvas.drawCircle(coinX, coinY, 14f, coinPaint)
            canvas.drawCircle(coinX, coinY, 14f, outlinePaint)
        }

        // 3. Draw Character Hero
        val charWidth = width * 0.45f
        val charHeight = height * 0.65f
        val charBounds = RectF(
            cx - charWidth / 2f,
            platformY - charHeight + 10f,
            cx + charWidth / 2f,
            platformY + 10f
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
