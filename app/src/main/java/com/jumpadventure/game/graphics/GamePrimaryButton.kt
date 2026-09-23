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
        set(value) {
            field = value
            invalidate()
        }

    var subText: String = "LEVEL 1"
        set(value) {
            field = value
            invalidate()
        }

    private var isPressedState = false

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A34A00")
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#FFE599")
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#50FFFFFF")
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#1F3045")
        textAlign = Paint.Align.LEFT
    }

    private val titleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#FFCA28")
        textAlign = Paint.Align.LEFT
    }

    private val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#4A2800")
        textAlign = Paint.Align.LEFT
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
        val desiredWidth = (310 * density).toInt()
        val desiredHeight = (76 * density).toInt()

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val w = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> Math.min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val h = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> Math.min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressedState = true
                animate().scaleX(0.96f).scaleY(0.96f).setDuration(60).start()
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isPressedState = false
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(60).start()
                invalidate()
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressedState = false
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(60).start()
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val shadowOffset = if (isPressedState) 3f else 10f
        val cornerRadius = h * 0.38f

        // 1. Dark Orange 3D Drop Shadow
        shadowRect.set(4f, shadowOffset + 4f, w - 4f, h)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        // 2. Main Glossy Orange/Gold Body
        buttonRect.set(4f, 2f, w - 4f, h - shadowOffset)
        bodyPaint.shader = LinearGradient(
            buttonRect.left, buttonRect.top, buttonRect.left, buttonRect.bottom,
            Color.parseColor("#FFC107"), Color.parseColor("#FF8F00"), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, bodyPaint)

        // 3. Top Inner Glossy Highlight
        highlightRect.set(buttonRect.left + 8f, buttonRect.top + 4f, buttonRect.right - 8f, buttonRect.top + buttonRect.height() * 0.42f)
        canvas.drawRoundRect(highlightRect, cornerRadius * 0.8f, cornerRadius * 0.8f, highlightPaint)

        // 4. Bevel Outer Border
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, borderPaint)

        val titleSize = buttonRect.height() * 0.36f
        titlePaint.textSize = titleSize
        titleShadowPaint.textSize = titleSize

        if (subText.isEmpty()) {
            // Center single main text horizontally and vertically
            val oldAlign = titlePaint.textAlign
            titlePaint.textAlign = Paint.Align.CENTER
            titleShadowPaint.textAlign = Paint.Align.CENTER

            val centerX = buttonRect.centerX()
            val titleY = buttonRect.centerY() + titleSize * 0.35f

            canvas.drawText(mainText, centerX, titleY + 2f, titleShadowPaint)
            canvas.drawText(mainText, centerX, titleY, titlePaint)

            titlePaint.textAlign = oldAlign
            titleShadowPaint.textAlign = oldAlign
        } else {
            // 5. Draw Play Triangle Icon
            val iconSize = buttonRect.height() * 0.42f
            val iconX = buttonRect.left + buttonRect.height() * 0.55f
            val iconY = buttonRect.centerY()

            playIconPath.reset()
            playIconPath.moveTo(iconX - iconSize * 0.35f, iconY - iconSize * 0.5f)
            playIconPath.lineTo(iconX + iconSize * 0.5f, iconY)
            playIconPath.lineTo(iconX - iconSize * 0.35f, iconY + iconSize * 0.5f)
            playIconPath.close()

            canvas.drawPath(playIconPath, iconPaint)

            // 6. Draw Main Title "PLAY NOW" & Subtitle "LEVEL X"
            val textStartX = iconX + iconSize * 0.75f

            val subSize = buttonRect.height() * 0.22f
            subTitlePaint.textSize = subSize

            val titleY = buttonRect.centerY() - 2f
            val subY = buttonRect.centerY() + subSize + 4f

            canvas.drawText(mainText, textStartX, titleY + 2f, titleShadowPaint)
            canvas.drawText(mainText, textStartX, titleY, titlePaint)

            canvas.drawText(subText, textStartX, subY, subTitlePaint)
        }
    }
}
