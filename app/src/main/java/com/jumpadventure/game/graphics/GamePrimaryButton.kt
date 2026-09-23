package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GamePrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var mainText: String = "PLAY NOW"
        set(value) { field = value; invalidate() }

    var subText: String = "LEVEL 1"
        set(value) { field = value; invalidate() }

    var variant: Variant = Variant.ORANGE
        set(value) { field = value; invalidate() }

    enum class Variant { ORANGE, GREEN, BLUE }

    private var isPressedState = false
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#FFE599")
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#50FFFFFF") }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#1F3045")
        textAlign = Paint.Align.CENTER
    }
    private val titleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#FFCA28")
        textAlign = Paint.Align.CENTER
    }
    private val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#4A2800")
        textAlign = Paint.Align.CENTER
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F3045")
        style = Paint.Style.FILL
    }

    private val buttonRect = RectF()
    private val shadowRect = RectF()
    private val highlightRect = RectF()
    private val playIconPath = Path()

    init {
        isClickable = true
        isFocusable = true
    }

    fun setPlayInfo(levelNum: Int) {
        mainText = "PLAY NOW"
        subText = "LEVEL $levelNum"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredWidth = (320 * density).toInt()
        val desiredHeight = (78 * density).toInt()
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressedState = true
                animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).start()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isPressedState = false
                animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                invalidate()
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressedState = false
                animate().scaleX(1f).scaleY(1f).setDuration(60).start()
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

        val shadowOffset = if (isPressedState) 3f else 8f
        val cornerRadius = h * 0.34f

        shadowRect.set(4f, shadowOffset + 3f, w - 4f, h)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        buttonRect.set(4f, 2f, w - 4f, h - shadowOffset)
        val topColor: Int
        val bottomColor: Int
        val shadowColor: Int
        when (variant) {
            Variant.ORANGE -> { topColor = Color.parseColor("#FFC107"); bottomColor = Color.parseColor("#FF8F00"); shadowColor = Color.parseColor("#A34A00") }
            Variant.GREEN -> { topColor = Color.parseColor("#8BEA3E"); bottomColor = Color.parseColor("#23A63A"); shadowColor = Color.parseColor("#0F6E21") }
            Variant.BLUE -> { topColor = Color.parseColor("#57D5FF"); bottomColor = Color.parseColor("#0878D8"); shadowColor = Color.parseColor("#07509A") }
        }
        shadowPaint.color = shadowColor
        bodyPaint.shader = LinearGradient(
            buttonRect.left, buttonRect.top, buttonRect.left, buttonRect.bottom,
            topColor, bottomColor, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, bodyPaint)

        highlightRect.set(
            buttonRect.left + 8f,
            buttonRect.top + 4f,
            buttonRect.right - 8f,
            buttonRect.top + buttonRect.height() * 0.38f
        )
        canvas.drawRoundRect(highlightRect, cornerRadius * 0.8f, cornerRadius * 0.8f, highlightPaint)
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, borderPaint)

        // Keep the PLAY NOW label as one compact centered visual group:
        // icon + text are centered together instead of anchoring text at a fixed X.
        val titleSize = minOf(buttonRect.height() * 0.22f, 20f)
        titlePaint.textSize = titleSize
        titleShadowPaint.textSize = titleSize

        val titleWidth = titlePaint.measureText(mainText)
        val iconSize = minOf(h * 0.26f, 19f)
        val groupGap = 10f
        val groupWidth = iconSize + groupGap + titleWidth
        val groupLeft = buttonRect.centerX() - groupWidth / 2f
        val iconCenterX = groupLeft + iconSize / 2f
        val iconCenterY = buttonRect.centerY() - 5f

        playIconPath.reset()
        playIconPath.moveTo(iconCenterX - iconSize * 0.35f, iconCenterY - iconSize * 0.5f)
        playIconPath.lineTo(iconCenterX + iconSize * 0.5f, iconCenterY)
        playIconPath.lineTo(iconCenterX - iconSize * 0.35f, iconCenterY + iconSize * 0.5f)
        playIconPath.close()
        canvas.drawPath(playIconPath, iconPaint)

        val titleX = groupLeft + iconSize + groupGap
        val titleY = buttonRect.centerY() - 4f
        canvas.drawText(mainText, titleX, titleY + 2f, titleShadowPaint)
        canvas.drawText(mainText, titleX, titleY, titlePaint)

        if (subText.isNotBlank()) {
            val subSize = minOf(buttonRect.height() * 0.13f, 11f)
            subTitlePaint.textSize = subSize
            val subY = buttonRect.centerY() + subSize + 5f
            canvas.drawText(subText, buttonRect.centerX(), subY, subTitlePaint)
        }
    }
}