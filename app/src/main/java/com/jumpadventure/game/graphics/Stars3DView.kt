package com.jumpadventure.game.graphics

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.cos
import kotlin.math.sin

class Stars3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var starsEarned: Int = 3
        set(value) {
            field = value.coerceIn(0, 3)
            invalidate()
        }

    var onStarImpactListener: ((index: Int, isCenter: Boolean) -> Unit)? = null

    private val starScales = floatArrayOf(1f, 1f, 1f)
    private val starFlashes = floatArrayOf(0f, 0f, 0f)
    private val shimmerOffsets = floatArrayOf(-1f, -1f, -1f)
    private val entranceAnimators = arrayOfNulls<ValueAnimator>(3)
    private var continuousShimmerAnimator: ValueAnimator? = null
    private var continuousShimmerProgress = -1f

    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 150
    }
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val starPath = Path()
    private val starFacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9C4200") }
    private val starUnearnedShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#334155") }
    private val starStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#632200")
    }
    private val unearnedStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1E293B")
    }
    private val starHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 255, 255)
    }

    fun stopAllAnimations() {
        entranceAnimators.forEach { it?.cancel() }
        entranceAnimators.fill(null)
        continuousShimmerAnimator?.cancel()
        continuousShimmerAnimator = null
        starScales.fill(1f)
        starFlashes.fill(0f)
        shimmerOffsets.fill(-1f)
        continuousShimmerProgress = -1f
    }

    fun startPopAnimation() {
        stopAllAnimations()

        val entranceOrder = intArrayOf(0, 1, 2)
        var maxEntranceTime = 0L

        for (orderIdx in 0 until 3) {
            val starIdx = entranceOrder[orderIdx]
            shimmerOffsets[starIdx] = -1f

            if (starIdx < starsEarned) {
                starScales[starIdx] = 0f
                starFlashes[starIdx] = 0f

                val delay = (orderIdx * 240).toLong()
                val duration = 320L
                val endTime = delay + duration
                if (endTime > maxEntranceTime) maxEntranceTime = endTime

                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    this.duration = duration
                    startDelay = delay
                    interpolator = OvershootInterpolator(if (starIdx == 1) 2.6f else 2.0f)

                    var soundTriggered = false

                    addUpdateListener { va ->
                        val progress = va.animatedValue as Float
                        starScales[starIdx] = progress

                        if (!soundTriggered && progress >= 0.25f) {
                            soundTriggered = true
                            starFlashes[starIdx] = 1.0f
                            onStarImpactListener?.invoke(starIdx, starIdx == 1)
                        }

                        if (starFlashes[starIdx] > 0f) {
                            starFlashes[starIdx] = (starFlashes[starIdx] - 0.12f).coerceAtLeast(0f)
                        }
                        invalidate()
                    }
                }
                entranceAnimators[starIdx] = animator
                animator.start()
            } else {
                starScales[starIdx] = 1f
                starFlashes[starIdx] = 0f
            }
        }

        if (starsEarned > 0) {
            startContinuousShimmer(delayStart = maxEntranceTime + 180L)
        }
        invalidate()
    }

    private fun startContinuousShimmer(delayStart: Long) {
        continuousShimmerAnimator?.cancel()
        continuousShimmerAnimator = ValueAnimator.ofFloat(-0.6f, 1.6f).apply {
            duration = 1800
            startDelay = delayStart
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                continuousShimmerProgress = va.animatedValue as Float
                invalidate()
            }
        }
        continuousShimmerAnimator?.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredWidth = (220 * density).toInt()
        val desiredHeight = (64 * density).toInt()
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

        val count = 3
        val starSize = (h * 0.36f).coerceIn(18f, 34f)
        val spacing = starSize * 2.55f
        val startX = w / 2f - (count - 1) * spacing / 2f
        val centerY = h * 0.54f

        for (i in 0 until count) {
            val cx = startX + i * spacing
            val isEarned = i < starsEarned
            val popScale = starScales[i]

            if (popScale <= 0f) continue

            // Center star is clearly larger (1.48x) and elevated higher
            val scaleFactor = if (i == 1) 1.48f else 1.0f
            val currentSize = starSize * scaleFactor
            val starCenterY = if (i == 1) centerY - starSize * 0.28f else centerY + starSize * 0.08f

            starStrokePaint.strokeWidth = (currentSize * 0.08f).coerceAtLeast(2.5f)
            unearnedStrokePaint.strokeWidth = (currentSize * 0.08f).coerceAtLeast(2.5f)

            canvas.save()
            canvas.scale(popScale, popScale, cx, starCenterY)

            // Setup star Path
            starPath.reset()
            for (p in 0 until 10) {
                val angle = Math.toRadians(-90.0 + p * 36.0)
                val radius = if (p % 2 == 0) currentSize else currentSize * 0.45f
                val px = cx + cos(angle).toFloat() * radius
                val py = starCenterY + sin(angle).toFloat() * radius
                if (p == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
            }
            starPath.close()

            // 1. Draw 3D bottom extrusion / shadow
            canvas.save()
            canvas.translate(0f, currentSize * 0.16f)
            canvas.drawPath(starPath, if (isEarned) starShadowPaint else starUnearnedShadowPaint)
            canvas.restore()

            // 2. Main face shader gradient
            starFacePaint.shader = if (isEarned) {
                LinearGradient(
                    cx - currentSize, starCenterY - currentSize, cx + currentSize, starCenterY + currentSize,
                    intArrayOf(
                        Color.parseColor("#FFF9C4"),
                        Color.parseColor("#FFD54F"),
                        Color.parseColor("#FFB300"),
                        Color.parseColor("#FF8F00")
                    ),
                    floatArrayOf(0f, 0.35f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
            } else {
                LinearGradient(
                    cx - currentSize, starCenterY - currentSize, cx + currentSize, starCenterY + currentSize,
                    intArrayOf(
                        Color.parseColor("#E2E8F0"),
                        Color.parseColor("#94A3B8"),
                        Color.parseColor("#64748B"),
                        Color.parseColor("#334155")
                    ),
                    floatArrayOf(0f, 0.35f, 0.75f, 1f),
                    Shader.TileMode.CLAMP
                )
            }

            canvas.drawPath(starPath, starFacePaint)
            canvas.drawPath(starPath, if (isEarned) starStrokePaint else unearnedStrokePaint)

            // 3. Highlight facet on top-left arm & premium angled 3D shimmer sweep
            if (isEarned) {
                val highlightPath = Path().apply {
                    moveTo(cx - currentSize * 0.38f, starCenterY - currentSize * 0.35f)
                    lineTo(cx - currentSize * 0.05f, starCenterY - currentSize * 0.75f)
                    lineTo(cx - currentSize * 0.15f, starCenterY - currentSize * 0.15f)
                    close()
                }
                canvas.drawPath(highlightPath, starHighlightPaint)

                // Flash burst on impact
                val flashAlpha = starFlashes[i]
                if (flashAlpha > 0f) {
                    flashPaint.alpha = (flashAlpha * 210).toInt()
                    canvas.drawCircle(cx, starCenterY, currentSize * 1.25f, flashPaint)
                }

                // Angled 3D shimmer sweep across earned stars
                val sweep = if (continuousShimmerProgress >= -0.5f) continuousShimmerProgress else shimmerOffsets[i]
                if (sweep >= -0.5f && sweep <= 1.5f) {
                    canvas.save()
                    canvas.clipPath(starPath)
                    canvas.rotate(-25f, cx, starCenterY)

                    val sweepX = cx - currentSize * 1.4f + sweep * currentSize * 2.8f
                    val sweepWidth = maxOf(4f, currentSize * 0.32f)

                    shimmerPaint.shader = LinearGradient(
                        sweepX - sweepWidth, 0f, sweepX + sweepWidth, 0f,
                        intArrayOf(
                            Color.argb(0, 255, 255, 255),
                            Color.argb(220, 255, 255, 255),
                            Color.argb(0, 255, 255, 255)
                        ),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )

                    canvas.drawRect(
                        sweepX - sweepWidth,
                        starCenterY - currentSize * 1.5f,
                        sweepX + sweepWidth,
                        starCenterY + currentSize * 1.5f,
                        shimmerPaint
                    )

                    shimmerPaint.shader = null
                    canvas.restore()
                }
            }

            canvas.restore()
        }
        starFacePaint.shader = null
    }

    override fun onDetachedFromWindow() {
        stopAllAnimations()
        super.onDetachedFromWindow()
    }
}
