package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.jumpadventure.game.R

class GameCurrencyBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class CurrencyType { COIN, GEM }

    var type: CurrencyType = CurrencyType.COIN
        set(value) {
            field = value
            invalidate()
        }

    var amount: Int = 0
        set(value) {
            field = value
            formattedText = formatAmount(value)
            invalidate()
        }

    var showPlusButton: Boolean = true
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var onPlusClickListener: (() -> Unit)? = null

    private var formattedText = "0"
    private var isPlusPressed = false

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#20000000")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.parseColor("#1F3045")
    }

    private val plusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val plusBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val plusIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val pillRect = RectF()
    private val plusRect = RectF()
    private val plusShadowRect = RectF()

    private var coinDrawable: Drawable? = null
    private var gemDrawable: Drawable? = null

    init {
        isClickable = true
        isFocusable = true
        loadDrawables()
    }

    private fun loadDrawables() {
        runCatching {
            coinDrawable = ContextCompat.getDrawable(context, R.drawable.ic_coin)
            gemDrawable = ContextCompat.getDrawable(context, R.drawable.ic_gem)
        }
    }

    private fun formatAmount(valAmount: Int): String {
        return String.format("%,d", valAmount)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val hDp = 44f
        val h = (hDp * density).toInt()

        val textWidth = textPaint.run {
            textSize = 14f * density
            measureText(formattedText)
        }

        val paddingStart = 38f * density

        val calculatedW = if (showPlusButton) {
            val plusW = 32f * density
            val paddingBetween = 8f * density
            (paddingStart + textWidth + paddingBetween + plusW + 6f * density).toInt()
        } else {
            (paddingStart + textWidth + 14f * density).toInt()
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val finalW = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(calculatedW, widthSize)
            else -> calculatedW
        }

        val finalH = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(h, heightSize)
            else -> h
        }

        setMeasuredDimension(finalW, finalH)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!showPlusButton) {
            return super.onTouchEvent(event)
        }

        val x = event.x
        val y = event.y

        val inPlus = plusRect.contains(x, y) || x >= plusRect.left - 10f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (inPlus) {
                    isPlusPressed = true
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isPlusPressed && !inPlus) {
                    isPlusPressed = false
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isPlusPressed) {
                    isPlusPressed = false
                    invalidate()
                    onPlusClickListener?.invoke()
                    performClick()
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isPlusPressed = false
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val pillR = h * 0.48f

        // 1. Draw Pill Main Body
        val pillRight = if (showPlusButton) w - 12f * density else w - 2f * density
        pillRect.set(2f, 2f, pillRight, h - 2f)
        shadowPaint.color = Color.parseColor("#15000000")
        canvas.drawRoundRect(RectF(pillRect.left, pillRect.top + 3f, pillRect.right, pillRect.bottom + 3f), pillR, pillR, shadowPaint)

        bgPaint.shader = LinearGradient(
            pillRect.left, pillRect.top, pillRect.left, pillRect.bottom,
            Color.parseColor("#FFFDF7"), Color.parseColor("#F1F5F9"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(pillRect, pillR, pillR, bgPaint)

        borderPaint.color = if (type == CurrencyType.COIN) Color.parseColor("#FFD43B") else Color.parseColor("#C084FC")
        borderPaint.strokeWidth = 2.5f * density
        canvas.drawRoundRect(pillRect, pillR, pillR, borderPaint)

        // Pill top highlight
        val highlightRect = RectF(pillRect.left + pillR * 0.5f, pillRect.top + 2f, pillRect.right - pillR * 0.5f, pillRect.top + pillRect.height() * 0.35f)
        canvas.drawRoundRect(highlightRect, 8f, 8f, highlightPaint)

        // 2. Draw Currency Vector Icon on Left
        val iconSize = (h * 0.68f).coerceIn(24f * density, 30f * density)
        val iconX = pillRect.left + 5f * density
        val iconY = (h - iconSize) / 2f

        val iconDrawable = if (type == CurrencyType.COIN) coinDrawable else gemDrawable
        if (iconDrawable != null) {
            iconDrawable.setBounds(
                iconX.toInt(),
                iconY.toInt(),
                (iconX + iconSize).toInt(),
                (iconY + iconSize).toInt()
            )
            iconDrawable.draw(canvas)
        }

        // 3. Draw Number Typography
        val textSizeDp = 14f
        textPaint.textSize = textSizeDp * density
        val textX = iconX + iconSize + 6f * density
        val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(formattedText, textX, textY, textPaint)

        // 4. Draw Attached 3D "+" Action Button at Right End (only if showPlusButton is true)
        if (showPlusButton) {
            val plusSize = (h * 0.68f).coerceIn(26f * density, 30f * density)
            val plusX = w - plusSize - 2f * density
            val plusY = (h - plusSize) / 2f
            val plusPressOffset = if (isPlusPressed) 2f else 0f

            plusRect.set(plusX, plusY + plusPressOffset, plusX + plusSize, plusY + plusSize + plusPressOffset)
            plusShadowRect.set(plusX, plusY + 4f, plusX + plusSize, plusY + plusSize + 4f)

            val (topColor, bottomColor, shadowColor, borderColor) = if (type == CurrencyType.COIN) {
                listOf(
                    Color.parseColor("#FFD43B"),
                    Color.parseColor("#FF9F1C"),
                    Color.parseColor("#B35C00"),
                    Color.parseColor("#FFE885")
                )
            } else {
                listOf(
                    Color.parseColor("#E040FB"),
                    Color.parseColor("#9C27B0"),
                    Color.parseColor("#4A148C"),
                    Color.parseColor("#F38FFF")
                )
            }

            // Plus Shadow
            shadowPaint.color = shadowColor
            val plusR = plusSize * 0.38f
            canvas.drawRoundRect(plusShadowRect, plusR, plusR, shadowPaint)

            // Plus Body Gradient
            plusBgPaint.shader = LinearGradient(
                plusRect.left, plusRect.top, plusRect.left, plusRect.bottom,
                topColor, bottomColor, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(plusRect, plusR, plusR, plusBgPaint)

            // Plus Border
            plusBorderPaint.color = borderColor
            canvas.drawRoundRect(plusRect, plusR, plusR, plusBorderPaint)

            // Plus Symbol
            val cx = plusRect.centerX()
            val cy = plusRect.centerY()
            val arm = plusSize * 0.26f
            canvas.drawLine(cx - arm, cy, cx + arm, cy, plusIconPaint)
            canvas.drawLine(cx, cy - arm, cx, cy + arm, plusIconPaint)
        }
    }
}
