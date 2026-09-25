package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class NavTab(val id: String, val title: String) {
        SHOP("SHOP", "Shop"),
        HEROES("CHARACTERS", "Heroes"),
        WORLDS("WORLDS", "Worlds"),
        TROPHIES("ACHIEVEMENTS", "Trophies")
    }

    var selectedTabId: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var onTabSelectedListener: ((tabId: String) -> Unit)? = null

    private var pressedTabIndex = -1
    private var animSelectedScale = 1.0f

    fun animateSelectedTab() {
        animSelectedScale = 0.85f
        invalidate()
        animate().cancel()
        val animator = android.animation.ValueAnimator.ofFloat(0.85f, 1.16f, 1.0f).apply {
            duration = 260
            interpolator = android.view.animation.OvershootInterpolator(2.0f)
            addUpdateListener { va ->
                animSelectedScale = va.animatedValue as Float
                invalidate()
            }
        }
        animator.start()
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val shadowRect = RectF()
    private val buttonRect = RectF()
    private val highlightRect = RectF()
    private val iconPath = Path()

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredHeight = (72 * density).toInt()
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val finalH = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(widthSize, finalH)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        if (w <= 0f) return super.onTouchEvent(event)

        val tabs = NavTab.values()
        val density = resources.displayMetrics.density
        val gap = 8f * density
        val horizontalPad = 2f * density
        val itemW = (w - horizontalPad * 2f - gap * (tabs.size - 1)) / tabs.size
        val x = event.x

        var index = -1
        for (i in tabs.indices) {
            val left = horizontalPad + i * (itemW + gap)
            val right = left + itemW
            if (x >= left - gap / 2f && x <= right + gap / 2f) {
                index = i
                break
            }
        }
        if (index < 0) index = 0

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedTabIndex = index
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressedTabIndex != index) {
                    pressedTabIndex = index
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pressedTabIndex == index) {
                    val clickedTab = tabs[index]
                    selectedTabId = clickedTab.id
                    animateSelectedTab()
                    onTabSelectedListener?.invoke(clickedTab.id)
                    performClick()
                }
                pressedTabIndex = -1
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedTabIndex = -1
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
        val tabs = NavTab.values()
        val gap = 8f * density
        val horizontalPad = 2f * density
        val itemW = (w - horizontalPad * 2f - gap * (tabs.size - 1)) / tabs.size
        val itemH = h - 8f * density
        val cornerR = 18f * density

        tabs.forEachIndexed { i, tab ->
            val left = horizontalPad + i * (itemW + gap)
            val top = 2f * density
            val isSelected = selectedTabId == tab.id || (selectedTabId.isEmpty() && i == 0)
            val isPressed = i == pressedTabIndex

            val scale = if (isPressed) 0.92f else if (isSelected) (1.0f * animSelectedScale) else 1f
            val cx = left + itemW / 2f
            val cy = top + itemH / 2f

            canvas.save()
            canvas.scale(scale, scale, cx, cy)

            // Bottom Extrusion Shadow
            val shadowHeight = if (isSelected) 6f * density else 4f * density
            shadowRect.set(left, top + shadowHeight, left + itemW, top + itemH)

            // Distinct semantic colors for each button:
            // SHOP = Gold/Amber, HEROES = Blue/Cyan, WORLDS = Green/Teal, TROPHIES = Purple/Gold
            val (topColor, botColor, shadowColor, borderColor) = when (tab) {
                NavTab.SHOP -> if (isSelected)
                    listOf("#FFD43B", "#FF9F1C", "#B35C00", "#FFE885")
                else
                    listOf("#3D2D10", "#241804", "#120B00", "#664B1A")

                NavTab.HEROES -> if (isSelected)
                    listOf("#38BDF8", "#0284C7", "#0369A1", "#BAE6FD")
                else
                    listOf("#0F2D40", "#081B28", "#030C12", "#1D4A68")

                NavTab.WORLDS -> if (isSelected)
                    listOf("#34D399", "#059669", "#047857", "#A7F3D0")
                else
                    listOf("#0C3625", "#062016", "#02100A", "#175B3F")

                NavTab.TROPHIES -> if (isSelected)
                    listOf("#C084FC", "#7E22CE", "#581C87", "#F3E8FF")
                else
                    listOf("#301742", "#1C0A28", "#0D0314", "#52286E")
            }

            shadowPaint.color = Color.parseColor(shadowColor)
            canvas.drawRoundRect(shadowRect, cornerR, cornerR, shadowPaint)

            buttonRect.set(left, top, left + itemW, top + itemH - shadowHeight)
            bgPaint.shader = LinearGradient(
                buttonRect.left, buttonRect.top, buttonRect.left, buttonRect.bottom,
                Color.parseColor(topColor), Color.parseColor(botColor),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(buttonRect, cornerR, cornerR, bgPaint)

            // Top Gloss Highlight
            highlightRect.set(
                buttonRect.left + 4f * density,
                buttonRect.top + 2f * density,
                buttonRect.right - 4f * density,
                buttonRect.top + buttonRect.height() * 0.35f
            )
            canvas.drawRoundRect(highlightRect, cornerR * 0.7f, cornerR * 0.7f, highlightPaint)

            // Border
            borderPaint.color = Color.parseColor(borderColor)
            borderPaint.strokeWidth = if (isSelected) 2.5f * density else 1.5f * density
            canvas.drawRoundRect(buttonRect, cornerR, cornerR, borderPaint)

            // Icon
            val iconY = buttonRect.top + buttonRect.height() * 0.38f
            draw3DNavIcon(canvas, tab, cx, iconY, 24f * density, isSelected)

            // Label
            labelPaint.textSize = (if (isSelected) 12f else 11f) * density
            labelPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#94A3B8")
            val labelY = buttonRect.bottom - 6f * density
            canvas.drawText(tab.title, cx, labelY, labelPaint)

            canvas.restore()
        }
        bgPaint.shader = null
    }

    private fun draw3DNavIcon(
        canvas: Canvas,
        tab: NavTab,
        cx: Float,
        cy: Float,
        size: Float,
        isSelected: Boolean
    ) {
        val r = size / 2f
        iconPath.reset()

        when (tab) {
            NavTab.SHOP -> {
                // 3D Treasure Chest / Bag
                val topColor = if (isSelected) Color.WHITE else Color.parseColor("#FFD43B")
                val botColor = if (isSelected) Color.parseColor("#FFE599") else Color.parseColor("#FF9F1C")

                iconPaint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, topColor, botColor, Shader.TileMode.CLAMP)

                // Chest Body
                val chestRect = RectF(cx - r * 0.85f, cy - r * 0.2f, cx + r * 0.85f, cy + r * 0.8f)
                canvas.drawRoundRect(chestRect, 6f, 6f, iconPaint)

                // Lid
                val lidRect = RectF(cx - r * 0.95f, cy - r * 0.7f, cx + r * 0.95f, cy - r * 0.1f)
                canvas.drawRoundRect(lidRect, 8f, 8f, iconPaint)

                // Lock clasp
                val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1F3045") }
                canvas.drawCircle(cx, cy - r * 0.15f, r * 0.2f, lockPaint)
            }
            NavTab.HEROES -> {
                // 3D Hero Avatar / Helmet
                val topColor = if (isSelected) Color.WHITE else Color.parseColor("#7DD3FC")
                val botColor = if (isSelected) Color.parseColor("#BAE6FD") else Color.parseColor("#0284C7")

                iconPaint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, topColor, botColor, Shader.TileMode.CLAMP)

                // Head
                canvas.drawCircle(cx, cy - r * 0.2f, r * 0.65f, iconPaint)

                // Shoulders
                val shoulderRect = RectF(cx - r * 0.85f, cy + r * 0.2f, cx + r * 0.85f, cy + r * 0.85f)
                canvas.drawRoundRect(shoulderRect, 10f, 10f, iconPaint)
            }
            NavTab.WORLDS -> {
                // 3D Globe / Map
                val topColor = if (isSelected) Color.WHITE else Color.parseColor("#6EE7B7")
                val botColor = if (isSelected) Color.parseColor("#A7F3D0") else Color.parseColor("#059669")

                iconPaint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, topColor, botColor, Shader.TileMode.CLAMP)
                canvas.drawCircle(cx, cy, r * 0.85f, iconPaint)

                // Ring / Grid lines
                iconStrokePaint.color = Color.parseColor("#1F3045")
                iconStrokePaint.strokeWidth = 3f
                canvas.drawCircle(cx, cy, r * 0.85f, iconStrokePaint)
                canvas.drawLine(cx - r * 0.85f, cy, cx + r * 0.85f, cy, iconStrokePaint)
                canvas.drawOval(RectF(cx - r * 0.4f, cy - r * 0.85f, cx + r * 0.4f, cy + r * 0.85f), iconStrokePaint)
            }
            NavTab.TROPHIES -> {
                // 3D Trophy Cup
                val topColor = if (isSelected) Color.WHITE else Color.parseColor("#FDE047")
                val botColor = if (isSelected) Color.parseColor("#FEF08A") else Color.parseColor("#EAB308")

                iconPaint.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, topColor, botColor, Shader.TileMode.CLAMP)

                // Cup Bowl
                val cupPath = Path().apply {
                    moveTo(cx - r * 0.75f, cy - r * 0.75f)
                    lineTo(cx + r * 0.75f, cy - r * 0.75f)
                    lineTo(cx + r * 0.5f, cy + r * 0.15f)
                    quadTo(cx, cy + r * 0.45f, cx - r * 0.5f, cy + r * 0.15f)
                    close()
                }
                canvas.drawPath(cupPath, iconPaint)

                // Base
                val baseRect = RectF(cx - r * 0.55f, cy + r * 0.55f, cx + r * 0.55f, cy + r * 0.8f)
                canvas.drawRoundRect(baseRect, 4f, 4f, iconPaint)
                canvas.drawRect(RectF(cx - r * 0.18f, cy + r * 0.35f, cx + r * 0.18f, cy + r * 0.6f), iconPaint)
            }
        }
        iconPaint.shader = null
    }
}
