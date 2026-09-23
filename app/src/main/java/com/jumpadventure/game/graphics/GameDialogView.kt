package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class GameDialogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFDF7")
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2A38")
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F3045")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#50FFFFFF")
        style = Paint.Style.FILL
    }

    private val shadowRect = RectF()
    private val panelRect = RectF()
    private val highlightRect = RectF()

    val titleView: TextView
    val messageView: TextView
    val buttonContainer: LinearLayout

    init {
        setWillNotDraw(false)
        val density = resources.displayMetrics.density
        val padding = (24 * density).toInt()
        setPadding(padding, padding, padding, padding + (8 * density).toInt())

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        titleView = TextView(context).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF9F1C"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (8 * density).toInt()) }
        }

        messageView = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1F3045"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (16 * density).toInt()) }
        }

        buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        contentLayout.addView(titleView)
        contentLayout.addView(messageView)
        contentLayout.addView(buttonContainer)
        addView(contentLayout)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cornerRadius = 36f
        val shadowDepth = 12f

        shadowRect.set(0f, shadowDepth, w, h)
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        panelRect.set(0f, 0f, w, h - shadowDepth)
        canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, panelPaint)

        highlightRect.set(8f, 6f, w - 8f, 24f)
        canvas.drawRoundRect(highlightRect, 12f, 12f, highlightPaint)

        canvas.drawRoundRect(panelRect, cornerRadius, cornerRadius, borderPaint)
        super.onDraw(canvas)
    }
}
