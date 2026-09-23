package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class HomeEnvironmentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var animTime = 0f
    private val frameRate = 60f

    // Sky Gradient
    private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Mountains
    private val mountainFarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7BBEE8") }
    private val mountainMidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5A9ED4") }

    // Castle
    private val castlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0E6ED") }
    private val castleRoofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3B82F6") }

    // Waterfall
    private val waterfallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80E0FF") }
    private val waterfallSplashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    // Trees & Foliage
    private val treeTrunkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5D4037") }
    private val treeFoliageBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E7D32") }
    private val treeFoliageFrontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#43A047") }

    // Clouds
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CCFFFFFF") }

    // Foreground Grass
    private val grassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#388E3C") }

    // Particles & Sparkles
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    // Reusable Paths for render loop
    private val mountainFarPath = Path()
    private val mountainMidPath = Path()
    private val roofPath = Path()
    private val fgGrassPath = Path()

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

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. SKY GRADIENT (#58CFF0 to #9BE5FF)
        skyPaint.shader = LinearGradient(0f, 0f, 0f, h, Color.parseColor("#58CFF0"), Color.parseColor("#B3EEFF"), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, skyPaint)

        // 2. DRIFTING CLOUDS
        val cloud1X = (w * 0.15f + animTime * 12f) % (w + 200f) - 100f
        drawCloud(canvas, cloud1X, h * 0.12f, 80f)

        val cloud2X = (w * 0.65f + animTime * 8f) % (w + 250f) - 120f
        drawCloud(canvas, cloud2X, h * 0.20f, 110f)

        val cloud3X = (w * 0.40f + animTime * 15f) % (w + 180f) - 90f
        drawCloud(canvas, cloud3X, h * 0.08f, 70f)

        // 3. DISTANT MOUNTAINS
        mountainFarPath.reset()
        mountainFarPath.moveTo(0f, h * 0.52f)
        mountainFarPath.lineTo(w * 0.25f, h * 0.32f)
        mountainFarPath.lineTo(w * 0.55f, h * 0.52f)
        mountainFarPath.lineTo(w * 0.80f, h * 0.36f)
        mountainFarPath.lineTo(w, h * 0.52f)
        mountainFarPath.lineTo(w, h)
        mountainFarPath.lineTo(0f, h)
        mountainFarPath.close()
        canvas.drawPath(mountainFarPath, mountainFarPaint)

        mountainMidPath.reset()
        mountainMidPath.moveTo(0f, h * 0.58f)
        mountainMidPath.lineTo(w * 0.35f, h * 0.42f)
        mountainMidPath.lineTo(w * 0.70f, h * 0.60f)
        mountainMidPath.lineTo(w, h * 0.48f)
        mountainMidPath.lineTo(w, h)
        mountainMidPath.lineTo(0f, h)
        mountainMidPath.close()
        canvas.drawPath(mountainMidPath, mountainMidPaint)

        // 4. DISTANT CASTLE ON MOUNTAIN
        val castleX = w * 0.33f
        val castleY = h * 0.41f
        canvas.drawRect(castleX - 20f, castleY - 40f, castleX + 20f, castleY, castlePaint)

        roofPath.reset()
        roofPath.moveTo(castleX - 25f, castleY - 40f)
        roofPath.lineTo(castleX, castleY - 70f)
        roofPath.lineTo(castleX + 25f, castleY - 40f)
        roofPath.close()
        canvas.drawPath(roofPath, castleRoofPaint)

        canvas.drawRect(castleX - 45f, castleY - 25f, castleX - 25f, castleY, castlePaint)
        canvas.drawRect(castleX + 25f, castleY - 25f, castleX + 45f, castleY, castlePaint)

        // 5. WATERFALL FROM MOUNTAIN
        val waterfallX = w * 0.70f
        val waterfallTop = h * 0.50f
        val waterfallBottom = h * 0.75f
        val wfWidth = (18f + sin((animTime * 4f).toDouble()) * 2f).toFloat()
        canvas.drawRect(waterfallX - wfWidth / 2f, waterfallTop, waterfallX + wfWidth / 2f, waterfallBottom, waterfallPaint)

        val splashOffset = (animTime * 100f) % 30f
        canvas.drawCircle(waterfallX, waterfallBottom - splashOffset, wfWidth * 0.8f, waterfallSplashPaint)
        canvas.drawCircle(waterfallX - 10f, waterfallBottom, 12f, waterfallSplashPaint)
        canvas.drawCircle(waterfallX + 10f, waterfallBottom, 14f, waterfallSplashPaint)

        // 6. MIDGROUND TREES
        drawTree(canvas, w * 0.08f, h * 0.62f, 90f)
        drawTree(canvas, w * 0.20f, h * 0.65f, 75f)
        drawTree(canvas, w * 0.85f, h * 0.63f, 100f)
        drawTree(canvas, w * 0.94f, h * 0.66f, 80f)

        // 7. FOREGROUND GRASS & FOLIAGE AT BOTTOM
        fgGrassPath.reset()
        fgGrassPath.moveTo(0f, h * 0.88f)
        fgGrassPath.quadTo(w * 0.25f, h * 0.84f, w * 0.5f, h * 0.87f)
        fgGrassPath.quadTo(w * 0.75f, h * 0.90f, w, h * 0.86f)
        fgGrassPath.lineTo(w, h)
        fgGrassPath.lineTo(0f, h)
        fgGrassPath.close()
        canvas.drawPath(fgGrassPath, grassPaint)

        // 8. AMBIENT PARTICLES
        for (i in 0..12) {
            val px = (w * ((i * 37) % 100) / 100f + sin((animTime + i).toDouble()) * 15f).toFloat() % w
            val py = (h * 0.2f + (i * 60f + animTime * 20f) % (h * 0.7f))
            val pSize = 3f + (i % 4) * 1.5f
            particlePaint.alpha = (150 + sin((animTime * 3f + i).toDouble()) * 100).toInt().coerceIn(0, 255)
            canvas.drawCircle(px, py, pSize, particlePaint)
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        canvas.drawCircle(cx, cy, radius, cloudPaint)
        canvas.drawCircle(cx - radius * 0.6f, cy + radius * 0.1f, radius * 0.7f, cloudPaint)
        canvas.drawCircle(cx + radius * 0.6f, cy + radius * 0.1f, radius * 0.7f, cloudPaint)
        canvas.drawCircle(cx + radius * 1.1f, cy + radius * 0.2f, radius * 0.5f, cloudPaint)
    }

    private fun drawTree(canvas: Canvas, x: Float, y: Float, scale: Float) {
        val trunkWidth = scale * 0.25f
        val trunkHeight = scale * 0.6f
        canvas.drawRect(x - trunkWidth / 2f, y - trunkHeight, x + trunkWidth / 2f, y, treeTrunkPaint)

        val foliageRadius = scale * 0.55f
        canvas.drawCircle(x, y - trunkHeight - foliageRadius * 0.4f, foliageRadius, treeFoliageBackPaint)
        canvas.drawCircle(x - foliageRadius * 0.3f, y - trunkHeight - foliageRadius * 0.6f, foliageRadius * 0.8f, treeFoliageFrontPaint)
        canvas.drawCircle(x + foliageRadius * 0.3f, y - trunkHeight - foliageRadius * 0.6f, foliageRadius * 0.8f, treeFoliageFrontPaint)
    }
}
