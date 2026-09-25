package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GameDialogBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class BoardType { PAUSE, WINNER }

    var boardType: BoardType = BoardType.PAUSE
        set(value) {
            field = value
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val shadowRect = RectF()
    private val boardRect = RectF()
    private val highlightRect = RectF()
    private val starPath = Path()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        val cornerR = 32f * density
        val shadowDepth = 10f * density

        // 1. Draw 3D Extrusion Shadow
        shadowRect.set(2f * density, shadowDepth, w - 2f * density, h)
        shadowPaint.color = if (boardType == BoardType.PAUSE) Color.parseColor("#09101A") else Color.parseColor("#522800")
        canvas.drawRoundRect(shadowRect, cornerR, cornerR, shadowPaint)

        // 2. Draw Main Board Base
        boardRect.set(2f * density, 0f, w - 2f * density, h - shadowDepth)

        if (boardType == BoardType.PAUSE) {
            bgPaint.shader = LinearGradient(
                boardRect.left, boardRect.top, boardRect.left, boardRect.bottom,
                Color.parseColor("#1C2C42"), Color.parseColor("#0F1A28"),
                Shader.TileMode.CLAMP
            )
            borderPaint.color = Color.parseColor("#38BDF8")
            borderPaint.strokeWidth = 3.5f * density
        } else { // WINNER
            bgPaint.shader = LinearGradient(
                boardRect.left, boardRect.top, boardRect.left, boardRect.bottom,
                Color.parseColor("#26203D"), Color.parseColor("#120E22"),
                Shader.TileMode.CLAMP
            )
            borderPaint.color = Color.parseColor("#FFD43B")
            borderPaint.strokeWidth = 4.5f * density
        }

        canvas.drawRoundRect(boardRect, cornerR, cornerR, bgPaint)

        // 3. Draw Top Gloss Highlight
        highlightRect.set(
            boardRect.left + 8f * density,
            boardRect.top + 4f * density,
            boardRect.right - 8f * density,
            boardRect.top + boardRect.height() * 0.28f
        )
        canvas.drawRoundRect(highlightRect, cornerR * 0.7f, cornerR * 0.7f, highlightPaint)

        // 4. Draw Beveled Outer Border
        canvas.drawRoundRect(boardRect, cornerR, cornerR, borderPaint)

        // 5. Board Specific Decorative Accents
        if (boardType == BoardType.WINNER) {
            // Gold corner rivets
            accentPaint.color = Color.parseColor("#FFE885")
            val rivetOffset = 20f * density
            val rivetR = 4f * density
            canvas.drawCircle(boardRect.left + rivetOffset, boardRect.top + rivetOffset, rivetR, accentPaint)
            canvas.drawCircle(boardRect.right - rivetOffset, boardRect.top + rivetOffset, rivetR, accentPaint)
            canvas.drawCircle(boardRect.left + rivetOffset, boardRect.bottom - rivetOffset, rivetR, accentPaint)
            canvas.drawCircle(boardRect.right - rivetOffset, boardRect.bottom - rivetOffset, rivetR, accentPaint)
        } else {
            // Blue cyan inner accent line
            accentPaint.style = Paint.Style.STROKE
            accentPaint.color = Color.parseColor("#1538BDF8")
            accentPaint.strokeWidth = 2f * density
            val innerRect = RectF(
                boardRect.left + 8f * density,
                boardRect.top + 8f * density,
                boardRect.right - 8f * density,
                boardRect.bottom - 8f * density
            )
            canvas.drawRoundRect(innerRect, cornerR - 6f * density, cornerR - 6f * density, accentPaint)
            accentPaint.style = Paint.Style.FILL
        }

        bgPaint.shader = null
    }
}
