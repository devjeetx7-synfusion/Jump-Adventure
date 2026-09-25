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
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#30000000") }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#33FFFFFF")
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#25FFFFFF") }

    private val activePillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val activePillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#80FFFFFF")
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

    private val containerRect = RectF()
    private val shadowRect = RectF()
    private val pillRect = RectF()
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

        val itemW = w / NavTab.values().size
        val x = event.x
        val index = (x / itemW).toInt().coerceIn(0, NavTab.values().size - 1)

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
                    val clickedTab = NavTab.values()[index]
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
        val cornerR = 24f * density

        // 1. Panel Container Shadow & Body
        shadowRect.set(0f, 6f * density, w, h)
        canvas.drawRoundRect(shadowRect, cornerR, cornerR, shadowPaint)

        containerRect.set(0f, 0f, w, h - 4f * density)
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, containerRect.bottom,
            Color.parseColor("#1F3045"), Color.parseColor("#0F172A"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(containerRect, cornerR, cornerR, bgPaint)
        canvas.drawRoundRect(containerRect, cornerR, cornerR, borderPaint)

        // Top glossy edge
        val glossyRect = RectF(cornerR * 0.5f, 3f * density, w - cornerR * 0.5f, 10f * density)
        canvas.drawRoundRect(glossyRect, 10f, 10f, highlightPaint)

        // 2. Navigation Items
        val tabs = NavTab.values()
        val itemW = w / tabs.size

        tabs.forEachIndexed { i, tab ->
            val itemLeft = i * itemW
            val itemCenterX = itemLeft + itemW / 2f
            val isSelected = (selectedTabId == tab.id) || (selectedTabId.isEmpty() && i == 2) // Default active indication if blank
            val isPressed = (i == pressedTabIndex)

            // Draw Active Pill Capsule background
            if (isSelected) {
                val pillW = itemW - 12f * density
                val pillH = h - 16f * density
                val pillLeft = itemCenterX - pillW / 2f
                val pillTop = 6f * density
                pillRect.set(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH)

                val (topC, botC) = when (tab) {
                    NavTab.SHOP -> Pair("#FF9F1C", "#D97706")
                    NavTab.HEROES -> Pair("#38BDF8", "#0284C7")
                    NavTab.WORLDS -> Pair("#36C96F", "#059669")
                    NavTab.TROPHIES -> Pair("#AB47BC", "#7E22CE")
                }

                activePillBgPaint.shader = LinearGradient(
                    pillRect.left, pillRect.top, pillRect.left, pillRect.bottom,
                    Color.parseColor(topC), Color.parseColor(botC),
                    Shader.TileMode.CLAMP
                )
                val pillCorner = 16f * density
                canvas.drawRoundRect(pillRect, pillCorner, pillCorner, activePillBgPaint)
                canvas.drawRoundRect(pillRect, pillCorner, pillCorner, activePillBorderPaint)
            }

            // Draw Icon
            canvas.save()
            val scale = if (isPressed) 0.90f else if (isSelected) (1.08f * animSelectedScale) else 1.0f
            val iconCenterY = (h * 0.36f) - (if (isSelected) 3f * density else 0f)
            canvas.scale(scale, scale, itemCenterX, iconCenterY)

            draw3DNavIcon(canvas, tab, itemCenterX, iconCenterY, 26f * density, isSelected)
            canvas.restore()

            // Draw Label
            labelPaint.textSize = (if (isSelected) 12f else 11f) * density
            labelPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#94A3B8")
            val labelY = h - 10f * density
            canvas.drawText(tab.title, itemCenterX, labelY, labelPaint)
        }
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
