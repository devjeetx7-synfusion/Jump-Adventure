package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Circular 3D power-up control used in gameplay.
 * The click behaviour remains owned by MainActivity; this view is visual only.
 */
class GamePowerUpButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Type { MAGNET, SPEED, SHIELD }

    var type: Type = Type.MAGNET
        set(value) { field = value; invalidate() }

    var count: Int = 3
        set(value) { field = value.coerceAtLeast(0); invalidate() }

    var active: Boolean = false
        set(value) { field = value; invalidate() }

    private var pressedState = false
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(95, 255, 255, 255)
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#172033")
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        val desired = (68f * d).toInt()
        val w = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desired, MeasureSpec.getSize(widthMeasureSpec))
            else -> desired
        }
        val h = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desired, MeasureSpec.getSize(heightMeasureSpec))
            else -> desired
        }
        setMeasuredDimension(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedState = true
                animate().scaleX(0.91f).scaleY(0.91f).setDuration(70).start()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                pressedState = false
                animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                performClick()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedState = false
                animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.42f
        val y = cy + if (pressedState) 2f else 5f

        shadowPaint.color = Color.argb(115, 0, 25, 55)
        canvas.drawCircle(cx, y + 3f, radius, shadowPaint)

        val top: Int
        val bottom: Int
        when (type) {
            Type.MAGNET -> { top = Color.parseColor("#42D9FF"); bottom = Color.parseColor("#0879D7") }
            Type.SPEED -> { top = Color.parseColor("#5FE7FF"); bottom = Color.parseColor("#0874C8") }
            Type.SHIELD -> { top = Color.parseColor("#4DE3B0"); bottom = Color.parseColor("#078D6D") }
        }

        facePaint.shader = LinearGradient(cx, cy - radius, cx, cy + radius, top, bottom, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, y, radius, facePaint)
        facePaint.shader = null
        canvas.drawCircle(cx, y, radius, rimPaint)

        canvas.drawOval(
            RectF(cx - radius * .55f, y - radius * .72f, cx + radius * .10f, y - radius * .30f),
            highlightPaint
        )

        drawIcon(canvas, cx, y, radius * .54f)

        val badgeR = radius * .34f
        val bx = cx + radius * .70f
        val by = y - radius * .70f
        badgePaint.color = if (active) Color.parseColor("#FFD43B") else Color.parseColor("#172033")
        canvas.drawCircle(bx, by, badgeR, badgePaint)
        badgeTextPaint.textSize = badgeR * 1.18f
        badgeTextPaint.color = if (active) Color.parseColor("#172033") else Color.WHITE
        canvas.drawText(count.toString(), bx, by - (badgeTextPaint.ascent() + badgeTextPaint.descent()) / 2f, badgeTextPaint)
    }

    private fun drawIcon(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        iconPaint.style = Paint.Style.STROKE
        iconPaint.strokeWidth = s * .18f
        when (type) {
            Type.MAGNET -> {
                val r = s * .55f
                val rect = RectF(cx - r, cy - r * .55f, cx + r, cy + r * .95f)
                canvas.drawArc(rect, 180f, 180f, false, iconPaint)
                iconPaint.strokeWidth = s * .24f
                canvas.drawLine(cx - r, cy + r * .18f, cx - r, cy + r * .65f, iconPaint)
                canvas.drawLine(cx + r, cy + r * .18f, cx + r, cy + r * .65f, iconPaint)
            }
            Type.SPEED -> {
                val p = Path()
                p.moveTo(cx - s * .55f, cy + s * .55f)
                p.lineTo(cx + s * .05f, cy + s * .05f)
                p.lineTo(cx - s * .10f, cy + s * .05f)
                p.lineTo(cx + s * .55f, cy - s * .60f)
                p.lineTo(cx - s * .02f, cy - s * .08f)
                p.lineTo(cx + s * .13f, cy - s * .08f)
                p.close()
                iconPaint.style = Paint.Style.FILL
                canvas.drawPath(p, iconPaint)
            }
            Type.SHIELD -> {
                val p = Path()
                p.moveTo(cx, cy - s * .65f)
                p.lineTo(cx + s * .58f, cy - s * .38f)
                p.lineTo(cx + s * .46f, cy + s * .38f)
                p.quadTo(cx, cy + s * .72f, cx - s * .46f, cy + s * .38f)
                p.lineTo(cx - s * .58f, cy - s * .38f)
                p.close()
                iconPaint.style = Paint.Style.FILL
                canvas.drawPath(p, iconPaint)
            }
        }
        iconPaint.style = Paint.Style.FILL
    }
}
