package com.jumpadventure.game.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jumpadventure.game.engine.*
import kotlin.math.sin

class GameplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply { isAntiAlias = true }
    private val bgPaint = Paint().apply { isAntiAlias = true }

    var currentWorldTheme: WorldTheme = WorldTheme.GREEN_FOREST
    var player: Player = Player(100f, 300f)

    var platforms: MutableList<Platform> = mutableListOf()
    var coins: MutableList<Coin> = mutableListOf()
    var stars: MutableList<Star> = mutableListOf()
    var spikes: MutableList<Spike> = mutableListOf()
    var enemies: MutableList<Enemy> = mutableListOf()
    var checkpoints: MutableList<Checkpoint> = mutableListOf()
    var powerUps: MutableList<PowerUpItem> = mutableListOf()
    var finishFlag: FinishFlag? = null

    val physicsEngine = PhysicsEngine()

    var cameraX: Float = 0f
    var cameraY: Float = 0f
    var worldWidth: Float = 3000f
    var worldHeight: Float = 1200f

    var isPaused: Boolean = false
    private var lastFrameTimeNanos: Long = 0L

    private val btnLeftRect = RectF()
    private val btnRightRect = RectF()
    private val btnJumpRect = RectF()

    var moveLeftPressed: Boolean = false
    var moveRightPressed: Boolean = false

    init {
        isFocusable = true
    }

    fun loadLevelData(
        theme: WorldTheme,
        width: Float,
        height: Float,
        p: Player,
        plats: List<Platform>,
        cns: List<Coin>,
        strs: List<Star>,
        spks: List<Spike>,
        enms: List<Enemy>,
        cps: List<Checkpoint>,
        pups: List<PowerUpItem>,
        finish: FinishFlag
    ) {
        currentWorldTheme = theme
        worldWidth = width
        worldHeight = height
        player = p
        platforms = plats.toMutableList()
        coins = cns.toMutableList()
        stars = strs.toMutableList()
        spikes = spks.toMutableList()
        enemies = enms.toMutableList()
        checkpoints = cps.toMutableList()
        powerUps = pups.toMutableList()
        finishFlag = finish

        cameraX = 0f
        cameraY = 0f
        lastFrameTimeNanos = System.nanoTime()
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val btnWidth = w * 0.18f
        val btnHeight = btnWidth
        val bottomMargin = h * 0.05f

        btnLeftRect.set(20f, h - btnHeight - bottomMargin, 20f + btnWidth, h - bottomMargin)
        btnRightRect.set(40f + btnWidth, h - btnHeight - bottomMargin, 40f + btnWidth * 2f, h - bottomMargin)
        btnJumpRect.set(w - btnWidth - 30f, h - btnHeight - bottomMargin, w - 30f, h - bottomMargin)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.nanoTime()
        if (lastFrameTimeNanos == 0L) lastFrameTimeNanos = now
        val deltaTime = ((now - lastFrameTimeNanos) / 1000000000f).coerceAtMost(0.05f)
        lastFrameTimeNanos = now

        if (!isPaused) {
            if (moveLeftPressed) {
                player.vx = -player.baseSpeed
                player.isFacingRight = false
            } else if (moveRightPressed) {
                player.vx = player.baseSpeed
                player.isFacingRight = true
            } else if (player.isGrounded) {
                player.vx = 0f
            }

            physicsEngine.update(
                player, platforms, coins, stars, spikes, enemies,
                checkpoints, finishFlag, powerUps, deltaTime, worldHeight
            )

            val targetCamX = (player.x - width * 0.35f).coerceIn(0f, (worldWidth - width).coerceAtLeast(0f))
            val targetCamY = (player.y - height * 0.5f).coerceIn(-500f, (worldHeight - height).coerceAtLeast(0f))

            cameraX += (targetCamX - cameraX) * 0.1f
            cameraY += (targetCamY - cameraY) * 0.1f
        }

        val bgShader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            currentWorldTheme.skyTopColor, currentWorldTheme.skyBottomColor,
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = bgShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        renderBackgroundDecorations(canvas)

        platforms.forEach { it.render(canvas, paint, cameraX, cameraY) }
        spikes.forEach { it.render(canvas, paint, cameraX, cameraY) }
        coins.forEach { it.render(canvas, paint, cameraX, cameraY) }
        stars.forEach { it.render(canvas, paint, cameraX, cameraY) }
        powerUps.forEach { it.render(canvas, paint, cameraX, cameraY) }
        checkpoints.forEach { it.render(canvas, paint, cameraX, cameraY) }
        enemies.forEach { it.render(canvas, paint, cameraX, cameraY) }
        finishFlag?.render(canvas, paint, cameraX, cameraY)
        player.render(canvas, paint, cameraX, cameraY)

        renderTouchControls(canvas)

        if (!isPaused) {
            postInvalidateOnAnimation()
        }
    }

    private fun renderBackgroundDecorations(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        when (currentWorldTheme.bgDecorType) {
            "TREES" -> {
                paint.color = 0xFF388E3C.toInt()
                for (i in 0..5) {
                    val x = (i * 400f - cameraX * 0.3f) % (width + 400f) - 100f
                    canvas.drawCircle(x, height * 0.8f, 120f, paint)
                }
            }
            "PYRAMIDS" -> {
                paint.color = 0xFFE65100.toInt()
                for (i in 0..3) {
                    val x = (i * 600f - cameraX * 0.2f) % (width + 600f) - 200f
                    val path = Path().apply {
                        moveTo(x, height * 0.85f)
                        lineTo(x + 150f, height * 0.5f)
                        lineTo(x + 300f, height * 0.85f)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
            "CLOUDS" -> {
                paint.color = 0xB3FFFFFF.toInt()
                for (i in 0..4) {
                    val x = (i * 350f - cameraX * 0.15f) % (width + 350f) - 100f
                    val y = height * 0.25f + sin(i.toDouble()) * 40f
                    canvas.drawCircle(x, y.toFloat(), 60f, paint)
                    canvas.drawCircle(x + 50f, y.toFloat() - 10f, 80f, paint)
                    canvas.drawCircle(x + 100f, y.toFloat(), 60f, paint)
                }
            }
            else -> {
                paint.color = 0x30FFFFFF.toInt()
                canvas.drawRect(0f, height * 0.75f, width.toFloat(), height.toFloat(), paint)
            }
        }
    }

    private fun renderTouchControls(canvas: Canvas) {
        paint.color = if (moveLeftPressed) 0xFFFFB74D.toInt() else 0xFFFF9800.toInt()
        canvas.drawRoundRect(btnLeftRect, 24f, 24f, paint)
        paint.color = Color.WHITE
        val pathLeft = Path().apply {
            moveTo(btnLeftRect.centerX() + 15f, btnLeftRect.centerY() - 25f)
            lineTo(btnLeftRect.centerX() - 20f, btnLeftRect.centerY())
            lineTo(btnLeftRect.centerX() + 15f, btnLeftRect.centerY() + 25f)
            close()
        }
        canvas.drawPath(pathLeft, paint)

        paint.color = if (moveRightPressed) 0xFFFFB74D.toInt() else 0xFFFF9800.toInt()
        canvas.drawRoundRect(btnRightRect, 24f, 24f, paint)
        val pathRight = Path().apply {
            moveTo(btnRightRect.centerX() - 15f, btnRightRect.centerY() - 25f)
            lineTo(btnRightRect.centerX() + 20f, btnRightRect.centerY())
            lineTo(btnRightRect.centerX() - 15f, btnRightRect.centerY() + 25f)
            close()
        }
        canvas.drawPath(pathRight, paint)

        paint.color = 0xFF4CAF50.toInt()
        canvas.drawRoundRect(btnJumpRect, 24f, 24f, paint)
        val pathJump = Path().apply {
            moveTo(btnJumpRect.centerX() - 25f, btnJumpRect.centerY() + 15f)
            lineTo(btnJumpRect.centerX(), btnJumpRect.centerY() - 20f)
            lineTo(btnJumpRect.centerX() + 25f, btnJumpRect.centerY() + 15f)
            close()
        }
        canvas.drawPath(pathJump, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val x = event.getX(index)
        val y = event.getY(index)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (btnLeftRect.contains(x, y)) {
                    moveLeftPressed = true
                } else if (btnRightRect.contains(x, y)) {
                    moveRightPressed = true
                } else if (btnJumpRect.contains(x, y)) {
                    if (player.isGrounded) {
                        player.vy = player.jumpVelocity
                        player.isGrounded = false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.pointerCount == 1) {
                    moveLeftPressed = false
                    moveRightPressed = false
                }
            }
        }
        return true
    }
}
