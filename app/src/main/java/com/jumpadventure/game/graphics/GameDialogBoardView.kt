package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

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
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val shadowRect = RectF()
    private val boardRect = RectF()
    private val innerRect = RectF()
    private val highlightRect = RectF()
    private val accentPath = Path()

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode != MeasureSpec.EXACTLY) {
            // Prevent MATCH_PARENT child from expanding WRAP_CONTENT parent during AT_MOST pass.
            setMeasuredDimension(widthSize, 0)
        } else {
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension(widthSize, heightSize)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val d = resources.displayMetrics.density
        val margin = 3f * d
        val shadowDepth = min(12f * d, h * 0.055f)
        val corner = min(30f * d, w * 0.085f)

        boardRect.set(margin, 0f, w - margin, h - shadowDepth)
        shadowRect.set(margin + 1f * d, shadowDepth, w - margin - 1f * d, h)

        // Deep 3D extrusion.
        shadowPaint.shader = LinearGradient(
            0f, shadowRect.top, 0f, shadowRect.bottom,
            if (boardType == BoardType.PAUSE) Color.parseColor("#07111E")
            else Color.parseColor("#5C2B00"),
            if (boardType == BoardType.PAUSE) Color.parseColor("#02070D")
            else Color.parseColor("#2A1000"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(shadowRect, corner, corner, shadowPaint)

        // Main board.
        bgPaint.shader = if (boardType == BoardType.PAUSE) {
            LinearGradient(
                0f, boardRect.top, 0f, boardRect.bottom,
                Color.parseColor("#233B57"),
                Color.parseColor("#101D2B"),
                Shader.TileMode.CLAMP
            )
        } else {
            LinearGradient(
                0f, boardRect.top, 0f, boardRect.bottom,
                Color.parseColor("#3B315B"),
                Color.parseColor("#171126"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(boardRect, corner, corner, bgPaint)

        // Inner bevel.
        innerRect.set(
            boardRect.left + 8f * d,
            boardRect.top + 8f * d,
            boardRect.right - 8f * d,
            boardRect.bottom - 8f * d
        )
        innerPaint.style = Paint.Style.STROKE
        innerPaint.strokeWidth = 2f * d
        innerPaint.color = if (boardType == BoardType.PAUSE) {
            Color.parseColor("#204A6B")
        } else {
            Color.parseColor("#6D4708")
        }
        canvas.drawRoundRect(innerRect, corner - 6f * d, corner - 6f * d, innerPaint)

        // Soft top gloss, restrained so content stays readable.
        highlightPaint.shader = LinearGradient(
            0f, boardRect.top, 0f, boardRect.top + boardRect.height() * 0.22f,
            Color.argb(78, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        highlightRect.set(
            boardRect.left + 8f * d,
            boardRect.top + 4f * d,
            boardRect.right - 8f * d,
            boardRect.top + boardRect.height() * 0.28f
        )
        canvas.drawRoundRect(highlightRect, corner * 0.72f, corner * 0.72f, highlightPaint)

        // Outer bright rim / depth line.
        borderPaint.strokeWidth = if (boardType == BoardType.PAUSE) 3.4f * d else 4.2f * d
        borderPaint.color = if (boardType == BoardType.PAUSE) Color.parseColor("#20C9FF")
                            else Color.parseColor("#FFC928")
        canvas.drawRoundRect(boardRect, corner, corner, borderPaint)

        if (boardType == BoardType.PAUSE) {
            // Simple cyan corner/side accents.
            accentPaint.style = Paint.Style.STROKE
            accentPaint.strokeWidth = 2.2f * d
            accentPaint.color = Color.parseColor("#27445F")
            val ir = RectF(
                boardRect.left + 14f * d,
                boardRect.top + 14f * d,
                boardRect.right - 14f * d,
                boardRect.bottom - 14f * d
            )
            canvas.drawRoundRect(ir, corner - 10f * d, corner - 10f * d, accentPaint)
        } else {
            // Winner: larger center glow behind stars + golden corner bolts.
            val glow = RadialGradient(
                boardRect.centerX(),
                boardRect.top + boardRect.height() * 0.27f,
                maxOf(30f * d, boardRect.width() * 0.62f),
                intArrayOf(
                    Color.argb(72, 255, 220, 70),
                    Color.argb(25, 255, 180, 20),
                    Color.argb(0, 255, 180, 20)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            accentPaint.shader = glow
            canvas.drawOval(
                RectF(
                    boardRect.left + 14f * d,
                    boardRect.top + 24f * d,
                    boardRect.right - 14f * d,
                    boardRect.top + boardRect.height() * 0.50f
                ),
                accentPaint
            )
            accentPaint.shader = null

            val boltR = 4.5f * d
            sparklePaint.color = Color.parseColor("#FFE98A")
            for ((x, y) in arrayOf(
                boardRect.left + 18f * d to boardRect.top + 18f * d,
                boardRect.right - 18f * d to boardRect.top + 18f * d,
                boardRect.left + 18f * d to boardRect.bottom - 18f * d,
                boardRect.right - 18f * d to boardRect.bottom - 18f * d
            )) {
                canvas.drawCircle(x, y, boltR, sparklePaint)
                canvas.drawCircle(x, y, boltR * 0.35f, whiteDotPaint)
            }
        }

        bgPaint.shader = null
        shadowPaint.shader = null
        innerPaint.shader = null
        highlightPaint.shader = null
    }

    private val whiteDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 180 }
}
