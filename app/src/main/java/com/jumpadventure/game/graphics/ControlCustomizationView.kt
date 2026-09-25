package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jumpadventure.game.model.GameSaveData

class ControlCustomizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ControlType(
        val key: String,
        val label: String,
        val topColorHex: String,
        val botColorHex: String
    ) {
        LEFT("LEFT", "LEFT", "#42D9FF", "#0879D7"),
        RIGHT("RIGHT", "RIGHT", "#42D9FF", "#0879D7"),
        JUMP("JUMP", "JUMP", "#4DE3B0", "#078D6D"),
        MAGNET("MAGNET", "MAGNET", "#38BDF8", "#0284C7"),
        SPEED("SPEED", "SPEED", "#FFD43B", "#FF9F1C"),
        SHIELD("SHIELD", "SHIELD", "#36C96F", "#059669")
    }

    class ControlItem(
        val type: ControlType,
        var rect: RectF = RectF(),
        var scale: Float = 1.0f,
        var baseWidth: Float = 0f,
        var baseHeight: Float = 0f
    )

    var items = mutableListOf<ControlItem>()
    var selectedControl: ControlItem? = null
        set(value) {
            field = value
            onSelectedControlChangedListener?.invoke(value)
            invalidate()
        }

    var onSelectedControlChangedListener: ((ControlItem?) -> Unit)? = null

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B00F172A")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(85, 0, 20, 50)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = Color.argb(230, 255, 255, 255)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#FFD43B")
    }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 255, 255, 255)
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        isClickable = true
        isFocusable = true
    }

    fun setupControls(data: GameSaveData, viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return

        val density = resources.displayMetrics.density
        val baseSide = (viewWidth * 0.16f).coerceIn(68f * density, 88f * density)
        val baseJump = (viewWidth * 0.20f).coerceIn(84f * density, 106f * density)
        val basePower = (54f * density).coerceIn(46f * density, 62f * density)

        val marginHoriz = 20f * density
        val marginBottom = 120f * density
        val gap = 16f * density

        // Default positions
        val defLeftX = marginHoriz
        val defLeftY = viewHeight - baseSide - marginBottom
        val defRightX = marginHoriz + baseSide + gap
        val defRightY = viewHeight - baseSide - marginBottom
        val defJumpX = viewWidth - marginHoriz - baseJump
        val defJumpY = viewHeight - baseJump - marginBottom

        val defMagnetX = viewWidth - marginHoriz - basePower
        val defMagnetY = defJumpY - basePower * 3.3f
        val defSpeedX = viewWidth - marginHoriz - basePower
        val defSpeedY = defJumpY - basePower * 2.2f
        val defShieldX = viewWidth - marginHoriz - basePower
        val defShieldY = defJumpY - basePower * 1.1f

        items.clear()

        // LEFT
        val lScale = data.leftScale.coerceIn(0.7f, 1.5f)
        val lW = baseSide * lScale
        val lH = baseSide * lScale
        val lx = if (data.leftX >= 0) data.leftX.coerceIn(0f, viewWidth - lW) else defLeftX
        val ly = if (data.leftY >= 0) data.leftY.coerceIn(0f, viewHeight - lH) else defLeftY
        items.add(ControlItem(ControlType.LEFT, RectF(lx, ly, lx + lW, ly + lH), lScale, baseSide, baseSide))

        // RIGHT
        val rScale = data.rightScale.coerceIn(0.7f, 1.5f)
        val rW = baseSide * rScale
        val rH = baseSide * rScale
        val rx = if (data.rightX >= 0) data.rightX.coerceIn(0f, viewWidth - rW) else defRightX
        val ry = if (data.rightY >= 0) data.rightY.coerceIn(0f, viewHeight - rH) else defRightY
        items.add(ControlItem(ControlType.RIGHT, RectF(rx, ry, rx + rW, ry + rH), rScale, baseSide, baseSide))

        // JUMP
        val jScale = data.jumpScale.coerceIn(0.7f, 1.5f)
        val jW = baseJump * jScale
        val jH = baseJump * jScale
        val jx = if (data.jumpX >= 0) data.jumpX.coerceIn(0f, viewWidth - jW) else defJumpX
        val jy = if (data.jumpY >= 0) data.jumpY.coerceIn(0f, viewHeight - jH) else defJumpY
        items.add(ControlItem(ControlType.JUMP, RectF(jx, jy, jx + jW, jy + jH), jScale, baseJump, baseJump))

        // MAGNET
        val mScale = data.magnetScale.coerceIn(0.7f, 1.5f)
        val mW = basePower * mScale
        val mH = basePower * mScale
        val mx = if (data.magnetX >= 0) data.magnetX.coerceIn(0f, viewWidth - mW) else defMagnetX
        val my = if (data.magnetY >= 0) data.magnetY.coerceIn(0f, viewHeight - mH) else defMagnetY
        items.add(ControlItem(ControlType.MAGNET, RectF(mx, my, mx + mW, my + mH), mScale, basePower, basePower))

        // SPEED
        val sScale = data.speedScale.coerceIn(0.7f, 1.5f)
        val sW = basePower * sScale
        val sH = basePower * sScale
        val sx = if (data.speedX >= 0) data.speedX.coerceIn(0f, viewWidth - sW) else defSpeedX
        val sy = if (data.speedY >= 0) data.speedY.coerceIn(0f, viewHeight - sH) else defSpeedY
        items.add(ControlItem(ControlType.SPEED, RectF(sx, sy, sx + sW, sy + sH), sScale, basePower, basePower))

        // SHIELD
        val shScale = data.shieldScale.coerceIn(0.7f, 1.5f)
        val shW = basePower * shScale
        val shH = basePower * shScale
        val shx = if (data.shieldX >= 0) data.shieldX.coerceIn(0f, viewWidth - shW) else defShieldX
        val shy = if (data.shieldY >= 0) data.shieldY.coerceIn(0f, viewHeight - shH) else defShieldY
        items.add(ControlItem(ControlType.SHIELD, RectF(shx, shy, shx + shW, shy + shH), shScale, basePower, basePower))

        selectedControl = items.firstOrNull()
        invalidate()
    }

    fun saveToSaveData(data: GameSaveData) {
        items.forEach { item ->
            when (item.type) {
                ControlType.LEFT -> { data.leftX = item.rect.left; data.leftY = item.rect.top; data.leftScale = item.scale }
                ControlType.RIGHT -> { data.rightX = item.rect.left; data.rightY = item.rect.top; data.rightScale = item.scale }
                ControlType.JUMP -> { data.jumpX = item.rect.left; data.jumpY = item.rect.top; data.jumpScale = item.scale }
                ControlType.MAGNET -> { data.magnetX = item.rect.left; data.magnetY = item.rect.top; data.magnetScale = item.scale }
                ControlType.SPEED -> { data.speedX = item.rect.left; data.speedY = item.rect.top; data.speedScale = item.scale }
                ControlType.SHIELD -> { data.shieldX = item.rect.left; data.shieldY = item.rect.top; data.shieldScale = item.scale }
            }
        }
        data.hasCustomControls = true
    }

    fun resetToDefault(data: GameSaveData) {
        data.hasCustomControls = false
        data.leftX = -1f; data.leftY = -1f; data.leftScale = 1.0f
        data.rightX = -1f; data.rightY = -1f; data.rightScale = 1.0f
        data.jumpX = -1f; data.jumpY = -1f; data.jumpScale = 1.0f
        data.magnetX = -1f; data.magnetY = -1f; data.magnetScale = 1.0f
        data.speedX = -1f; data.speedY = -1f; data.speedScale = 1.0f
        data.shieldX = -1f; data.shieldY = -1f; data.shieldScale = 1.0f
        setupControls(data, width, height)
    }

    fun updateSelectedScale(delta: Float) {
        val ctrl = selectedControl ?: return
        val newScale = (ctrl.scale + delta).coerceIn(0.7f, 1.5f)
        ctrl.scale = newScale

        val cx = ctrl.rect.centerX()
        val cy = ctrl.rect.centerY()
        val newW = ctrl.baseWidth * newScale
        val newH = ctrl.baseHeight * newScale

        val left = (cx - newW / 2f).coerceIn(0f, (width - newW).coerceAtLeast(0f))
        val top = (cy - newH / 2f).coerceIn(0f, (height - newH).coerceAtLeast(0f))
        ctrl.rect.set(left, top, left + newW, top + newH)

        onSelectedControlChangedListener?.invoke(ctrl)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                activePointerId = event.getPointerId(0)

                val hit = items.findLast { it.rect.contains(x, y) }
                if (hit != null) {
                    selectedControl = hit
                    dragOffsetX = x - hit.rect.left
                    dragOffsetY = y - hit.rect.top
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex != -1 && selectedControl != null) {
                    val x = event.getX(pointerIndex)
                    val y = event.getPointerId(pointerIndex).let { event.getY(pointerIndex) }

                    val ctrl = selectedControl!!
                    val w = ctrl.rect.width()
                    val h = ctrl.rect.height()

                    val newLeft = (x - dragOffsetX).coerceIn(0f, (width - w).coerceAtLeast(0f))
                    val newTop = (y - dragOffsetY).coerceIn(0f, (height - h).coerceAtLeast(0f))

                    ctrl.rect.set(newLeft, newTop, newLeft + w, newTop + h)
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (items.isEmpty()) {
            setupControls(GameSaveData(), w.toInt(), h.toInt())
        }

        // Draw Translucent Overlay Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Draw Control Items
        items.forEach { item ->
            val isSelected = (item == selectedControl)
            drawControlItem(canvas, item, isSelected)
        }
    }

    private fun drawControlItem(canvas: Canvas, item: ControlItem, isSelected: Boolean) {
        val rect = item.rect
        val topColor = Color.parseColor(item.type.topColorHex)
        val botColor = Color.parseColor(item.type.botColorHex)

        val radius = minOf(rect.width(), rect.height()) * 0.5f

        // Shadow
        canvas.drawCircle(rect.centerX(), rect.centerY() + 6f, radius - 1f, shadowPaint)

        // Glass Gradient Face
        glassPaint.shader = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            topColor, botColor, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius - 2f, glassPaint)
        glassPaint.shader = null

        // Glossy Highlight
        canvas.drawOval(
            RectF(rect.centerX() - radius * 0.5f, rect.centerY() - radius * 0.75f, rect.centerX() + radius * 0.2f, rect.centerY() - radius * 0.35f),
            highlightPaint
        )

        // Border
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius - 2f, borderPaint)

        // Selected Glowing Outline
        if (isSelected) {
            val glowPadding = 6f
            canvas.drawCircle(rect.centerX(), rect.centerY(), radius + glowPadding, glowPaint)
        }

        // Icon / Label
        when (item.type) {
            ControlType.LEFT -> {
                val path = Path().apply {
                    val cx = rect.centerX()
                    val cy = rect.centerY()
                    val s = rect.width() * 0.22f
                    moveTo(cx + s * 0.8f, cy - s)
                    lineTo(cx - s * 0.9f, cy)
                    lineTo(cx + s * 0.8f, cy + s)
                    close()
                }
                canvas.drawPath(path, iconPaint)
            }
            ControlType.RIGHT -> {
                val path = Path().apply {
                    val cx = rect.centerX()
                    val cy = rect.centerY()
                    val s = rect.width() * 0.22f
                    moveTo(cx - s * 0.8f, cy - s)
                    lineTo(cx + s * 0.9f, cy)
                    lineTo(cx - s * 0.8f, cy + s)
                    close()
                }
                canvas.drawPath(path, iconPaint)
            }
            ControlType.JUMP -> {
                textPaint.textSize = minOf(rect.width() * 0.22f, 26f)
                canvas.drawText("JUMP", rect.centerX(), rect.centerY() + textPaint.textSize * 0.34f, textPaint)
            }
            ControlType.MAGNET -> {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = rect.width() * 0.12f
                }
                val mcx = rect.centerX()
                val mcy = rect.centerY()
                val ms = rect.width() * 0.26f
                canvas.drawArc(RectF(mcx - ms, mcy - ms, mcx + ms, mcy + ms * 0.5f), 180f, 180f, false, strokePaint)
                canvas.drawLine(mcx - ms, mcy, mcx - ms, mcy + ms * 0.5f, strokePaint)
                canvas.drawLine(mcx + ms, mcy, mcx + ms, mcy + ms * 0.5f, strokePaint)
            }
            ControlType.SPEED -> {
                val path = Path().apply {
                    val scx = rect.centerX()
                    val scy = rect.centerY()
                    val ss = rect.width() * 0.28f
                    moveTo(scx - ss * 0.4f, scy + ss * 0.6f)
                    lineTo(scx + ss * 0.1f, scy + ss * 0.05f)
                    lineTo(scx - ss * 0.1f, scy + ss * 0.05f)
                    lineTo(scx + ss * 0.4f, scy - ss * 0.6f)
                    lineTo(scx - ss * 0.1f, scy - ss * 0.05f)
                    lineTo(scx + ss * 0.1f, scy - ss * 0.05f)
                    close()
                }
                canvas.drawPath(path, iconPaint)
            }
            ControlType.SHIELD -> {
                val path = Path().apply {
                    val shcx = rect.centerX()
                    val shcy = rect.centerY()
                    val shs = rect.width() * 0.28f
                    moveTo(shcx, shcy - shs * 0.65f)
                    lineTo(shcx + shs * 0.55f, shcy - shs * 0.35f)
                    lineTo(shcx + shs * 0.45f, shcy + shs * 0.35f)
                    quadTo(shcx, shcy + shs * 0.7f, shcx - shs * 0.45f, shcy + shs * 0.35f)
                    lineTo(shcx - shs * 0.55f, shcy - shs * 0.35f)
                    close()
                }
                canvas.drawPath(path, iconPaint)
            }
        }
    }
}
