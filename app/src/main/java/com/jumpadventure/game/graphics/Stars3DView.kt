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

    private val starScales = floatArrayOf(1f, 1f, 1f)

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

    fun startPopAnimation() {
        for (i in 0 until 3) {
            shimmerAnimators[i]?.cancel()
            shimmerAnimators[i] = null
            shimmerOffsets[i] = -1f
            if (i < starsEarned) {
                starScales[i] = 0f
                val pop = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 280
                    startDelay = (i * 140).toLong()
                    interpolator = OvershootInterpolator(2.0f)
                    addUpdateListener { va ->
                        starScales[i] = va.animatedValue as Float
                        invalidate()
                    }
                }
                pop.start()

                val shimmer = ValueAnimator.ofFloat(-1f, 1.15f).apply {
                    duration = 900
                    startDelay = (i * 140 + 420).toLong()
                    repeatCount = 1
                    interpolator = android.view.animation.LinearInterpolator()
                    addUpdateListener { va ->
                        shimmerOffsets[i] = va.animatedValue as Float
                        invalidate()
                    }
                }
                shimmerAnimators[i] = shimmer
                shimmer.start()
            } else {
                starScales[i] = 1f
            }
        }
        invalidate()
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
        val starSize = (h * 0.38f).coerceIn(16f, 32f)
        val spacing = starSize * 2.3f
        val startX = w / 2f - (count - 1) * spacing / 2f
        val centerY = h / 2f

        starStrokePaint.strokeWidth = (starSize * 0.08f).coerceAtLeast(2.5f)
        unearnedStrokePaint.strokeWidth = (starSize * 0.08f).coerceAtLeast(2.5f)

        for (i in 0 until count) {
            val cx = startX + i * spacing
            val isEarned = i < starsEarned
            val popScale = starScales[i]

            if (popScale <= 0f) continue

            canvas.save()
            canvas.scale(popScale, popScale, cx, centerY)

            // Middle star is slightly larger for 3D reward balance
            val scaleFactor = if (i == 1) 1.18f else 1.0f
            val currentSize = starSize * scaleFactor

            // Setup star Path
            starPath.reset()
            for (p in 0 until 10) {
                val angle = Math.toRadians(-90.0 + p * 36.0)
                val radius = if (p % 2 == 0) currentSize else currentSize * 0.45f
                val px = cx + cos(angle).toFloat() * radius
                val py = centerY + sin(angle).toFloat() * radius
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
                    cx - currentSize, centerY - currentSize, cx + currentSize, centerY + currentSize,
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
                    cx - currentSize, centerY - currentSize, cx + currentSize, centerY + currentSize,
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

            // 3. Highlight facet on top-left arm
            if (isEarned) {
                val highlightPath = Path().apply {
                    moveTo(cx - currentSize * 0.38f, centerY - currentSize * 0.35f)
                    lineTo(cx - currentSize * 0.05f, centerY - currentSize * 0.75f)
                    lineTo(cx - currentSize * 0.15f, centerY - currentSize * 0.15f)
                    close()
                }
                canvas.drawPath(highlightPath, starHighlightPaint)

                // One-pass shimmer sweep across each earned star.
                val sweep = shimmerOffsets[i]
                if (sweep >= -0.5f && sweep <= 1.1f) {
                    canvas.save()
                    canvas.clipPath(starPath)
                    val sweepX = cx - currentSize + sweep * currentSize * 2.0f
                    shimmerPaint.alpha = 150
                    val sweepWidth = maxOf(3f, currentSize * 0.14f)
                    val sweepRect = RectF(
                        sweepX - sweepWidth,
                        centerY - currentSize * 1.15f,
                        sweepX + sweepWidth,
                        centerY + currentSize * 1.15f
                    )
                    canvas.drawRoundRect(sweepRect, sweepWidth, sweepWidth, shimmerPaint)
                    canvas.restore()
                }
            }

            canvas.restore()
        }
        starFacePaint.shader = null
    }
}
