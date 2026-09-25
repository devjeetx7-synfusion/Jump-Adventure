package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class SparkleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Particle(
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var size: Float = 0f,
        var alpha: Float = 0f,
        var maxAlpha: Float = 1f,
        var color: Int = Color.WHITE,
        var lifetime: Float = 0f,
        var maxLifetime: Float = 1f,
        var shape: Int = 0 // 0: star/diamond, 1: circle
    )

    private val particles = Array(32) { Particle() }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPath = Path()
    private var isAnimating = false
    private var animProgress = 0f

    init {
        // Touch pass-through so particles never block clicks
        isClickable = false
        isFocusable = false
    }

    fun triggerBurst() {
        val w = width.toFloat().coerceAtLeast(100f)
        val h = height.toFloat().coerceAtLeast(100f)
        val originX = w / 2f
        // Burst from the center of the modal rather than the overlay's top area.
        val originY = h * 0.50f

        val colors = intArrayOf(
            Color.parseColor("#FFD43B"), // Gold
            Color.parseColor("#FF9F1C"), // Orange
            Color.parseColor("#36C96F"), // Green
            Color.parseColor("#58CFF0"), // Light Blue
            Color.parseColor("#FFFFFF")  // White
        )

        val rng = Random(System.currentTimeMillis())
        for (p in particles) {
            p.x = originX + (rng.nextFloat() - 0.5f) * (w * 0.2f)
            p.y = originY + (rng.nextFloat() - 0.5f) * (h * 0.1f)
            val angle = rng.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (120f + rng.nextFloat() * 280f)
            p.vx = Math.cos(angle.toDouble()).toFloat() * speed
            p.vy = Math.sin(angle.toDouble()).toFloat() * speed - 60f
            p.size = 8f + rng.nextFloat() * 14f
            p.maxAlpha = 0.7f + rng.nextFloat() * 0.3f
            p.alpha = 0f
            p.color = colors[rng.nextInt(colors.size)]
            p.maxLifetime = 0.8f + rng.nextFloat() * 0.6f
            p.lifetime = 0f
            p.shape = if (rng.nextBoolean()) 0 else 1
        }

        isAnimating = true
        animProgress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        val dt = 0.033f // ~30 FPS frame delta
        animProgress += dt
        var activeCount = 0

        for (p in particles) {
            p.lifetime += dt
            if (p.lifetime >= p.maxLifetime) continue

            activeCount++
            val progress = p.lifetime / p.maxLifetime
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 90f * dt // gentle gravity drag

            // Fade in quickly, expand, then fade out
            p.alpha = if (progress < 0.2f) {
                (progress / 0.2f) * p.maxAlpha
            } else {
                (1f - (progress - 0.2f) / 0.8f) * p.maxAlpha
            }

            particlePaint.color = p.color
            particlePaint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)

            if (p.shape == 0) {
                // Diamond / Star particle
                starPath.reset()
                val s = p.size
                starPath.moveTo(p.x, p.y - s)
                starPath.lineTo(p.x + s * 0.4f, p.y - s * 0.4f)
                starPath.lineTo(p.x + s, p.y)
                starPath.lineTo(p.x + s * 0.4f, p.y + s * 0.4f)
                starPath.lineTo(p.x, p.y + s)
                starPath.lineTo(p.x - s * 0.4f, p.y + s * 0.4f)
                starPath.lineTo(p.x - s, p.y)
                starPath.lineTo(p.x - s * 0.4f, p.y - s * 0.4f)
                starPath.close()
                canvas.drawPath(starPath, particlePaint)
            } else {
                // Glowing dot particle
                canvas.drawCircle(p.x, p.y, p.size * 0.45f, particlePaint)
            }
        }

        if (activeCount > 0) {
            postInvalidateDelayed(33)
        } else {
            isAnimating = false
        }
    }
}
