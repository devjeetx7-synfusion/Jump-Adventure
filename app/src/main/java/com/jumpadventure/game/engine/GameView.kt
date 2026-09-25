package com.jumpadventure.game.engine

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jumpadventure.game.audio.SoundManager
import com.jumpadventure.game.graphics.CharacterRenderer
import com.jumpadventure.game.level.ElementType
import com.jumpadventure.game.level.LevelElement
import com.jumpadventure.game.level.LevelGenerator
import com.jumpadventure.game.level.WorldRepository
import com.jumpadventure.game.model.GameSaveData
import com.jumpadventure.game.model.WorldInfo
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

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

    // Player Physics State & Logical Collider vs Visual Drawing Sizes
    private var playerX = 50f
    private var playerY = 700f
    // Visual hero artwork rendering dimensions
    private val visualWidth = 92f
    private val visualHeight = 124f
    // Logical physics collider dimensions (slightly tighter to prevent clipping/getting stuck)
    private val colliderWidth = 54f
    private val colliderHeight = 104f
    private val colliderOffsetX = (visualWidth - colliderWidth) / 2f
    private val colliderOffsetY = visualHeight - colliderHeight

    private var velocityX = 0f
    private var velocityY = 0f
    private val moveSpeed = 9f
    private val jumpStrength = -17f
    private val gravity = 0.75f
    private var isGrounded = false
    private var lives = 3
    private var checkpointX = 50f
    private var checkpointY = 676f

    @Volatile
    private var levelCompletionHandled = false
    @Volatile
    private var gameOverHandled = false

    // Power-ups state
    var isMagnetActive = false
    private var magnetTimer = 0
    var isShieldActive = false
    var isSpeedActive = false
    private var speedTimer = 0

    // Touch Controls
    private var moveLeftPressed = false
    private var moveRightPressed = false
    private var isFacingRight = true
    private var animTick = 0f

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

    // Cached Background Bitmap & Rects
    private var cachedBgBitmap: Bitmap? = null
    private val bgSrcRect = Rect()
    private val bgDstRect = RectF()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bgEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
    private val magnetButtonRect = RectF()
    private val speedButtonRect = RectF()
    private val shieldButtonRect = RectF()

    init {
        holder.addCallback(this)
        loadLevel(currentLevelNumber)
    }

    fun loadLevel(levelNum: Int) {
        currentLevelNumber = levelNum
        worldInfo = WorldRepository.getWorldForLevel(levelNum)
        levelLayout = LevelGenerator.generateLevel(levelNum)

        // Always spawn the hero on top of the actual starting platform.
        // The previous hard-coded Y=700 placed the 124px visual hero below the
        // 800px platform, so gravity made the character fall immediately.
        playerX = 50f
        playerY = findSafeSpawnY(playerX)
        velocityX = 0f
        velocityY = 0f
        checkpointX = playerX
        checkpointY = playerY
        lives = 3
        levelCompletionHandled = false
        gameOverHandled = false
        isGrounded = true
        moveLeftPressed = false
        moveRightPressed = false

        coinsCollectedInLevel = 0
        starsCollectedInLevel = 0
        levelStartTime = System.currentTimeMillis()

        activeElements.clear()
        levelLayout.elements.forEach { elem ->
            activeElements.add(ActiveElement(elem, elem.x, elem.y))
        }

        skyPaint.color = Color.parseColor(worldInfo.skyColorHex)
        platformPaint.color = Color.parseColor(worldInfo.platformColorHex)

        // Cached environmental background bitmap loading
        val bgResId = WorldRepository.getWorldBackgroundRes(worldInfo.id)
        cachedBgBitmap = BitmapFactory.decodeResource(resources, bgResId)
    }

    fun stopGameLoop() {
        isRunning = false
        gameThread?.let {
            var retry = true
            while (retry) {
                try {
                    it.join(500)
                    retry = false
                } catch (e: InterruptedException) {
                    retry = false
                }
            }
        }
        gameThread = null
    }

    fun pauseGame() {
        isRunning = false
    }

    fun resumeGame() {
        if (!isRunning && holder.surface.isValid) {
            isRunning = true
            gameThread = Thread(this).apply { start() }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        android.util.Log.d("JUMP_DEBUG", "SURFACE_CREATED")
        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        updateControlLayouts(width, height)
    }

    fun updateControlLayouts(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val density = resources.displayMetrics.density

        val baseSideSize = (width * 0.16f).coerceIn(68f * density, 88f * density)
        val baseJumpSize = (width * 0.20f).coerceIn(84f * density, 106f * density)
        val basePowerSize = (54f * density).coerceIn(46f * density, 62f * density)

        val marginHoriz = 20f * density
        val marginBottom = 40f * density
        val gap = 16f * density

        // Default placements
        val defLeftX = marginHoriz
        val defLeftY = height - baseSideSize - marginBottom
        val defRightX = marginHoriz + baseSideSize + gap
        val defRightY = height - baseSideSize - marginBottom
        val defJumpX = width - marginHoriz - baseJumpSize
        val defJumpY = height - baseJumpSize - marginBottom

        val defMagnetX = width - marginHoriz - basePowerSize
        val defMagnetY = defJumpY - basePowerSize * 3.3f
        val defSpeedX = width - marginHoriz - basePowerSize
        val defSpeedY = defJumpY - basePowerSize * 2.2f
        val defShieldX = width - marginHoriz - basePowerSize
        val defShieldY = defJumpY - basePowerSize * 1.1f

        // LEFT
        val leftW = baseSideSize * saveData.leftScale
        val leftH = baseSideSize * saveData.leftScale
        val lx = if (saveData.leftX >= 0) saveData.leftX.coerceIn(0f, width - leftW) else defLeftX
        val ly = if (saveData.leftY >= 0) saveData.leftY.coerceIn(0f, height - leftH) else defLeftY
        leftButtonRect.set(lx, ly, lx + leftW, ly + leftH)

        // RIGHT
        val rightW = baseSideSize * saveData.rightScale
        val rightH = baseSideSize * saveData.rightScale
        val rx = if (saveData.rightX >= 0) saveData.rightX.coerceIn(0f, width - rightW) else defRightX
        val ry = if (saveData.rightY >= 0) saveData.rightY.coerceIn(0f, height - rightH) else defRightY
        rightButtonRect.set(rx, ry, rx + rightW, ry + rightH)

        // JUMP
        val jumpW = baseJumpSize * saveData.jumpScale
        val jumpH = baseJumpSize * saveData.jumpScale
        val jx = if (saveData.jumpX >= 0) saveData.jumpX.coerceIn(0f, width - jumpW) else defJumpX
        val jy = if (saveData.jumpY >= 0) saveData.jumpY.coerceIn(0f, height - jumpH) else defJumpY
        jumpButtonRect.set(jx, jy, jx + jumpW, jy + jumpH)

        // MAGNET
        val magnetW = basePowerSize * saveData.magnetScale
        val magnetH = basePowerSize * saveData.magnetScale
        val mx = if (saveData.magnetX >= 0) saveData.magnetX.coerceIn(0f, width - magnetW) else defMagnetX
        val my = if (saveData.magnetY >= 0) saveData.magnetY.coerceIn(0f, height - magnetH) else defMagnetY
        magnetButtonRect.set(mx, my, mx + magnetW, my + magnetH)

        // SPEED
        val speedW = basePowerSize * saveData.speedScale
        val speedH = basePowerSize * saveData.speedScale
        val sx = if (saveData.speedX >= 0) saveData.speedX.coerceIn(0f, width - speedW) else defSpeedX
        val sy = if (saveData.speedY >= 0) saveData.speedY.coerceIn(0f, height - speedH) else defSpeedY
        speedButtonRect.set(sx, sy, sx + speedW, sy + speedH)

        // SHIELD
        val shieldW = basePowerSize * saveData.shieldScale
        val shieldH = basePowerSize * saveData.shieldScale
        val shx = if (saveData.shieldX >= 0) saveData.shieldX.coerceIn(0f, width - shieldW) else defShieldX
        val shy = if (saveData.shieldY >= 0) saveData.shieldY.coerceIn(0f, height - shieldH) else defShieldY
        shieldButtonRect.set(shx, shy, shx + shieldW, shy + shieldH)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        var retry = true
        while (retry) {
            try {
                gameThread?.join(500)
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun run() {
        val targetFPS = 60
        val targetTime = 1000L / targetFPS

        android.util.Log.d("JUMP_DEBUG", "GAME_LOOP_STARTED")

        while (isRunning) {
            val startTime = System.currentTimeMillis()

            try {
                update()
                drawFrame()
            } catch (e: Throwable) {
                android.util.Log.e("JUMP_DEBUG", "Error in game loop iteration", e)
            }

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

        animTick += 1f / 60f

        // Horizontal velocity update
        val currentSpeed = if (isSpeedActive) moveSpeed * 1.5f else moveSpeed
        if (moveLeftPressed) {
            velocityX = -currentSpeed
            isFacingRight = false
        } else if (moveRightPressed) {
            velocityX = currentSpeed
            isFacingRight = true
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

        // Collision logic using dedicated logical collider rectangle
        isGrounded = false
        val colliderLeft = playerX + colliderOffsetX
        val colliderTop = playerY + colliderOffsetY
        val playerRect = RectF(colliderLeft, colliderTop, colliderLeft + colliderWidth, colliderTop + colliderHeight)

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
                        val prevFeetY = colliderTop + colliderHeight - velocityY
                        val currentFeetY = colliderTop + colliderHeight
                        if (velocityY >= 0f) {
                            val platformTop = active.currentY
                            val isLanding = (prevFeetY <= platformTop + 24f && currentFeetY >= platformTop - 6f)
                            if (isLanding) {
                                playerY = platformTop - visualHeight
                                velocityY = 0f
                                isGrounded = true

                                // Ride moving platform
                                if (elem.type == ElementType.MOVING_PLATFORM) {
                                    playerX += elem.speed * active.direction
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Second pass for triggers / items / damage
        val updatedColliderTop = playerY + colliderOffsetY
        val updatedPlayerRect = RectF(playerX + colliderOffsetX, updatedColliderTop, playerX + colliderOffsetX + colliderWidth, updatedColliderTop + colliderHeight)

        activeElements.forEach { active ->
            if (active.isCollected) return@forEach
            val elem = active.original
            val elemRect = RectF(active.currentX, active.currentY, active.currentX + elem.width, active.currentY + elem.height)

            if (RectF.intersects(updatedPlayerRect, elemRect)) {
                when (elem.type) {

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
                            checkpointY = findSafeSpawnY(checkpointX)
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

                    else -> {}
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
        if (gameOverHandled || levelCompletionHandled) return

        soundManager.playHit()
        lives--

        if (lives <= 0) {
            // Stop the game loop BEFORE showing the game-over UI. Otherwise the
            // loop keeps firing GAME OVER every frame and can steal button input.
            gameOverHandled = true
            isRunning = false
            moveLeftPressed = false
            moveRightPressed = false
            velocityX = 0f
            velocityY = 0f
            onGameOver()
            return
        }

        playerX = checkpointX
        playerY = checkpointY
        velocityX = 0f
        velocityY = 0f
        isGrounded = true
    }

    private fun findSafeSpawnY(x: Float): Float {
        val support = levelLayout.elements
            .asSequence()
            .filter {
                (it.type == ElementType.PLATFORM ||
                 it.type == ElementType.MOVING_PLATFORM ||
                 it.type == ElementType.BOX) &&
                x + colliderOffsetX + colliderWidth > it.x &&
                x + colliderOffsetX < it.x + it.width
            }
            .minByOrNull { it.y }

        return support?.y?.minus(visualHeight) ?: (800f - visualHeight)
    }

    private fun completeLevel() {
        if (levelCompletionHandled) return
        levelCompletionHandled = true
        isRunning = false

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
            val w = width.toFloat()
            val h = height.toFloat()

            // 1. Environmental Background Asset Rendering with Parallax
            val bg = cachedBgBitmap
            if (bg != null && !bg.isRecycled && w > 0f && h > 0f) {
                val bgW = bg.width.toFloat()
                val bgH = bg.height.toFloat()

                if (bgW > 0f && bgH > 0f) {
                    val scale = maxOf(w / bgW, h / bgH)
                    val scaledW = bgW * scale

                    val maxOffset = (scaledW - w).coerceAtLeast(0f)
                    val parallaxDistance = maxOffset * 0.35f
                    val progress = (cameraX / levelLayout.totalWidth.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    val parallaxX = -(progress * parallaxDistance)

                    bgSrcRect.set(0, 0, bg.width, bg.height)
                    bgDstRect.set(parallaxX, 0f, parallaxX + scaledW, h)
                    canvas.drawBitmap(bg, bgSrcRect, bgDstRect, bgPaint)

                    if (parallaxX > 0f) {
                        bgEdgePaint.color = bg.getPixel(0, bg.height / 2)
                        canvas.drawRect(0f, 0f, parallaxX, h, bgEdgePaint)
                    }
                    if (parallaxX + scaledW < w) {
                        bgEdgePaint.color = bg.getPixel(bg.width - 1, bg.height / 2)
                        canvas.drawRect(parallaxX + scaledW, 0f, w, h, bgEdgePaint)
                    }
                }
            } else {
                skyPaint.shader = LinearGradient(0f, 0f, 0f, h.coerceAtLeast(1f), Color.parseColor("#1A237E"), Color.parseColor("#4FC3F7"), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w.coerceAtLeast(1f), h.coerceAtLeast(1f), skyPaint)
                skyPaint.shader = null
            }

            canvas.save()
            canvas.translate(-cameraX, 0f)

            // Render Level Elements
            activeElements.forEach { active ->
                if (active.isCollected) return@forEach
                val elem = active.original
                val rect = RectF(active.currentX, active.currentY, active.currentX + elem.width, active.currentY + elem.height)

                when (elem.type) {
                    ElementType.PLATFORM, ElementType.MOVING_PLATFORM -> {
                        // Premium 2.5D styled Platform
                        platformPaint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, platformPaint.color, Color.parseColor("#4E342E"), Shader.TileMode.CLAMP)
                        canvas.drawRoundRect(rect, 12f, 12f, platformPaint)
                        canvas.drawRoundRect(rect, 12f, 12f, darkOutlinePaint)
                        platformPaint.shader = null
                        val capHeight = (elem.height * 0.28f).coerceAtMost(16f)
                        val capRect = RectF(rect.left, rect.top, rect.right, rect.top + capHeight)
                        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(capRect.left, capRect.top, capRect.left, capRect.bottom, Color.parseColor("#81C784"), Color.parseColor("#388E3C"), Shader.TileMode.CLAMP) }
                        canvas.drawRoundRect(capRect, 8f, 8f, capPaint)
                    }

                    ElementType.BOX -> {
                        val boxPaint = Paint().apply { color = Color.parseColor("#8D6E63") }
                        canvas.drawRoundRect(rect, 12f, 12f, boxPaint)
                        canvas.drawRoundRect(rect, 12f, 12f, darkOutlinePaint)
                    }

                    ElementType.COIN -> {
                        val cx = active.currentX + elem.width / 2f
                        val cy = active.currentY + elem.height / 2f + (sin(animTick * 6.0) * 6.0).toFloat()
                        coinPaint.shader = RadialGradient(cx - elem.width * 0.1f, cy - elem.height * 0.1f, elem.width * 0.8f, Color.parseColor("#FFF59D"), Color.parseColor("#F57F17"), Shader.TileMode.CLAMP)
                        canvas.drawCircle(cx, cy, elem.width / 2f, coinPaint)
                        canvas.drawCircle(cx, cy, elem.width / 2f, darkOutlinePaint)
                        coinPaint.shader = null
                        // Inner sheen line
                        val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 180 }
                        canvas.drawCircle(cx - 4f, cy - 4f, elem.width / 6f, shinePaint)
                    }

                    ElementType.STAR -> {
                        val cx = active.currentX + elem.width / 2f
                        val cy = active.currentY + elem.height / 2f + (sin(animTick * 5.0) * 8.0).toFloat()
                        val starPath = Path().apply {
                            val rOut = elem.width / 2f
                            val rIn = elem.width / 4f
                            for (s in 0..9) {
                                val r = if (s % 2 == 0) rOut else rIn
                                val angle = s * (Math.PI / 5) - Math.PI / 2
                                if (s == 0) moveTo(cx + (Math.cos(angle) * r).toFloat(), cy + (Math.sin(angle) * r).toFloat())
                                else lineTo(cx + (Math.cos(angle) * r).toFloat(), cy + (Math.sin(angle) * r).toFloat())
                            }
                            close()
                        }
                        starPaint.shader = RadialGradient(cx, cy, elem.width / 2f, Color.parseColor("#FFF59D"), Color.parseColor("#F57F17"), Shader.TileMode.CLAMP)
                        canvas.drawPath(starPath, starPaint)
                        canvas.drawPath(starPath, darkOutlinePaint)
                        starPaint.shader = null
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

            // Draw Player Character (Full Body Illustrated Hero)
            drawPlayer(canvas)

            canvas.restore()

            // Draw Touch Controls HUD
            drawControls(canvas)

        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        val playerBounds = RectF(playerX, playerY, playerX + visualWidth, playerY + visualHeight)

        val animState = when {
            !isGrounded && velocityY < 0 -> CharacterRenderer.AnimState.JUMP
            !isGrounded && velocityY >= 0 -> CharacterRenderer.AnimState.FALL
            moveLeftPressed || moveRightPressed -> CharacterRenderer.AnimState.RUN
            else -> CharacterRenderer.AnimState.IDLE
        }

        canvas.save()
        canvas.scale(1.28f, 1.28f, playerBounds.centerX(), playerBounds.bottom)
        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = playerBounds,
            characterId = saveData.selectedCharacter,
            facingRight = isFacingRight,
            animState = animState,
            animTime = animTick
        )
        canvas.restore()

        // Shield aura effect
        if (isShieldActive) {
            val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4000E6FF")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(playerBounds.centerX(), playerBounds.centerY(), visualHeight * 0.75f, shieldPaint)
        }
    }

    private fun drawControls(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (leftButtonRect.isEmpty) {
            updateControlLayouts(w.toInt(), h.toInt())
        }

        // Transparent circular 3D game controls without big background panel.
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(85, 0, 20, 50)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = Color.argb(230, 255, 255, 255)
        }
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
        }
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        fun drawCircleButton(rect: RectF, pressed: Boolean, topColor: Int, bottomColor: Int) {
            val radius = minOf(rect.width(), rect.height()) * 0.5f
            val offset = if (pressed) 2f else 6f
            canvas.drawCircle(rect.centerX(), rect.centerY() + offset, radius - 1f, shadowPaint)

            glassPaint.shader = LinearGradient(
                rect.left, rect.top, rect.left, rect.bottom,
                topColor, bottomColor, Shader.TileMode.CLAMP
            )
            val centerY = rect.centerY() - (if (pressed) 1f else 0f)
            canvas.drawCircle(rect.centerX(), centerY, radius - 2f, glassPaint)
            glassPaint.shader = null

            // Glossy top highlight oval
            canvas.drawOval(
                RectF(rect.centerX() - radius * 0.5f, centerY - radius * 0.75f, rect.centerX() + radius * 0.2f, centerY - radius * 0.35f),
                highlightPaint
            )

            canvas.drawCircle(rect.centerX(), centerY, radius - 2f, borderPaint)
        }

        // 1. LEFT (Blue / Cyan)
        drawCircleButton(
            leftButtonRect,
            moveLeftPressed,
            Color.parseColor("#42D9FF"),
            Color.parseColor("#0879D7")
        )
        val leftPath = Path().apply {
            val cx = leftButtonRect.centerX()
            val cy = leftButtonRect.centerY()
            val s = leftButtonRect.width() * 0.22f
            moveTo(cx + s * 0.8f, cy - s)
            lineTo(cx - s * 0.9f, cy)
            lineTo(cx + s * 0.8f, cy + s)
            close()
        }
        canvas.drawPath(leftPath, iconPaint)

        // 2. RIGHT (Blue / Cyan)
        drawCircleButton(
            rightButtonRect,
            moveRightPressed,
            Color.parseColor("#42D9FF"),
            Color.parseColor("#0879D7")
        )
        val rightPath = Path().apply {
            val cx = rightButtonRect.centerX()
            val cy = rightButtonRect.centerY()
            val s = rightButtonRect.width() * 0.22f
            moveTo(cx - s * 0.8f, cy - s)
            lineTo(cx + s * 0.9f, cy)
            lineTo(cx - s * 0.8f, cy + s)
            close()
        }
        canvas.drawPath(rightPath, iconPaint)

        // 3. JUMP (Green / Teal)
        drawCircleButton(
            jumpButtonRect,
            false,
            Color.parseColor("#4DE3B0"),
            Color.parseColor("#078D6D")
        )
        textPaint.textSize = minOf(jumpButtonRect.width() * 0.22f, 26f)
        canvas.drawText("JUMP", jumpButtonRect.centerX(), jumpButtonRect.centerY() + textPaint.textSize * 0.34f, textPaint)

        // 4. MAGNET (Blue)
        drawCircleButton(
            magnetButtonRect,
            isMagnetActive,
            Color.parseColor("#38BDF8"),
            Color.parseColor("#0284C7")
        )
        val magnetIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = magnetButtonRect.width() * 0.12f
        }
        val mcx = magnetButtonRect.centerX()
        val mcy = magnetButtonRect.centerY()
        val ms = magnetButtonRect.width() * 0.26f
        canvas.drawArc(RectF(mcx - ms, mcy - ms, mcx + ms, mcy + ms * 0.5f), 180f, 180f, false, magnetIconPaint)
        canvas.drawLine(mcx - ms, mcy, mcx - ms, mcy + ms * 0.5f, magnetIconPaint)
        canvas.drawLine(mcx + ms, mcy, mcx + ms, mcy + ms * 0.5f, magnetIconPaint)

        // 5. SPEED (Orange / Gold)
        drawCircleButton(
            speedButtonRect,
            isSpeedActive,
            Color.parseColor("#FFD43B"),
            Color.parseColor("#FF9F1C")
        )
        val speedPath = Path().apply {
            val scx = speedButtonRect.centerX()
            val scy = speedButtonRect.centerY()
            val ss = speedButtonRect.width() * 0.28f
            moveTo(scx - ss * 0.4f, scy + ss * 0.6f)
            lineTo(scx + ss * 0.1f, scy + ss * 0.05f)
            lineTo(scx - ss * 0.1f, scy + ss * 0.05f)
            lineTo(scx + ss * 0.4f, scy - ss * 0.6f)
            lineTo(scx - ss * 0.1f, scy - ss * 0.05f)
            lineTo(scx + ss * 0.1f, scy - ss * 0.05f)
            close()
        }
        canvas.drawPath(speedPath, iconPaint)

        // 6. SHIELD (Green / Teal)
        drawCircleButton(
            shieldButtonRect,
            isShieldActive,
            Color.parseColor("#36C96F"),
            Color.parseColor("#059669")
        )
        val shieldPath = Path().apply {
            val shcx = shieldButtonRect.centerX()
            val shcy = shieldButtonRect.centerY()
            val shs = shieldButtonRect.width() * 0.28f
            moveTo(shcx, shcy - shs * 0.65f)
            lineTo(shcx + shs * 0.55f, shcy - shs * 0.35f)
            lineTo(shcx + shs * 0.45f, shcy + shs * 0.35f)
            quadTo(shcx, shcy + shs * 0.7f, shcx - shs * 0.45f, shcy + shs * 0.35f)
            lineTo(shcx - shs * 0.55f, shcy - shs * 0.35f)
            close()
        }
        canvas.drawPath(shieldPath, iconPaint)
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
                } else if (magnetButtonRect.contains(x, y)) {
                    soundManager.playButtonClick()
                    activateMagnetPowerUp()
                } else if (speedButtonRect.contains(x, y)) {
                    soundManager.playButtonClick()
                    activateSpeedPowerUp()
                } else if (shieldButtonRect.contains(x, y)) {
                    soundManager.playButtonClick()
                    activateShieldPowerUp()
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
