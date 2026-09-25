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

    enum class Variant { ORANGE, GREEN, BLUE, GOLD, PURPLE, RED, GRAY, GLASS }
    enum class IconType { AUTO, PLAY, RESTART, HOME, SETTINGS, NONE }

    var iconType: IconType = IconType.AUTO
        set(value) { field = value; invalidate() }

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
        color = Color.parseColor("#FFFDF7")
        textAlign = Paint.Align.CENTER
    }
    private val titleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#1B2A38")
        textAlign = Paint.Align.CENTER
    }
    private val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#FFE599")
        textAlign = Paint.Align.CENTER
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val buttonRect = RectF()
    private val shadowRect = RectF()
    private val highlightRect = RectF()
    private val iconPath = Path()

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

        val shadowOffset = if (isPressedState) 3f else 7f
        val cornerRadius = h * 0.32f

        shadowRect.set(4f, shadowOffset + 3f, w - 4f, h)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        buttonRect.set(4f, 2f, w - 4f, h - shadowOffset)
        val topColor: Int
        val bottomColor: Int
        val shadowColor: Int
        val borderColor: Int
        when (if (!isEnabled) Variant.GRAY else variant) {
            Variant.ORANGE -> { topColor = Color.parseColor("#FFC107"); bottomColor = Color.parseColor("#FF8F00"); shadowColor = Color.parseColor("#A34A00"); borderColor = Color.parseColor("#FFE599") }
            Variant.GREEN -> { topColor = Color.parseColor("#8BEA3E"); bottomColor = Color.parseColor("#23A63A"); shadowColor = Color.parseColor("#0F6E21"); borderColor = Color.parseColor("#C8FF8C") }
            Variant.BLUE -> { topColor = Color.parseColor("#57D5FF"); bottomColor = Color.parseColor("#0878D8"); shadowColor = Color.parseColor("#07509A"); borderColor = Color.parseColor("#B3F0FF") }
            Variant.GOLD -> { topColor = Color.parseColor("#FFEE58"); bottomColor = Color.parseColor("#F57F17"); shadowColor = Color.parseColor("#8F4C00"); borderColor = Color.parseColor("#FFF9C4") }
            Variant.PURPLE -> { topColor = Color.parseColor("#E040FB"); bottomColor = Color.parseColor("#7B1FA2"); shadowColor = Color.parseColor("#4A148C"); borderColor = Color.parseColor("#EA80FC") }
            Variant.RED -> { topColor = Color.parseColor("#FF5252"); bottomColor = Color.parseColor("#D32F2F"); shadowColor = Color.parseColor("#8E0000"); borderColor = Color.parseColor("#FFCDD2") }
            Variant.GRAY -> { topColor = Color.parseColor("#CFD8DC"); bottomColor = Color.parseColor("#78909C"); shadowColor = Color.parseColor("#37474F"); borderColor = Color.parseColor("#ECEFF1") }
            Variant.GLASS -> { topColor = Color.argb(220, 255, 170, 40); bottomColor = Color.argb(240, 235, 100, 0); shadowColor = Color.argb(255, 120, 40, 0); borderColor = Color.parseColor("#FFE599") }
        }
        borderPaint.color = borderColor
        shadowPaint.color = shadowColor
        bodyPaint.shader = LinearGradient(
            buttonRect.left, buttonRect.top, buttonRect.left, buttonRect.bottom,
            topColor, bottomColor, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, bodyPaint)

        highlightRect.set(
            buttonRect.left + 8f,
            buttonRect.top + 3f,
            buttonRect.right - 8f,
            buttonRect.top + buttonRect.height() * 0.38f
        )
        canvas.drawRoundRect(highlightRect, cornerRadius * 0.8f, cornerRadius * 0.8f, highlightPaint)
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, borderPaint)

        val density = resources.displayMetrics.density
        val btnH = buttonRect.height()
        val hasSubText = subText.isNotBlank()

        val titleSizeSp = when {
            btnH >= 54f * density -> (btnH * 0.34f / density).coerceIn(18f, 22f)
            btnH >= 42f * density -> (btnH * 0.38f / density).coerceIn(15f, 18f)
            else -> (btnH * 0.42f / density).coerceIn(13f, 16f)
        }
        val titleSize = titleSizeSp * density
        titlePaint.textSize = titleSize
        titleShadowPaint.textSize = titleSize

        val resolvedIconType = if (iconType != IconType.AUTO) iconType else when {
            mainText.contains("PLAY") || mainText.contains("NEXT") || mainText.contains("RESUME") || mainText.contains("RETRY") -> IconType.PLAY
            mainText.contains("RESTART") -> IconType.RESTART
            mainText.contains("HOME") -> IconType.HOME
            mainText.contains("SETTINGS") -> IconType.SETTINGS
            else -> IconType.NONE
        }

        val iconSizeDp = when {
            btnH >= 54f * density -> (btnH * 0.38f / density).coerceIn(20f, 26f)
            else -> (btnH * 0.42f / density).coerceIn(16f, 22f)
        }
        val iconSize = if (resolvedIconType != IconType.NONE) iconSizeDp * density else 0f

        val subSizeSp = (btnH * 0.20f / density).coerceIn(10f, 13f)
        val subSize = subSizeSp * density
        subTitlePaint.textSize = subSize

        val titleWidth = titlePaint.measureText(mainText)
        val groupGap = if (resolvedIconType != IconType.NONE) 8f * density else 0f
        val groupWidth = iconSize + groupGap + titleWidth
        val groupLeft = buttonRect.centerX() - groupWidth / 2f

        val mainY = if (hasSubText) buttonRect.centerY() - (subSize * 0.25f) else buttonRect.centerY() + titleSize * 0.35f
        val iconCenterX = groupLeft + iconSize / 2f
        val iconCenterY = mainY - (titleSize * 0.32f)

        if (resolvedIconType != IconType.NONE) {
            drawCustomIcon(canvas, resolvedIconType, iconCenterX, iconCenterY, iconSize)
        }

        val titleX = if (resolvedIconType != IconType.NONE) groupLeft + iconSize + groupGap else buttonRect.centerX()
        titlePaint.textAlign = if (resolvedIconType != IconType.NONE) Paint.Align.LEFT else Paint.Align.CENTER
        titleShadowPaint.textAlign = titlePaint.textAlign

        canvas.drawText(mainText, titleX, mainY + 2.5f, titleShadowPaint)
        canvas.drawText(mainText, titleX, mainY, titlePaint)

        if (hasSubText) {
            val subY = buttonRect.centerY() + (subSize * 0.95f) + (3f * density)
            canvas.drawText(subText, buttonRect.centerX(), subY, subTitlePaint)
        }
    }

    private fun drawCustomIcon(canvas: Canvas, type: IconType, cx: Float, cy: Float, size: Float) {
        val r = size / 2f
        iconPath.reset()

        when (type) {
            IconType.PLAY -> {
                iconPath.moveTo(cx - r * 0.5f, cy - r * 0.7f)
                iconPath.lineTo(cx + r * 0.7f, cy)
                iconPath.lineTo(cx - r * 0.5f, cy + r * 0.7f)
                iconPath.close()
                canvas.drawPath(iconPath, iconPaint)
            }
            IconType.RESTART -> {
                val arcRect = RectF(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f)
                iconStrokePaint.strokeWidth = (r * 0.32f).coerceAtLeast(3f)
                canvas.drawArc(arcRect, 40f, 280f, false, iconStrokePaint)

                // Arrow head on arc top
                val arrowHead = Path().apply {
                    moveTo(cx + r * 0.3f, cy - r * 0.85f)
                    lineTo(cx + r * 0.9f, cy - r * 0.55f)
                    lineTo(cx + r * 0.85f, cy - r * 1.15f)
                    close()
                }
                canvas.drawPath(arrowHead, iconPaint)
            }
            IconType.HOME -> {
                // Roof
                iconPath.moveTo(cx, cy - r * 0.85f)
                iconPath.lineTo(cx + r * 0.85f, cy - r * 0.15f)
                iconPath.lineTo(cx + r * 0.65f, cy - r * 0.15f)
                iconPath.lineTo(cx + r * 0.65f, cy + r * 0.75f)
                iconPath.lineTo(cx - r * 0.65f, cy + r * 0.75f)
                iconPath.lineTo(cx - r * 0.65f, cy - r * 0.15f)
                iconPath.lineTo(cx - r * 0.85f, cy - r * 0.15f)
                iconPath.close()
                canvas.drawPath(iconPath, iconPaint)

                // Door cut out
                val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1F3045")
                }
                canvas.drawRect(RectF(cx - r * 0.22f, cy + r * 0.25f, cx + r * 0.22f, cy + r * 0.75f), doorPaint)
            }
            IconType.SETTINGS -> {
                iconStrokePaint.strokeWidth = (r * 0.28f).coerceAtLeast(3f)
                canvas.drawCircle(cx, cy, r * 0.45f, iconStrokePaint)
                canvas.drawCircle(cx, cy, r * 0.2f, iconPaint)
            }
            else -> {}
        }
    }
}
