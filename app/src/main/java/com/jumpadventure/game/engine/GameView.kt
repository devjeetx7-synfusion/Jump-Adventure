package com.jumpadventure.game.engine

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jumpadventure.game.audio.SoundManager
import com.jumpadventure.game.level.ElementType
import com.jumpadventure.game.level.LevelElement
import com.jumpadventure.game.level.LevelGenerator
import com.jumpadventure.game.level.WorldRepository
import com.jumpadventure.game.model.GameSaveData
import com.jumpadventure.game.model.WorldInfo
import kotlin.math.abs
import kotlin.math.hypot

class GameView(
    context: Context,
    val saveData: GameSaveData,
    val soundManager: SoundManager,
    val onLevelCompleted: (coinsEarned: Int, starsEarned: Int, timeTakenSec: Float) -> Unit,
    val onGameOver: () -> Unit,
    val onPauseClicked: () -> Unit,
    val onProgressUpdated: (coins: Int, stars: Int, levelProgressPercent: Float) -> Unit
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    @Volatile
    private var isRunning = false
    private var gameThread: Thread? = null

    // World & Level Layout
    var currentLevelNumber = saveData.currentLevel
    private lateinit var worldInfo: WorldInfo
    private lateinit var levelLayout: com.jumpadventure.game.level.LevelLayout

    // Player Physics State
    private var playerX = 50f
    private var playerY = 700f
    private val playerWidth = 60f
    private val playerHeight = 80f
    private var velocityX = 0f
    private var velocityY = 0f
    private val moveSpeed = 9f
    private val jumpStrength = -17f
    private val gravity = 0.75f
    private var isGrounded = false
    private var lives = 3
    private var checkpointX = 50f
    private var checkpointY = 700f

    // Power-ups state
    var isMagnetActive = false
    private var magnetTimer = 0
    var isShieldActive = false
    var isSpeedActive = false
    private var speedTimer = 0

    // Touch Controls
    private var moveLeftPressed = false
    private var moveRightPressed = false

    // Camera
    private var cameraX = 0f

    // Gameplay Statistics
    private var coinsCollectedInLevel = 0
    private var starsCollectedInLevel = 0
    private var levelStartTime = System.currentTimeMillis()

    // Dynamic Element States (for moving platforms/enemies/active coins)
    private class ActiveElement(
        val original: LevelElement,
        var currentX: Float,
        var currentY: Float,
        var direction: Float = 1f,
        var isCollected: Boolean = false,
        var isActivated: Boolean = false
    )

    private val activeElements = mutableListOf<ActiveElement>()

    // Colors & Paints
    private val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val platformPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val darkOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B2F")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val coinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.FILL
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEB3B")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
    }

    // Touch Control Rects
    private val leftButtonRect = RectF()
    private val rightButtonRect = RectF()
    private val jumpButtonRect = RectF()

    init {
        holder.addCallback(this)
        loadLevel(currentLevelNumber)
    }

    fun loadLevel(levelNum: Int) {
        currentLevelNumber = levelNum
        worldInfo = WorldRepository.getWorldForLevel(levelNum)
        levelLayout = LevelGenerator.generateLevel(levelNum)

        playerX = 50f
        playerY = 700f
        velocityX = 0f
        velocityY = 0f
        checkpointX = 50f
        checkpointY = 700f
        lives = 3

        coinsCollectedInLevel = 0
        starsCollectedInLevel = 0
        levelStartTime = System.currentTimeMillis()

        activeElements.clear()
        levelLayout.elements.forEach { elem ->
            activeElements.add(ActiveElement(elem, elem.x, elem.y))
        }

        skyPaint.color = Color.parseColor(worldInfo.skyColorHex)
        platformPaint.color = Color.parseColor(worldInfo.platformColorHex)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val btnSize = width * 0.18f
        val padding = 40f
        leftButtonRect.set(padding, height - btnSize - padding, padding + btnSize, height - padding)
        rightButtonRect.set(padding + btnSize + 30f, height - btnSize - padding, padding + btnSize * 2 + 30f, height - padding)
        jumpButtonRect.set(width - btnSize - padding, height - btnSize - padding, width - padding, height - padding)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun run() {
        val targetFPS = 60
        val targetTime = 1000L / targetFPS

        while (isRunning) {
            val startTime = System.currentTimeMillis()

            update()
            drawFrame()

            val elapsedTime = System.currentTimeMillis() - startTime
            val sleepTime = targetTime - elapsedTime
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun update() {
        // Power-up timers
        if (isMagnetActive) {
            magnetTimer--
            if (magnetTimer <= 0) isMagnetActive = false
        }
        if (isSpeedActive) {
            speedTimer--
            if (speedTimer <= 0) isSpeedActive = false
        }

        // Horizontal velocity update
        val currentSpeed = if (isSpeedActive) moveSpeed * 1.5f else moveSpeed
        if (moveLeftPressed) {
            velocityX = -currentSpeed
        } else if (moveRightPressed) {
            velocityX = currentSpeed
        } else {
            velocityX = 0f
        }

        // Apply gravity
        velocityY += gravity

        // Update player position
        playerX += velocityX
        playerY += velocityY

        // Camera follow
        val screenWidth = width.toFloat().takeIf { it > 0 } ?: 1080f
        cameraX = playerX - screenWidth * 0.35f
        if (cameraX < 0) cameraX = 0f

        // Collision logic
        isGrounded = false
        val playerRect = RectF(playerX, playerY, playerX + playerWidth, playerY + playerHeight)

        activeElements.forEach { active ->
            if (active.isCollected) return@forEach
            val elem = active.original

            // Dynamic movement for moving platforms and enemies
            if (elem.type == ElementType.MOVING_PLATFORM) {
                active.currentX += elem.speed * active.direction
                if (abs(active.currentX - elem.x) > elem.moveDistanceX) {
                    active.direction *= -1f
                }
            } else if (elem.type == ElementType.ENEMY) {
                active.currentX += elem.speed * active.direction
                if (abs(active.currentX - elem.x) > elem.moveDistanceX) {
                    active.direction *= -1f
                }
            }

            val elemRect = RectF(active.currentX, active.currentY, active.currentX + elem.width, active.currentY + elem.height)

            // Magnet effect for coins
            if (isMagnetActive && elem.type == ElementType.COIN) {
                val dist = hypot((active.currentX - playerX).toDouble(), (active.currentY - playerY).toDouble())
                if (dist < 350) {
                    active.currentX += (playerX - active.currentX) * 0.15f
                    active.currentY += (playerY - active.currentY) * 0.15f
                }
            }

            if (RectF.intersects(playerRect, elemRect)) {
                when (elem.type) {
                    ElementType.PLATFORM, ElementType.MOVING_PLATFORM, ElementType.BOX -> {
                        // Top collision (landing on platform)
                        if (velocityY > 0 && playerY + playerHeight - velocityY <= active.currentY + 15f) {
                            playerY = active.currentY - playerHeight
                            velocityY = 0f
                            isGrounded = true

                            // Ride moving platform
                            if (elem.type == ElementType.MOVING_PLATFORM) {
                                playerX += elem.speed * active.direction
                            }
                        }
                    }

                    ElementType.COIN -> {
                        active.isCollected = true
                        coinsCollectedInLevel += 10
                        soundManager.playCoin()
                    }

                    ElementType.STAR -> {
                        active.isCollected = true
                        starsCollectedInLevel += 1
                        soundManager.playStar()
                    }

                    ElementType.CHECKPOINT -> {
                        if (!active.isActivated) {
                            active.isActivated = true
                            checkpointX = active.currentX
                            checkpointY = active.currentY - playerHeight
                        }
                    }

                    ElementType.SPIKE, ElementType.ENEMY -> {
                        if (isShieldActive) {
                            isShieldActive = false
                            active.isCollected = true
                        } else {
                            handlePlayerHit()
                        }
                    }

                    ElementType.FINISH_DOOR -> {
                        completeLevel()
                    }
                }
            }
        }

        // Fall into gap check
        if (playerY > 1200f) {
            handlePlayerHit()
        }

        // Notify HUD callback
        val progress = (playerX / levelLayout.totalWidth).coerceIn(0f, 1f)
        onProgressUpdated(coinsCollectedInLevel, starsCollectedInLevel, progress)
    }

    private fun handlePlayerHit() {
        soundManager.playHit()
        lives--
        if (lives <= 0) {
            onGameOver()
        } else {
            playerX = checkpointX
            playerY = checkpointY
            velocityY = 0f
        }
    }

    private fun completeLevel() {
        soundManager.playLevelComplete()
        val timeSec = (System.currentTimeMillis() - levelStartTime) / 1000f
        onLevelCompleted(coinsCollectedInLevel, starsCollectedInLevel, timeSec)
    }

    fun triggerJump() {
        if (isGrounded) {
            velocityY = jumpStrength
            isGrounded = false
            soundManager.playJump()
            saveData.totalJumps++
        }
    }

    fun activateMagnetPowerUp() {
        isMagnetActive = true
        magnetTimer = 300 // 5 seconds at 60fps
    }

    fun activateShieldPowerUp() {
        isShieldActive = true
    }

    fun activateSpeedPowerUp() {
        isSpeedActive = true
        speedTimer = 300
    }

    private fun drawFrame() {
        if (!holder.surface.isValid) return
        val canvas = holder.lockCanvas() ?: return

        try {
            // Background Sky
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)

            // Draw Parallax Mountains & Clouds
            val bgPaint = Paint().apply { color = Color.WHITE; alpha = 60 }
            canvas.drawCircle(300f - cameraX * 0.2f, 250f, 120f, bgPaint)
            canvas.drawCircle(400f - cameraX * 0.2f, 220f, 150f, bgPaint)

            canvas.save()
            canvas.translate(-cameraX, 0f)

            // Render Level Elements
            activeElements.forEach { active ->
                if (active.isCollected) return@forEach
                val elem = active.original
                val rect = RectF(active.currentX, active.currentY, active.currentX + elem.width, active.currentY + elem.height)

                when (elem.type) {
                    ElementType.PLATFORM, ElementType.MOVING_PLATFORM -> {
                        // Top grass cap + main body
                        canvas.drawRoundRect(rect, 16f, 16f, platformPaint)
                        canvas.drawRoundRect(rect, 16f, 16f, darkOutlinePaint)
                        val capRect = RectF(rect.left, rect.top, rect.right, rect.top + 10f)
                        val capPaint = Paint().apply { color = Color.parseColor("#81C784") }
                        canvas.drawRoundRect(capRect, 8f, 8f, capPaint)
                    }

                    ElementType.BOX -> {
                        val boxPaint = Paint().apply { color = Color.parseColor("#A1887F") }
                        canvas.drawRoundRect(rect, 10f, 10f, boxPaint)
                        canvas.drawRoundRect(rect, 10f, 10f, darkOutlinePaint)
                    }

                    ElementType.COIN -> {
                        val cx = active.currentX + elem.width / 2f
                        val cy = active.currentY + elem.height / 2f
                        canvas.drawCircle(cx, cy, elem.width / 2f, coinPaint)
                        canvas.drawCircle(cx, cy, elem.width / 2f, darkOutlinePaint)
                    }

                    ElementType.STAR -> {
                        val cx = active.currentX + elem.width / 2f
                        val cy = active.currentY + elem.height / 2f
                        canvas.drawCircle(cx, cy, elem.width / 2f, starPaint)
                        canvas.drawCircle(cx, cy, elem.width / 2f, darkOutlinePaint)
                    }

                    ElementType.SPIKE -> {
                        val path = Path().apply {
                            moveTo(rect.left, rect.bottom)
                            lineTo(rect.centerX(), rect.top)
                            lineTo(rect.right, rect.bottom)
                            close()
                        }
                        val spikePaint = Paint().apply { color = Color.parseColor("#78909C") }
                        canvas.drawPath(path, spikePaint)
                        canvas.drawPath(path, darkOutlinePaint)
                    }

                    ElementType.ENEMY -> {
                        val enemyPaint = Paint().apply { color = Color.parseColor("#E53935") }
                        canvas.drawRoundRect(rect, 12f, 12f, enemyPaint)
                        canvas.drawRoundRect(rect, 12f, 12f, darkOutlinePaint)
                    }

                    ElementType.CHECKPOINT -> {
                        val flagColor = if (active.isActivated) Color.GREEN else Color.GRAY
                        val flagPaint = Paint().apply { color = flagColor }
                        canvas.drawRect(rect.left, rect.top, rect.left + 8f, rect.bottom, darkOutlinePaint)
                        canvas.drawRect(rect.left + 8f, rect.top, rect.right, rect.top + 25f, flagPaint)
                    }

                    ElementType.FINISH_DOOR -> {
                        val doorPaint = Paint().apply { color = Color.parseColor("#8D6E63") }
                        canvas.drawRoundRect(rect, 20f, 20f, doorPaint)
                        canvas.drawRoundRect(rect, 20f, 20f, darkOutlinePaint)
                        // Glowing arch
                        val archPaint = Paint().apply { color = Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 8f }
                        canvas.drawRoundRect(rect, 20f, 20f, archPaint)
                    }
                }
            }

            // Draw Player Character (Red Hoodie Runner)
            drawPlayer(canvas)

            canvas.restore()

            // Draw Touch Controls HUD
            drawControls(canvas)

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        val bodyRect = RectF(playerX, playerY, playerX + playerWidth, playerY + playerHeight)

        // Character Hoodie Body Color based on Skin
        val bodyColorHex = when (saveData.selectedCharacter) {
            "NINJA" -> "#212121"
            "ROBOT" -> "#78909C"
            "GIRL" -> "#EC407A"
            "PIRATE" -> "#D84315"
            "COWBOY" -> "#8D6E63"
            else -> "#E53935" // Red Hoodie
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(bodyColorHex)
        }

        // Draw character hoodie rounded rectangle
        canvas.drawRoundRect(bodyRect, 18f, 18f, bodyPaint)
        canvas.drawRoundRect(bodyRect, 18f, 18f, darkOutlinePaint)

        // Face visor / aperture
        val faceRect = RectF(playerX + 12f, playerY + 12f, playerX + playerWidth - 12f, playerY + 36f)
        val facePaint = Paint().apply { color = Color.parseColor("#FFE0B2") }
        canvas.drawRoundRect(faceRect, 8f, 8f, facePaint)

        // Cute eyes
        val eyePaint = Paint().apply { color = Color.BLACK }
        canvas.drawCircle(playerX + 22f, playerY + 24f, 5f, eyePaint)
        canvas.drawCircle(playerX + playerWidth - 22f, playerY + 24f, 5f, eyePaint)

        // Sneaker feet
        val shoePaint = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(RectF(playerX + 4f, playerY + playerHeight - 12f, playerX + 24f, playerY + playerHeight + 2f), 6f, 6f, shoePaint)
        canvas.drawRoundRect(RectF(playerX + playerWidth - 24f, playerY + playerHeight - 12f, playerX + playerWidth - 4f, playerY + playerHeight + 2f), 6f, 6f, shoePaint)

        // Shield aura effect
        if (isShieldActive) {
            val shieldPaint = Paint().apply {
                color = Color.parseColor("#4000E6FF")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(bodyRect.centerX(), bodyRect.centerY(), playerHeight * 0.75f, shieldPaint)
        }
    }

    private fun drawControls(canvas: Canvas) {
        val btnBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC263238")
        }
        val btnActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCFF9800")
        }

        // LEFT Button
        canvas.drawRoundRect(leftButtonRect, 24f, 24f, if (moveLeftPressed) btnActivePaint else btnBgPaint)
        canvas.drawRoundRect(leftButtonRect, 24f, 24f, darkOutlinePaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("◀", leftButtonRect.centerX(), leftButtonRect.centerY() + 12f, textPaint)

        // RIGHT Button
        canvas.drawRoundRect(rightButtonRect, 24f, 24f, if (moveRightPressed) btnActivePaint else btnBgPaint)
        canvas.drawRoundRect(rightButtonRect, 24f, 24f, darkOutlinePaint)
        canvas.drawText("▶", rightButtonRect.centerX(), rightButtonRect.centerY() + 12f, textPaint)

        // JUMP Button
        val jumpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC4CAF50")
        }
        canvas.drawRoundRect(jumpButtonRect, 24f, 24f, jumpPaint)
        canvas.drawRoundRect(jumpButtonRect, 24f, 24f, darkOutlinePaint)
        canvas.drawText("JUMP", jumpButtonRect.centerX(), jumpButtonRect.centerY() + 12f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (leftButtonRect.contains(x, y)) {
                    moveLeftPressed = true
                } else if (rightButtonRect.contains(x, y)) {
                    moveRightPressed = true
                } else if (jumpButtonRect.contains(x, y)) {
                    triggerJump()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (leftButtonRect.contains(x, y)) moveLeftPressed = false
                if (rightButtonRect.contains(x, y)) moveRightPressed = false
            }

            MotionEvent.ACTION_CANCEL -> {
                moveLeftPressed = false
                moveRightPressed = false
            }
        }
        return true
    }
}
