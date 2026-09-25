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

    enum class Variant { ORANGE, GREEN, BLUE, GLASS }

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
            MotionEvent.ACTION_MOVE -> {
                val inBounds = event.x >= 0f && event.x <= width.toFloat() && event.y >= 0f && event.y <= height.toFloat()
                if (isPressedState != inBounds) {
                    isPressedState = inBounds
                    animate().scaleX(if (inBounds) 0.96f else 1f).scaleY(if (inBounds) 0.96f else 1f).setDuration(60).start()
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val inBounds = event.x >= 0f && event.x <= width.toFloat() && event.y >= 0f && event.y <= height.toFloat()
                isPressedState = false
                animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                invalidate()
                if (inBounds) {
                    performClick()
                }
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
            Variant.GLASS -> { topColor = Color.argb(220, 255, 170, 40); bottomColor = Color.argb(240, 235, 100, 0); shadowColor = Color.argb(255, 120, 40, 0) }
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

        val density = resources.displayMetrics.density
        val btnH = buttonRect.height()
        val hasSubText = subText.isNotBlank()

        // Responsive typography matching target specs
        val titleSizeSp = when {
            btnH >= 60f * density -> (btnH * 0.32f / density).coerceIn(22f, 26f)
            btnH >= 44f * density -> (btnH * 0.36f / density).coerceIn(16f, 20f)
            else -> (btnH * 0.40f / density).coerceIn(14f, 17f)
        }
        val titleSize = titleSizeSp * density
        titlePaint.textSize = titleSize
        titleShadowPaint.textSize = titleSize

        val iconSizeDp = when {
            btnH >= 60f * density -> (btnH * 0.38f / density).coerceIn(24f, 30f)
            btnH >= 44f * density -> (btnH * 0.42f / density).coerceIn(22f, 28f)
            else -> (btnH * 0.44f / density).coerceIn(18f, 24f)
        }
        val iconSize = iconSizeDp * density

        val subSizeSp = when {
            btnH >= 60f * density -> (btnH * 0.20f / density).coerceIn(13f, 16f)
            else -> (btnH * 0.22f / density).coerceIn(10f, 13f)
        }
        val subSize = subSizeSp * density
        subTitlePaint.textSize = subSize

        // Center group container: [ ICON ][ MAIN TEXT ]
        val titleWidth = titlePaint.measureText(mainText)
        val groupGap = 8f * density
        val groupWidth = iconSize + groupGap + titleWidth
        val groupLeft = buttonRect.centerX() - groupWidth / 2f

        val mainY = if (hasSubText) buttonRect.centerY() - (subSize * 0.3f) else buttonRect.centerY() + titleSize * 0.35f
        val iconCenterX = groupLeft + iconSize / 2f
        val iconCenterY = mainY - (titleSize * 0.32f)

        playIconPath.reset()
        playIconPath.moveTo(iconCenterX - iconSize * 0.35f, iconCenterY - iconSize * 0.45f)
        playIconPath.lineTo(iconCenterX + iconSize * 0.45f, iconCenterY)
        playIconPath.lineTo(iconCenterX - iconSize * 0.35f, iconCenterY + iconSize * 0.45f)
        playIconPath.close()
        canvas.drawPath(playIconPath, iconPaint)

        titlePaint.textAlign = Paint.Align.LEFT
        titleShadowPaint.textAlign = Paint.Align.LEFT

        val titleX = groupLeft + iconSize + groupGap
        canvas.drawText(mainText, titleX, mainY + 2f, titleShadowPaint)
        canvas.drawText(mainText, titleX, mainY, titlePaint)

        if (hasSubText) {
            val subY = buttonRect.centerY() + (subSize * 0.9f) + (4f * density)
            canvas.drawText(subText, buttonRect.centerX(), subY, subTitlePaint)
        }
    }
}