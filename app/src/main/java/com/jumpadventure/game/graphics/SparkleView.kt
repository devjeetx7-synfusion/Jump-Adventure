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

    private val particles = Array(72) { Particle() }
    private val sparkleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPath = Path()
    private var isCelebrationActive = false
    private val rng = Random(System.currentTimeMillis())

    private val colors = intArrayOf(
        Color.parseColor("#FFD43B"), // Gold
        Color.parseColor("#FF9F1C"), // Orange
        Color.parseColor("#36C96F"), // Green
        Color.parseColor("#58CFF0"), // Light Blue
        Color.parseColor("#FFFFFF")  // White
    )

    init {
        // Touch pass-through so particles never block clicks
        isClickable = false
        isFocusable = false
    }

    fun startCelebration() {
        isCelebrationActive = true
        for (p in particles) {
            resetParticle(p, isInitial = true)
        }
        invalidate()
    }

    fun triggerBurst() {
        startCelebration()
    }

    fun stopCelebration() {
        isCelebrationActive = false
        for (p in particles) {
            p.lifetime = p.maxLifetime
            p.alpha = 0f
        }
        invalidate()
    }

    private fun resetParticle(p: Particle, isInitial: Boolean = false) {
        val w = width.toFloat().coerceAtLeast(100f)
        val h = height.toFloat().coerceAtLeast(100f)
        val originX = w / 2f
        val originY = h * 0.50f
        // Broad celebration launch zone so particles can burst beyond the board edges.
        val originSpreadX = w * 0.55f
        val originSpreadY = h * 0.22f

        p.x = originX + (rng.nextFloat() - 0.5f) * originSpreadX
        p.y = originY + (rng.nextFloat() - 0.5f) * originSpreadY

        val angle = rng.nextFloat() * 2f * Math.PI.toFloat()
        val speed = 80f + rng.nextFloat() * 220f
        p.vx = Math.cos(angle.toDouble()).toFloat() * speed
        p.vy = Math.sin(angle.toDouble()).toFloat() * speed - 40f
        p.size = 7f + rng.nextFloat() * 20f
        p.maxAlpha = 0.75f + rng.nextFloat() * 0.25f
        p.alpha = 0f
        p.color = colors[rng.nextInt(colors.size)]
        p.maxLifetime = 0.8f + rng.nextFloat() * 0.8f
        p.lifetime = if (isInitial) rng.nextFloat() * p.maxLifetime else 0f
        p.shape = if (rng.nextBoolean()) 0 else 1
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopCelebration()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != VISIBLE) {
            stopCelebration()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isCelebrationActive) return

        val dt = 0.033f // ~30 FPS frame delta
        var activeCount = 0

        for (p in particles) {
            p.lifetime += dt
            if (p.lifetime >= p.maxLifetime) {
                if (isCelebrationActive) {
                    resetParticle(p, isInitial = false)
                } else {
                    continue
                }
            }

            activeCount++
            val progress = (p.lifetime / p.maxLifetime).coerceIn(0f, 1f)
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 60f * dt // gentle gravity drag

            // Fade in quickly, expand, then fade out
            p.alpha = if (progress < 0.2f) {
                (progress / 0.2f) * p.maxAlpha
            } else {
                (1f - (progress - 0.2f) / 0.8f) * p.maxAlpha
            }

            particlePaint.color = p.color
            particlePaint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            sparkleGlowPaint.color = p.color
            sparkleGlowPaint.alpha = (p.alpha * 70f).toInt().coerceIn(0, 80)

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
                canvas.drawCircle(p.x, p.y, p.size * 1.45f, sparkleGlowPaint)
                canvas.drawPath(starPath, particlePaint)
            } else {
                // Glowing dot particle
                canvas.drawCircle(p.x, p.y, p.size * 0.9f, sparkleGlowPaint)
                canvas.drawCircle(p.x, p.y, p.size * 0.45f, particlePaint)
            }
        }

        if (isCelebrationActive || activeCount > 0) {
            postInvalidateDelayed(33)
        }
    }
}
