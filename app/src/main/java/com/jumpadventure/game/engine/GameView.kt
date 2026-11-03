package com.jumpadventure.game.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jumpadventure.game.audio.SoundManager
import com.jumpadventure.game.graphics.CharacterRenderer
import com.jumpadventure.game.level.ElementType
import com.jumpadventure.game.level.LevelElement
import com.jumpadventure.game.level.LevelGenerator
import com.jumpadventure.game.level.LevelLayout
import com.jumpadventure.game.level.WorldRepository
import com.jumpadventure.game.model.GameSaveData
import com.jumpadventure.game.model.WorldInfo
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

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

    @Volatile
    private var isPaused = false

    private var gameThread: Thread? = null

    var currentLevelNumber = saveData.currentLevel
    private lateinit var worldInfo: WorldInfo
    private lateinit var levelLayout: LevelLayout

    // Logical player collider is intentionally narrower than the visual character.
    private var playerX = 50f
    private var playerY = 700f
    private val visualWidth = 92f
    private val visualHeight = 124f
    private val colliderWidth = 54f
    private val colliderHeight = 104f
    private val colliderOffsetX = (visualWidth - colliderWidth) / 2f
    private val colliderOffsetY = visualHeight - colliderHeight

    private var velocityX = 0f
    private var velocityY = 0f
    private val baseMoveSpeed = 11f
    private val baseJumpStrength = -17f
    private val gravityPerFrame = 0.75f
    private val maxFallVelocity = 25f

    /** Fixed duration for the POWER pickup, which has no upgrade track in the shop. */
    private val POWER_DURATION = 8f

    /** Short grace window after a shield plate absorbs a hit. */
    private val SHIELD_HIT_INVULNERABILITY = 0.25f

    /** Assumed ground surface when a level has no platform under the spawn X. */
    private val DEFAULT_SPAWN_SURFACE_Y = 800f

    /** Tolerance when picking the surface "at or below" the reference feet. */
    private val SPAWN_SURFACE_SEARCH_SLACK = 48f

    /** How many positions to step forward while looking for a hazard-free spawn. */
    private val MAX_RESPAWN_HAZARD_SEARCH = 6
    private var isGrounded = false
    private var lives = 3
    private var checkpointX = 50f
    private var checkpointY = 676f

    // Platformer feel helpers.
    private var coyoteTimeRemaining = 0f
    private var jumpBufferRemaining = 0f
    private var damageInvulnerabilityRemaining = 0f
    private val coyoteTimeSeconds = 0.10f
    private val jumpBufferSeconds = 0.12f
    private val damageInvulnerabilitySeconds = 1.0f

    // Character gameplay skill state.
    private var maxMidAirJumps = 0
    private var midAirJumpsDone = 0
    private var isPassiveMagnet = false
    private var isFireResistant = false
    private var isCoinBonusActive = false
    private var characterSpeedMultiplier = 1.0f
    private var characterJumpMultiplier = 1.0f
    private var enemySpeedMultiplier = 1.0f

    @Volatile
    private var levelCompletionHandled = false

    @Volatile
    private var gameOverHandled = false

    // Power-up state. Pickups for HUD power-ups become charges instead of granting
    // unlimited activation from the touch buttons. All activation / duration /
    // charge rules live in PowerUpState so they can be unit-tested.
    internal val powerUps = PowerUpState()

    val isMagnetActive: Boolean get() = powerUps.magnet.isActive
    val magnetTimeRemaining: Float get() = powerUps.magnet.timeRemaining
    private val magnetCharges: Int get() = powerUps.magnet.charges

    val isShieldActive: Boolean get() = powerUps.shield.isActive
    val shieldTimeRemaining: Float get() = powerUps.shield.timeRemaining
    private val shieldCharges: Int get() = powerUps.shield.charges
    private val shieldHitsRemaining: Int get() = powerUps.shieldHitsRemaining

    val isSpeedActive: Boolean get() = powerUps.speed.isActive
    val speedTimeRemaining: Float get() = powerUps.speed.timeRemaining
    private val speedCharges: Int get() = powerUps.speed.charges

    val isHighJumpActive: Boolean get() = powerUps.highJump.isActive
    val highJumpTimeRemaining: Float get() = powerUps.highJump.timeRemaining

    val isPowerActive: Boolean get() = powerUps.power.isActive
    val powerTimeRemaining: Float get() = powerUps.power.timeRemaining

    /** True once the level has been won or lost; the run can no longer resume. */
    val isRunFinished: Boolean
        get() = levelCompletionHandled || gameOverHandled

    /** Number of lives left, exposed for HUD / diagnostics. */
    val livesRemaining: Int get() = lives

    private var moveLeftPressed = false
    private var moveRightPressed = false
    private var isFacingRight = true
    private var animTick = 0f

    // Timed animation overrides (LAND / HIT / GAME_OVER) layered over the
    // physics-driven states so landings and hits read clearly.
    private var animOverride: CharacterRenderer.AnimState? = null
    private var animOverrideRemaining = 0f
    private var cameraX = 0f

    private var coinsCollectedInLevel = 0
    private var starsCollectedInLevel = 0
    private var gemsCollectedInLevel = 0
    private var levelStartTime = System.currentTimeMillis()
    private var hudUpdateAccumulator = 0f

    private class ActiveElement(
        val original: LevelElement,
        var currentX: Float,
        var currentY: Float,
        var direction: Float = 1f,
        var isCollected: Boolean = false,
        var isActivated: Boolean = false
    )

    private class TrailParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var alpha: Int,
        val color: Int,
        val size: Float
    )

    private val activeElements = mutableListOf<ActiveElement>()
    private val trailParticles = mutableListOf<TrailParticle>()
    private val random = Random.Default

    private var cachedBgBitmap: Bitmap? = null
    private val bgSrcRect = Rect()
    private val bgDstRect = RectF()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bgEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
    private val reusableRect = RectF()
    private val controlShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(85, 0, 20, 50) }
    private val controlGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val controlFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val controlBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val controlBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E60F172A") }
    private val controlBuffBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val controlBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val controlIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val controlMagnetStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE }
    private val reusablePath = Path()

    private val leftButtonRect = RectF()
    private val rightButtonRect = RectF()
    private val jumpButtonRect = RectF()
    private val magnetButtonRect = RectF()
    private val speedButtonRect = RectF()
    private val shieldButtonRect = RectF()
    private val activeJumpPointers = mutableSetOf<Int>()

    init {
        holder.addCallback(this)
        isFocusable = true
        loadLevel(currentLevelNumber)
    }

    private fun upgradeLevel(id: String): Int = (saveData.powerUpLevels[id] ?: 1).coerceIn(1, 5)

    private fun magnetDuration(): Float = 8f + (upgradeLevel("MAGNET") - 1) * 1.5f
    private fun magnetRadius(): Double = 420.0 + (upgradeLevel("MAGNET") - 1) * 45.0
    private fun shieldDuration(): Float = 10f + (upgradeLevel("SHIELD") - 1) * 1.5f
    private fun shieldHitCapacity(): Int = 1 + (upgradeLevel("SHIELD") - 1) / 2
    private fun speedDuration(): Float = 8f + (upgradeLevel("SPEED") - 1)
    private fun speedPowerMultiplier(): Float = 1.50f + (upgradeLevel("SPEED") - 1) * 0.05f
    private fun highJumpDuration(): Float = 8f + (upgradeLevel("JUMP_BOOST") - 1)
    private fun jumpUpgradeMultiplier(): Float = 1f + (upgradeLevel("JUMP_BOOST") - 1) * 0.08f
    private fun coinUpgradeMultiplier(): Float = 1f + (upgradeLevel("DOUBLE_COIN") - 1) * 0.15f
    private fun starAssistRadius(): Double = (upgradeLevel("STAR_BOOST") - 1) * 70.0

    fun loadLevel(levelNum: Int) {
        currentLevelNumber = levelNum.coerceAtLeast(1)
        worldInfo = WorldRepository.getWorldForLevel(currentLevelNumber)
        levelLayout = LevelGenerator.generateLevel(currentLevelNumber)

        lives = 3
        levelCompletionHandled = false
        gameOverHandled = false
        moveLeftPressed = false
        moveRightPressed = false
        coyoteTimeRemaining = coyoteTimeSeconds
        jumpBufferRemaining = 0f
        damageInvulnerabilityRemaining = 0f
        velocityX = 0f
        velocityY = 0f
        animOverride = null
        animOverrideRemaining = 0f

        configureCharacterSkills(saveData.selectedCharacter)

        powerUps.resetAll()

        val startsWithShield = saveData.selectedCharacter in setOf("ROBOT", "ROBOT_X", "CYBER_BOT", "SAMURAI")
        if (startsWithShield) {
            powerUps.activateShield(shieldDuration(), shieldHitCapacity())
        }

        coinsCollectedInLevel = 0
        starsCollectedInLevel = 0
        gemsCollectedInLevel = 0
        hudUpdateAccumulator = 0f
        levelStartTime = System.currentTimeMillis()

        // Elements must be instantiated BEFORE the spawn point is resolved,
        // because findSafeSpawnY() reads live element positions.
        activeElements.clear()
        levelLayout.elements.forEach { elem ->
            activeElements.add(ActiveElement(elem, elem.x, elem.y))
        }
        trailParticles.clear()

        playerX = 50f
        playerY = findSafeSpawnY(playerX)
        isGrounded = true
        checkpointX = playerX
        checkpointY = playerY

        skyPaint.color = Color.parseColor(worldInfo.skyColorHex)
        platformPaint.color = Color.parseColor(worldInfo.platformColorHex)

        val oldBackground = cachedBgBitmap
        val bgResId = WorldRepository.getWorldBackgroundRes(worldInfo.id)
        cachedBgBitmap = BitmapFactory.decodeResource(resources, bgResId)
        if (oldBackground != null && oldBackground !== cachedBgBitmap && !oldBackground.isRecycled) {
            oldBackground.recycle()
        }

        // Make gameplay music follow the selected world instead of always world 1.
        if (soundManager.musicEnabled) soundManager.startMusic(worldInfo.id)
    }

    private fun configureCharacterSkills(charId: String) {
        maxMidAirJumps = if (charId in setOf("GALAXY_HERO", "GALAXY", "SPACE_RUNNER", "CRYSTAL_MAGE", "VOID_WALKER")) 1 else 0
        midAirJumpsDone = 0

        isPassiveMagnet = charId in setOf("FOREST_GUARDIAN", "FOREST", "JUNGLE_FIGHTER")
        isFireResistant = charId in setOf("LAVA_KNIGHT", "LAVA")
        isCoinBonusActive = charId in setOf("GOLDEN_WARRIOR", "PIRATE", "EXPLORER")
        enemySpeedMultiplier = if (charId in setOf("ICE_WARRIOR", "ICE", "ARCTIC_RANGER")) 0.5f else 1.0f

        characterSpeedMultiplier = when (charId) {
            "STORM_RIDER", "THUNDER_HERO", "NEON_RUNNER", "NEON" -> 1.20f
            "NINJA", "CYBER_NINJA", "SHADOW_HUNTER", "DESERT_RUNNER", "DESERT" -> 1.15f
            "GIRL", "COWBOY" -> 1.08f
            else -> 1.0f
        }

        characterJumpMultiplier = when (charId) {
            "GIRL" -> 1.08f
            "COWBOY" -> 1.04f
            else -> 1.0f
        }
    }

    fun stopGameLoop() {
        isRunning = false
        isPaused = false
        val thread = gameThread
        thread?.interrupt()
        if (thread != null && thread !== Thread.currentThread()) {
            runCatching { thread.join(600) }
            gameThread = null
        } else if (thread == null) {
            gameThread = null
        }
        // If called from the game thread itself, leave gameThread set: it will be
        // cleared by the loop exiting, and resumeGame() treats a dead thread as
        // "not running" anyway.
    }

    fun pauseGame() {
        // Clear input state before flipping the flag so a pointer that was held
        // down when the overlay opened cannot resume movement on unpause.
        moveLeftPressed = false
        moveRightPressed = false
        activeJumpPointers.clear()
        isPaused = true
    }

    fun resumeGame() {
        // Preserve the level-complete freeze: completeLevel() sets isPaused, and
        // resuming there would let the player keep moving behind the win dialog.
        if (levelCompletionHandled || gameOverHandled) return
        isPaused = false
        val thread = gameThread
        if (holder.surface.isValid && (thread == null || !thread.isAlive)) {
            isRunning = true
            gameThread = Thread(this, "JumpAdventure-GameLoop").apply { start() }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (gameThread?.isAlive == true) return
        isRunning = true
        isPaused = false
        gameThread = Thread(this, "JumpAdventure-GameLoop").apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        updateControlLayouts(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGameLoop()
    }

    override fun onDetachedFromWindow() {
        stopGameLoop()
        cachedBgBitmap?.let { if (!it.isRecycled) it.recycle() }
        cachedBgBitmap = null
        super.onDetachedFromWindow()
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

        val defLeftX = marginHoriz
        val defLeftY = height - baseSideSize - marginBottom
        val defRightX = marginHoriz + baseSideSize + gap
        val defRightY = defLeftY
        val defJumpX = width - marginHoriz - baseJumpSize
        val defJumpY = height - baseJumpSize - marginBottom
        val defMagnetX = width - marginHoriz - basePowerSize
        val defMagnetY = defJumpY - basePowerSize * 3.3f
        val defSpeedX = defMagnetX
        val defSpeedY = defJumpY - basePowerSize * 2.2f
        val defShieldX = defMagnetX
        val defShieldY = defJumpY - basePowerSize * 1.1f

        setControlRect(leftButtonRect, saveData.leftX, saveData.leftY, baseSideSize * saveData.leftScale, defLeftX, defLeftY, width, height)
        setControlRect(rightButtonRect, saveData.rightX, saveData.rightY, baseSideSize * saveData.rightScale, defRightX, defRightY, width, height)
        setControlRect(jumpButtonRect, saveData.jumpX, saveData.jumpY, baseJumpSize * saveData.jumpScale, defJumpX, defJumpY, width, height)
        setControlRect(magnetButtonRect, saveData.magnetX, saveData.magnetY, basePowerSize * saveData.magnetScale, defMagnetX, defMagnetY, width, height)
        setControlRect(speedButtonRect, saveData.speedX, saveData.speedY, basePowerSize * saveData.speedScale, defSpeedX, defSpeedY, width, height)
        setControlRect(shieldButtonRect, saveData.shieldX, saveData.shieldY, basePowerSize * saveData.shieldScale, defShieldX, defShieldY, width, height)
    }

    private fun setControlRect(
        rect: RectF,
        savedX: Float,
        savedY: Float,
        size: Float,
        defaultX: Float,
        defaultY: Float,
        width: Int,
        height: Int
    ) {
        val safeSize = size.coerceAtLeast(1f)
        val x = if (savedX >= 0f) savedX.coerceIn(0f, (width - safeSize).coerceAtLeast(0f)) else defaultX
        val y = if (savedY >= 0f) savedY.coerceIn(0f, (height - safeSize).coerceAtLeast(0f)) else defaultY
        rect.set(x, y, x + safeSize, y + safeSize)
    }

    override fun run() {
        val fixedStep = 1f / 60f
        val maxCatchUpSteps = 5
        var previous = System.nanoTime()
        var accumulator = 0f

        while (isRunning && !Thread.currentThread().isInterrupted) {
            val frameStart = System.nanoTime()
            val elapsed = ((frameStart - previous) / 1_000_000_000f).coerceIn(0f, 0.10f)
            previous = frameStart

            try {
                if (!isPaused) {
                    accumulator += elapsed
                    var steps = 0
                    while (accumulator >= fixedStep && steps < maxCatchUpSteps) {
                        update(fixedStep)
                        accumulator -= fixedStep
                        steps++
                    }
                    if (steps == maxCatchUpSteps) accumulator = 0f
                    drawFrame()
                } else {
                    Thread.sleep(16L)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (t: Throwable) {
                android.util.Log.e("JUMP_DEBUG", "Error in game loop iteration", t)
            }

            val frameMs = (System.nanoTime() - frameStart) / 1_000_000L
            val sleepMs = 16L - frameMs
            if (sleepMs > 0 && !isPaused) {
                try {
                    Thread.sleep(sleepMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    private fun update(deltaTime: Float) {
        val dtFactor = deltaTime * 60f
        animTick += deltaTime * if (moveLeftPressed || moveRightPressed) 2.5f else 1.5f

        updateTimers(deltaTime)

        val settingsSpeed = saveData.playerSpeedMultiplier.coerceIn(0.75f, 1.50f)
        val activeSpeed = if (isSpeedActive) speedPowerMultiplier() else 1.0f
        val effectiveSpeed = baseMoveSpeed * settingsSpeed * characterSpeedMultiplier * activeSpeed
        val targetVelocityX = when {
            moveLeftPressed && !moveRightPressed -> -effectiveSpeed
            moveRightPressed && !moveLeftPressed -> effectiveSpeed
            else -> 0f
        }
        if (targetVelocityX < 0f) isFacingRight = false
        if (targetVelocityX > 0f) isFacingRight = true

        val response = if (targetVelocityX == 0f) 0.24f else 0.32f
        velocityX += (targetVelocityX - velocityX) * (response * dtFactor).coerceIn(0f, 1f)
        if (abs(velocityX) < 0.05f) velocityX = 0f

        velocityY = (velocityY + gravityPerFrame * dtFactor).coerceAtMost(maxFallVelocity)

        if (isGrounded) {
            coyoteTimeRemaining = coyoteTimeSeconds
        } else {
            coyoteTimeRemaining = (coyoteTimeRemaining - deltaTime).coerceAtLeast(0f)
        }

        if (jumpBufferRemaining > 0f) {
            jumpBufferRemaining = (jumpBufferRemaining - deltaTime).coerceAtLeast(0f)
            if (isGrounded || coyoteTimeRemaining > 0f) performGroundJump()
        }

        updateParticles(dtFactor)
        updateMovingElements(dtFactor)
        applyAttraction(dtFactor)

        // Landing detection: remember airborne state + fall speed before collision.
        val wasGroundedBeforeMove = isGrounded
        val fallSpeedBeforeMove = velocityY
        moveAndCollide(dtFactor)
        if (!wasGroundedBeforeMove && isGrounded && fallSpeedBeforeMove > 9f) {
            animOverride = CharacterRenderer.AnimState.LAND
            animOverrideRemaining = 0.16f
            soundManager.playLand()
        }
        handleCollectiblesAndHazards()

        if (playerY > 1200f) handlePlayerHit()

        val screenWidth = width.toFloat().takeIf { it > 0f } ?: 1080f
        val targetCameraX = (playerX - screenWidth * 0.35f).coerceAtLeast(0f)
        cameraX += (targetCameraX - cameraX) * (0.18f * dtFactor).coerceAtMost(1f)

        hudUpdateAccumulator += deltaTime
        if (hudUpdateAccumulator >= 0.10f) {
            hudUpdateAccumulator = 0f
            val progress = (playerX / levelLayout.totalWidth.coerceAtLeast(1f)).coerceIn(0f, 1f)
            onProgressUpdated(coinsCollectedInLevel, starsCollectedInLevel, progress)
        }
    }

    private fun updateTimers(deltaTime: Float) {
        if (damageInvulnerabilityRemaining > 0f) {
            damageInvulnerabilityRemaining = (damageInvulnerabilityRemaining - deltaTime).coerceAtLeast(0f)
        }

        if (animOverrideRemaining > 0f) {
            animOverrideRemaining -= deltaTime
            if (animOverrideRemaining <= 0f) {
                animOverrideRemaining = 0f
                // GAME_OVER stays until the run is reset; everything else clears.
                if (animOverride != CharacterRenderer.AnimState.GAME_OVER) animOverride = null
            }
        }

        powerUps.tick(deltaTime)
    }

    private fun updateParticles(dtFactor: Float) {
        if ((abs(velocityX) > 0.3f || abs(velocityY) > 0.3f) && saveData.selectedTrail != "NONE" && trailParticles.size < 90) {
            val px = playerX + visualWidth / 2f
            val py = playerY + visualHeight * 0.7f
            val trailColor = trailColor(saveData.selectedTrail)
            trailParticles.add(
                TrailParticle(
                    x = px + (random.nextFloat() - 0.5f) * 20f,
                    y = py + (random.nextFloat() - 0.5f) * 10f,
                    vx = -velocityX * 0.2f + (random.nextFloat() - 0.5f) * 1.5f,
                    vy = (random.nextFloat() - 0.5f) * 1.5f,
                    alpha = 230,
                    color = trailColor,
                    size = 10f + random.nextFloat() * 12f
                )
            )
        }

        val iterator = trailParticles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dtFactor
            p.y += p.vy * dtFactor
            p.alpha -= (14f * dtFactor).toInt().coerceAtLeast(1)
            if (p.alpha <= 0) iterator.remove()
        }
    }

    private fun trailColor(type: String): Int = when (type) {
        "FIRE" -> intArrayOf(Color.YELLOW, Color.RED, Color.parseColor("#FF9F1C"))[random.nextInt(3)]
        "ICE" -> intArrayOf(Color.CYAN, Color.WHITE, Color.parseColor("#00E5FF"))[random.nextInt(3)]
        "LIGHTNING" -> intArrayOf(Color.YELLOW, Color.CYAN, Color.WHITE)[random.nextInt(3)]
        "RAINBOW" -> Color.HSVToColor(floatArrayOf((animTick * 180f) % 360f, 1f, 1f))
        "SHADOW" -> intArrayOf(Color.parseColor("#2D3748"), Color.BLACK, Color.parseColor("#4A5568"))[random.nextInt(3)]
        "GOLD" -> intArrayOf(Color.parseColor("#FFD700"), Color.parseColor("#FFD43B"), Color.WHITE)[random.nextInt(3)]
        "NEON" -> intArrayOf(Color.parseColor("#00E676"), Color.parseColor("#E040FB"), Color.CYAN)[random.nextInt(3)]
        "GALAXY" -> intArrayOf(Color.parseColor("#9C27B0"), Color.parseColor("#3F51B5"), Color.WHITE)[random.nextInt(3)]
        "LEAVES" -> intArrayOf(Color.parseColor("#4CAF50"), Color.parseColor("#81C784"))[random.nextInt(2)]
        "SNOW" -> Color.WHITE
        "LAVA" -> intArrayOf(Color.parseColor("#FF3D00"), Color.parseColor("#D84315"))[random.nextInt(2)]
        else -> Color.YELLOW
    }

    private fun updateMovingElements(dtFactor: Float) {
        activeElements.forEach { active ->
            if (active.isCollected) return@forEach
            val elem = active.original
            if (elem.type == ElementType.MOVING_PLATFORM || elem.type == ElementType.ENEMY) {
                val speed = if (elem.type == ElementType.ENEMY) elem.speed * enemySpeedMultiplier else elem.speed
                active.currentX += speed * active.direction * dtFactor
                val minX = elem.x - elem.moveDistanceX
                val maxX = elem.x + elem.moveDistanceX
                if (active.currentX < minX) {
                    active.currentX = minX
                    active.direction = 1f
                } else if (active.currentX > maxX) {
                    active.currentX = maxX
                    active.direction = -1f
                }
            }
        }
    }

    private fun applyAttraction(dtFactor: Float) {
        // Never drag collectibles around while the run is already over, and skip
        // entirely when the player is mid-respawn / invulnerable so coins cannot
        // be vacuumed across a hazard boundary.
        if (gameOverHandled || levelCompletionHandled) return

        val magnetOn = isMagnetActive || isPassiveMagnet
        val magnetRange = magnetRadius()
        val starRange = starAssistRadius()

        activeElements.forEach { active ->
            if (active.isCollected) return@forEach
            val type = active.original.type
            val isCollectible = type == ElementType.COIN || type == ElementType.STAR || type == ElementType.GEM || type.name.startsWith("POWERUP_")
            if (!isCollectible) return@forEach

            val allowedRange = when {
                magnetOn -> magnetRange
                type == ElementType.STAR && starRange > 0.0 -> starRange
                else -> 0.0
            }
            if (allowedRange <= 0.0) return@forEach

            // Measure and pull centre-to-centre. Comparing top-left corners made
            // the effective radius depend on element size.
            val collectibleCenterX = active.currentX + active.original.width / 2f
            val collectibleCenterY = active.currentY + active.original.height / 2f
            val playerCenterX = playerX + visualWidth / 2f
            val playerCenterY = playerY + visualHeight / 2f

            val dist = hypot(
                (collectibleCenterX - playerCenterX).toDouble(),
                (collectibleCenterY - playerCenterY).toDouble()
            )
            if (dist < allowedRange) {
                val pull = (0.18f * dtFactor).coerceAtMost(0.5f)
                active.currentX += (playerCenterX - collectibleCenterX) * pull
                active.currentY += (playerCenterY - collectibleCenterY) * pull
            }
        }
    }

    private fun moveAndCollide(dtFactor: Float) {
        playerX += velocityX * dtFactor
        playerX = playerX.coerceAtLeast(0f)

        var curLeft = playerX + colliderOffsetX
        var curTop = playerY + colliderOffsetY
        var curRight = curLeft + colliderWidth
        var curBottom = curTop + colliderHeight

        activeElements.forEach { active ->
            if (active.isCollected || !isSolid(active.original.type)) return@forEach
            val elem = active.original
            reusableRect.set(active.currentX, active.currentY, active.currentX + elem.width, active.currentY + elem.height)
            if (curBottom > reusableRect.top + 8f && curTop < reusableRect.bottom - 8f) {
                if (velocityX > 0f && curRight > reusableRect.left && curLeft < reusableRect.left) {
                    playerX = reusableRect.left - colliderOffsetX - colliderWidth
                    velocityX = 0f
                    curLeft = playerX + colliderOffsetX
                    curRight = curLeft + colliderWidth
                } else if (velocityX < 0f && curLeft < reusableRect.right && curRight > reusableRect.right) {
                    playerX = reusableRect.right - colliderOffsetX
                    velocityX = 0f
                    curLeft = playerX + colliderOffsetX
                    curRight = curLeft + colliderWidth
                }
            }
        }

        val prevColliderTop = playerY + colliderOffsetY
        playerY += velocityY * dtFactor
        curTop = playerY + colliderOffsetY
        curBottom = curTop + colliderHeight
        isGrounded = false

        activeElements.forEach { active ->
            if (active.isCollected || !isSolid(active.original.type)) return@forEach
            val elem = active.original
            val elemLeft = active.currentX
            val elemRight = active.currentX + elem.width
            val elemTop = active.currentY
            val elemBottom = active.currentY + elem.height

            if (curRight > elemLeft + 4f && curLeft < elemRight - 4f) {
                if (velocityY >= 0f) {
                    val prevFeet = prevColliderTop + colliderHeight
                    if (prevFeet <= elemTop + 20f && curBottom >= elemTop - 4f) {
                        // Align the COLLIDER bottom to the surface, not the visual
                        // box. Using visualHeight here left the collider 20 units
                        // inside the platform, so the sprite floated above every
                        // surface by colliderOffsetY.
                        playerY = elemTop - colliderOffsetY - colliderHeight
                        velocityY = 0f
                        isGrounded = true
                        midAirJumpsDone = 0
                        coyoteTimeRemaining = coyoteTimeSeconds

                        if (elem.type == ElementType.MOVING_PLATFORM) {
                            playerX += elem.speed * active.direction * dtFactor
                        }
                        if (jumpBufferRemaining > 0f) performGroundJump()
                    }
                } else if (prevColliderTop >= elemBottom - 16f && curTop <= elemBottom + 4f) {
                    playerY = elemBottom - colliderOffsetY
                    velocityY = 0f
                }
            }
        }
    }

    private fun isSolid(type: ElementType): Boolean =
        type == ElementType.PLATFORM || type == ElementType.MOVING_PLATFORM || type == ElementType.BOX

    private fun handleCollectiblesAndHazards() {
        val top = playerY + colliderOffsetY
        val playerRect = RectF(playerX + colliderOffsetX, top, playerX + colliderOffsetX + colliderWidth, top + colliderHeight)

        activeElements.forEach { active ->
            if (active.isCollected) return@forEach
            val elem = active.original
            reusableRect.set(active.currentX, active.currentY, active.currentX + elem.width, active.currentY + elem.height)
            if (!RectF.intersects(playerRect, reusableRect)) return@forEach

            when (elem.type) {
                ElementType.COIN -> {
                    active.isCollected = true
                    val characterBonus = if (isCoinBonusActive) 1.5f else 1f
                    coinsCollectedInLevel += (10f * characterBonus * coinUpgradeMultiplier()).toInt().coerceAtLeast(1)
                    soundManager.playCoin()
                }
                ElementType.STAR -> {
                    active.isCollected = true
                    starsCollectedInLevel++
                    soundManager.playStar()
                }
                ElementType.GEM -> {
                    active.isCollected = true
                    gemsCollectedInLevel++
                    soundManager.playStar()
                }
                ElementType.POWERUP_MAGNET -> {
                    active.isCollected = true
                    collectChargeablePowerUp(
                        powerUps.magnet,
                        { activateMagnetPowerUp() },
                        magnetDuration()
                    )
                }
                ElementType.POWERUP_SHIELD -> {
                    active.isCollected = true
                    collectChargeablePowerUp(
                        powerUps.shield,
                        { activateShieldPowerUp() },
                        shieldDuration()
                    )
                }
                ElementType.POWERUP_SPEED -> {
                    active.isCollected = true
                    collectChargeablePowerUp(
                        powerUps.speed,
                        { activateSpeedPowerUp() },
                        speedDuration()
                    )
                }
                ElementType.POWERUP_HIGH_JUMP -> {
                    active.isCollected = true
                    // High Jump has no HUD button, so it activates immediately.
                    // Re-collecting while active refreshes the timer rather than
                    // being silently discarded.
                    activateHighJumpPowerUp()
                    soundManager.playPowerUp()
                }
                ElementType.POWERUP_POWER -> {
                    active.isCollected = true
                    activatePowerPowerUp()
                    soundManager.playPowerUp()
                }
                ElementType.CHECKPOINT -> {
                    if (!active.isActivated) {
                        active.isActivated = true
                        val safe = findSafeRespawnPoint(active.currentX, active.currentY)
                        checkpointX = safe.first
                        checkpointY = safe.second
                        soundManager.playStar()
                    }
                }
                ElementType.SPIKE -> handleHazard(active, spike = true)
                ElementType.ENEMY -> handleHazard(active, spike = false)
                ElementType.FINISH_DOOR -> completeLevel()
                else -> Unit
            }
        }
    }

    /**
     * Pickup rule for the three HUD power-ups (Magnet / Shield / Speed).
     *
     * Previously every pickup unconditionally added a charge capped at 9, so
     * collecting an 11th pickup while already holding 9 silently did nothing.
     * Now the pickup always has an effect:
     *  - if the power-up is currently running, the remaining time is topped up
     *    back to a full duration (never shortened, so no pickup is wasted);
     *  - otherwise a charge is banked for the on-screen button to spend.
     */
    private fun collectChargeablePowerUp(
        slot: PowerUpState.Slot,
        activate: () -> Unit,
        fullDuration: Float
    ) {
        if (slot.isActive) {
            val extended = maxOf(slot.timeRemaining, fullDuration)
            activate()
            if (slot.timeRemaining < extended) {
                slot.activate(extended)
            }
        } else if (slot.charges >= PowerUpState.MAX_CHARGES) {
            // Charge rack is full: refresh the timer instead of dropping the pickup.
            activate()
        } else {
            slot.addCharge()
        }
        soundManager.playPowerUp()
    }

    private fun handleHazard(active: ActiveElement, spike: Boolean) {
        if (damageInvulnerabilityRemaining > 0f) return

        if (isPowerActive) {
            active.isCollected = true
            soundManager.playHit()
            return
        }
        if (spike && isFireResistant) return

        if (powerUps.absorbShieldHit()) {
            damageInvulnerabilityRemaining = SHIELD_HIT_INVULNERABILITY
            soundManager.playShieldBreak()
            if (!spike) active.isCollected = true
            return
        }

        handlePlayerHit()
    }

    private fun handlePlayerHit() {
        if (gameOverHandled || levelCompletionHandled || damageInvulnerabilityRemaining > 0f) return

        soundManager.playHit()
        lives--

        if (lives <= 0) {
            gameOverHandled = true
            isRunning = false
            moveLeftPressed = false
            moveRightPressed = false
            velocityX = 0f
            velocityY = 0f
            animOverride = CharacterRenderer.AnimState.GAME_OVER
            animOverrideRemaining = Float.MAX_VALUE
            soundManager.playGameOver()
            onGameOver()
            return
        }

        animOverride = CharacterRenderer.AnimState.HIT
        animOverrideRemaining = 0.4f

        val respawn = findSafeRespawnPoint(checkpointX, checkpointY)
        playerX = respawn.first
        playerY = respawn.second
        velocityX = 0f
        velocityY = 0f
        isGrounded = true
        midAirJumpsDone = 0
        coyoteTimeRemaining = coyoteTimeSeconds
        damageInvulnerabilityRemaining = damageInvulnerabilitySeconds
    }

    /**
     * Finds a standing Y position for [x].
     *
     * Previously this used `minByOrNull { it.y }`, i.e. the HIGHEST platform
     * overlapping x, which could place the player several screens above the
     * checkpoint. It also read from `levelLayout` instead of `activeElements`,
     * so moving platforms were evaluated at their spawn position rather than
     * where they actually are. Now it prefers the lowest platform at-or-below
     * [referenceY] (the player's current feet), uses live element positions,
     * and refuses surfaces that have a spike resting on them.
     */
    private fun findSafeSpawnY(x: Float, referenceY: Float = Float.MAX_VALUE): Float {
        val left = x + colliderOffsetX
        val right = left + colliderWidth

        val solids = activeElements
            .asSequence()
            .filter { !it.isCollected && isSolid(it.original.type) }
            .filter {
                right > it.currentX &&
                    left < it.currentX + it.original.width
            }
            .toList()
        if (solids.isEmpty()) return DEFAULT_SPAWN_SURFACE_Y - visualHeight

        val referenceFeet = if (referenceY == Float.MAX_VALUE) {
            Float.MAX_VALUE
        } else {
            referenceY + colliderOffsetY + colliderHeight
        }

        // Lowest surface at or just below the reference feet; fall back to the
        // lowest surface overall when the player is below every platform.
        val support = solids
            .filter { it.currentY >= referenceFeet - SPAWN_SURFACE_SEARCH_SLACK }
            .minByOrNull { it.currentY }
            ?: solids.maxByOrNull { it.currentY }!!

        return support.currentY - colliderOffsetY - colliderHeight
    }

    /**
     * Resolves a respawn point for [x] that is not overlapping a hazard.
     *
     * Without this, a checkpoint or level start placed next to a spike would
     * respawn the player directly back into the hazard, burning all three
     * lives in a single frame each time.
     */
    private fun findSafeRespawnPoint(x: Float, referenceY: Float): Pair<Float, Float> {
        var spawnX = x
        repeat(MAX_RESPAWN_HAZARD_SEARCH) {
            val spawnY = findSafeSpawnY(spawnX, referenceY)
            if (!isSpawnBlockedByHazard(spawnX, spawnY)) return spawnX to spawnY
            // Step forward past the hazard and retry on the next surface.
            spawnX += colliderWidth
        }
        val fallbackY = findSafeSpawnY(x, referenceY)
        return x to fallbackY
    }

    /** True when a spike or enemy overlaps the standing box at [x]. */
    private fun isSpawnBlockedByHazard(x: Float, surfaceY: Float): Boolean {
        val left = x + colliderOffsetX
        val right = left + colliderWidth
        val feet = surfaceY + colliderOffsetY + colliderHeight
        val head = surfaceY + colliderOffsetY
        return activeElements.any { active ->
            if (active.isCollected) return@any false
            val type = active.original.type
            if (type != ElementType.SPIKE && type != ElementType.ENEMY) return@any false
            val overlapsX = right > active.currentX && left < active.currentX + active.original.width
            val overlapsY = active.currentY + active.original.height > head && active.currentY < feet
            overlapsX && overlapsY
        }
    }

    private fun completeLevel() {
        if (levelCompletionHandled) return
        levelCompletionHandled = true
        isPaused = true
        moveLeftPressed = false
        moveRightPressed = false
        soundManager.playLevelComplete()

        // MainActivity persists saveData in its completion callback, so commit level gems here.
        saveData.gems += gemsCollectedInLevel
        val timeSec = (System.currentTimeMillis() - levelStartTime) / 1000f
        onProgressUpdated(coinsCollectedInLevel, starsCollectedInLevel, 1f)
        onLevelCompleted(coinsCollectedInLevel, starsCollectedInLevel, timeSec)
    }

    fun triggerJump() {
        // Ignore input once the run is decided; otherwise the player could keep
        // inflating totalJumps and playing jump sounds behind the win dialog.
        if (gameOverHandled || levelCompletionHandled || isPaused) return
        when {
            isGrounded || coyoteTimeRemaining > 0f -> performGroundJump()
            midAirJumpsDone < maxMidAirJumps -> {
                velocityY = currentJumpStrength() * 0.92f
                midAirJumpsDone++
                jumpBufferRemaining = 0f
                soundManager.playJump()
                saveData.totalJumps++
            }
            else -> jumpBufferRemaining = jumpBufferSeconds
        }
    }

    private fun performGroundJump() {
        velocityY = currentJumpStrength()
        isGrounded = false
        coyoteTimeRemaining = 0f
        jumpBufferRemaining = 0f
        midAirJumpsDone = 0
        soundManager.playJump()
        saveData.totalJumps++
    }

    private fun currentJumpStrength(): Float {
        val activeHighJump = if (isHighJumpActive) 1.35f else 1f
        return baseJumpStrength * activeHighJump * jumpUpgradeMultiplier() * characterJumpMultiplier
    }

    private fun cutJumpShort() {
        if (velocityY < -5f) velocityY *= 0.55f
    }

    fun activateMagnetPowerUp() {
        powerUps.magnet.activate(magnetDuration())
        soundManager.playPowerUpActivation("MAGNET")
    }

    fun activateShieldPowerUp() {
        powerUps.activateShield(shieldDuration(), shieldHitCapacity())
        soundManager.playPowerUpActivation("SHIELD")
    }

    fun activateSpeedPowerUp() {
        powerUps.speed.activate(speedDuration())
        soundManager.playPowerUpActivation("SPEED")
    }

    fun activateHighJumpPowerUp() {
        powerUps.highJump.activate(highJumpDuration())
        soundManager.playPowerUpActivation("HIGH_JUMP")
    }

    fun activatePowerPowerUp() {
        powerUps.power.activate(POWER_DURATION)
        soundManager.playPowerUpActivation("POWER")
    }

    private fun tryUseMagnetCharge() {
        if (!isPowerUpInputAllowed()) return
        // Passive-magnet heroes always have the magnet on; spending a charge
        // would be a wasted button press.
        if (isPassiveMagnet) return
        if (powerUps.magnet.consumeChargeAndActivate(magnetDuration())) {
            soundManager.playPowerUpActivation("MAGNET")
        }
    }

    private fun tryUseSpeedCharge() {
        if (!isPowerUpInputAllowed()) return
        if (powerUps.speed.consumeChargeAndActivate(speedDuration())) {
            soundManager.playPowerUpActivation("SPEED")
        }
    }

    private fun tryUseShieldCharge() {
        if (!isPowerUpInputAllowed()) return
        if (powerUps.consumeShieldChargeAndActivate(shieldDuration(), shieldHitCapacity())) {
            soundManager.playPowerUpActivation("SHIELD")
        }
    }

    /** Power-up buttons must not fire while paused or after the run ended. */
    private fun isPowerUpInputAllowed(): Boolean =
        !isPaused && !gameOverHandled && !levelCompletionHandled

    private fun drawFrame() {
        if (!holder.surface.isValid) return
        val canvas = holder.lockCanvas() ?: return
        try {
            drawBackground(canvas)
            canvas.save()
            canvas.translate(-cameraX, 0f)
            drawWorld(canvas)
            drawParticles(canvas)
            drawPlayer(canvas)
            canvas.restore()
            drawControls(canvas)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val bg = cachedBgBitmap
        if (bg != null && !bg.isRecycled && w > 0f && h > 0f && bg.width > 0 && bg.height > 0) {
            val bgW = bg.width.toFloat()
            val bgH = bg.height.toFloat()
            val scale = maxOf(w / bgW, h / bgH)
            val scaledW = bgW * scale
            val maxOffset = (scaledW - w).coerceAtLeast(0f)
            val progress = (cameraX / levelLayout.totalWidth.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val parallaxX = -(progress * maxOffset * 0.35f)
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
        } else {
            skyPaint.shader = LinearGradient(0f, 0f, 0f, h.coerceAtLeast(1f), Color.parseColor("#1A237E"), Color.parseColor("#4FC3F7"), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w.coerceAtLeast(1f), h.coerceAtLeast(1f), skyPaint)
            skyPaint.shader = null
        }
    }

    private fun drawWorld(canvas: Canvas) {
        val viewLeft = cameraX - 140f
        val viewRight = cameraX + width + 140f

        activeElements.forEach { active ->
            if (active.isCollected) return@forEach
            val elem = active.original
            if (active.currentX + elem.width < viewLeft || active.currentX > viewRight) return@forEach
            val rect = RectF(active.currentX, active.currentY, active.currentX + elem.width, active.currentY + elem.height)

            when (elem.type) {
                ElementType.PLATFORM, ElementType.MOVING_PLATFORM -> drawPlatform(canvas, rect, elem.height)
                ElementType.BOX -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D6E63") }
                    canvas.drawRoundRect(rect, 12f, 12f, p)
                    canvas.drawRoundRect(rect, 12f, 12f, darkOutlinePaint)
                }
                ElementType.COIN -> drawCoin(canvas, active, elem)
                ElementType.STAR -> drawStar(canvas, active, elem)
                ElementType.GEM -> drawGem(canvas, active, elem)
                ElementType.POWERUP_MAGNET,
                ElementType.POWERUP_SHIELD,
                ElementType.POWERUP_SPEED,
                ElementType.POWERUP_HIGH_JUMP,
                ElementType.POWERUP_POWER -> drawPowerUp(canvas, active, elem)
                ElementType.SPIKE -> {
                    val path = Path().apply {
                        moveTo(rect.left, rect.bottom)
                        lineTo(rect.centerX(), rect.top)
                        lineTo(rect.right, rect.bottom)
                        close()
                    }
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#78909C") }
                    canvas.drawPath(path, p)
                    canvas.drawPath(path, darkOutlinePaint)
                }
                ElementType.ENEMY -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E53935") }
                    canvas.drawRoundRect(rect, 12f, 12f, p)
                    canvas.drawRoundRect(rect, 12f, 12f, darkOutlinePaint)
                }
                ElementType.CHECKPOINT -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (active.isActivated) Color.GREEN else Color.GRAY }
                    canvas.drawRect(rect.left, rect.top, rect.left + 8f, rect.bottom, darkOutlinePaint)
                    canvas.drawRect(rect.left + 8f, rect.top, rect.right, rect.top + 25f, p)
                }
                ElementType.FINISH_DOOR -> {
                    val door = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D6E63") }
                    val arch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.YELLOW
                        style = Paint.Style.STROKE
                        strokeWidth = 8f
                    }
                    canvas.drawRoundRect(rect, 20f, 20f, door)
                    canvas.drawRoundRect(rect, 20f, 20f, darkOutlinePaint)
                    canvas.drawRoundRect(rect, 20f, 20f, arch)
                }
            }
        }
    }

    private fun drawPlatform(canvas: Canvas, rect: RectF, height: Float) {
        platformPaint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, platformPaint.color, Color.parseColor("#4E342E"), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, 12f, 12f, platformPaint)
        canvas.drawRoundRect(rect, 12f, 12f, darkOutlinePaint)
        platformPaint.shader = null
        val capHeight = (height * 0.28f).coerceAtMost(16f)
        val cap = RectF(rect.left, rect.top, rect.right, rect.top + capHeight)
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cap.left, cap.top, cap.left, cap.bottom, Color.parseColor("#81C784"), Color.parseColor("#388E3C"), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(cap, 8f, 8f, capPaint)
    }

    private fun drawCoin(canvas: Canvas, active: ActiveElement, elem: LevelElement) {
        val cx = active.currentX + elem.width / 2f
        val cy = active.currentY + elem.height / 2f + (sin(animTick * 6.0) * 6.0).toFloat()
        coinPaint.shader = RadialGradient(cx, cy, elem.width * 0.7f, Color.parseColor("#FFF59D"), Color.parseColor("#F57F17"), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, elem.width / 2f, coinPaint)
        canvas.drawCircle(cx, cy, elem.width / 2f, darkOutlinePaint)
        coinPaint.shader = null
    }

    private fun drawStar(canvas: Canvas, active: ActiveElement, elem: LevelElement) {
        val cx = active.currentX + elem.width / 2f
        val cy = active.currentY + elem.height / 2f + (sin(animTick * 5.0) * 8.0).toFloat()
        val path = Path()
        val outer = elem.width / 2f
        val inner = elem.width / 4f
        for (i in 0..9) {
            val r = if (i % 2 == 0) outer else inner
            val angle = i * (Math.PI / 5) - Math.PI / 2
            val x = cx + (Math.cos(angle) * r).toFloat()
            val y = cy + (Math.sin(angle) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        starPaint.shader = RadialGradient(cx, cy, outer, Color.parseColor("#FFF59D"), Color.parseColor("#F57F17"), Shader.TileMode.CLAMP)
        canvas.drawPath(path, starPaint)
        canvas.drawPath(path, darkOutlinePaint)
        starPaint.shader = null
    }

    private fun drawGem(canvas: Canvas, active: ActiveElement, elem: LevelElement) {
        val cx = active.currentX + elem.width / 2f
        val cy = active.currentY + elem.height / 2f + (sin(animTick * 6.0) * 5.0).toFloat()
        val r = elem.width * 0.5f
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy - r * 0.2f)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy - r * 0.2f)
            close()
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r, Color.parseColor("#E040FB"), Color.parseColor("#7B1FA2"), Shader.TileMode.CLAMP)
        }
        canvas.drawPath(path, p)
        canvas.drawPath(path, darkOutlinePaint)
    }

    private fun drawPowerUp(canvas: Canvas, active: ActiveElement, elem: LevelElement) {
        val cx = active.currentX + elem.width / 2f
        val cy = active.currentY + elem.height / 2f + (sin(animTick * 7.0) * 6.0).toFloat()
        val r = elem.width * 0.5f
        val colors = when (elem.type) {
            ElementType.POWERUP_MAGNET -> "#38BDF8" to "#0284C7"
            ElementType.POWERUP_SHIELD -> "#36C96F" to "#059669"
            ElementType.POWERUP_SPEED -> "#FFD43B" to "#FF9F1C"
            ElementType.POWERUP_HIGH_JUMP -> "#A855F7" to "#7E22CE"
            else -> "#EC407A" to "#C2185B"
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx, cy - r, cx, cy + r, Color.parseColor(colors.first), Color.parseColor(colors.second), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r, p)
        canvas.drawCircle(cx, cy, r, darkOutlinePaint)

        val label = when (elem.type) {
            ElementType.POWERUP_MAGNET -> "M"
            ElementType.POWERUP_SHIELD -> "S"
            ElementType.POWERUP_SPEED -> "⚡"
            ElementType.POWERUP_HIGH_JUMP -> "J"
            else -> "P"
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = r * 1.1f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(label, cx, cy + text.textSize * 0.35f, text)
    }

    private fun drawParticles(canvas: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        trailParticles.forEach { particle ->
            p.color = particle.color
            p.alpha = particle.alpha.coerceIn(0, 255)
            canvas.drawCircle(particle.x, particle.y, particle.size, p)
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        // Blink while invulnerable after respawn.
        if (damageInvulnerabilityRemaining > 0f && ((damageInvulnerabilityRemaining * 12f).toInt() and 1) == 0) return

        val bounds = RectF(playerX, playerY, playerX + visualWidth, playerY + visualHeight)
        val animState = when {
            animOverride != null -> animOverride!!
            !isGrounded && midAirJumpsDone > 0 && velocityY < 0f -> CharacterRenderer.AnimState.DOUBLE_JUMP
            !isGrounded && velocityY < 0f -> CharacterRenderer.AnimState.JUMP
            !isGrounded -> CharacterRenderer.AnimState.FALL
            abs(velocityX) > 0.4f -> CharacterRenderer.AnimState.RUN
            else -> CharacterRenderer.AnimState.IDLE
        }

        canvas.save()
        canvas.scale(1.28f, 1.28f, bounds.centerX(), bounds.bottom)
        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = bounds,
            characterId = saveData.selectedCharacter,
            skinId = saveData.selectedSkin,
            facingRight = isFacingRight,
            animState = animState,
            animTime = animTick
        )
        canvas.restore()

        if (isPowerActive) {
            val aura = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#60FFD700") }
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), visualHeight * 0.82f + (sin(animTick * 12.0) * 8.0).toFloat(), aura)
        }

        if (isShieldActive) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4000E6FF") }
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CC00E5FF")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), visualHeight * 0.75f, fill)
            canvas.drawCircle(bounds.centerX(), bounds.centerY(), visualHeight * 0.75f, border)
        }
    }

    private fun drawControls(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (leftButtonRect.isEmpty) updateControlLayouts(w.toInt(), h.toInt())

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        drawControlCircle(canvas, leftButtonRect, moveLeftPressed, false, Color.parseColor("#42D9FF"), Color.parseColor("#0879D7"), null)
        drawArrow(canvas, leftButtonRect, right = false)

        drawControlCircle(canvas, rightButtonRect, moveRightPressed, false, Color.parseColor("#42D9FF"), Color.parseColor("#0879D7"), null)
        drawArrow(canvas, rightButtonRect, right = true)

        drawControlCircle(canvas, jumpButtonRect, false, false, Color.parseColor("#4DE3B0"), Color.parseColor("#078D6D"), null)
        textPaint.textSize = minOf(jumpButtonRect.width() * 0.22f, 26f)
        canvas.drawText("JUMP", jumpButtonRect.centerX(), jumpButtonRect.centerY() + textPaint.textSize * 0.34f, textPaint)

        val magnetBadge = when {
            isMagnetActive -> String.format("%.1fs", magnetTimeRemaining)
            isPassiveMagnet -> "PASSIVE"
            magnetCharges > 0 -> "x$magnetCharges"
            else -> "0"
        }
        val magnetUsable = isMagnetActive || isPassiveMagnet || magnetCharges > 0
        drawControlCircle(canvas, magnetButtonRect, false, isMagnetActive || isPassiveMagnet, if (magnetUsable) Color.parseColor("#38BDF8") else Color.parseColor("#64748B"), Color.parseColor("#0284C7"), magnetBadge)
        drawMagnetIcon(canvas)

        val speedBadge = when {
            isSpeedActive -> String.format("%.1fs", speedTimeRemaining)
            speedCharges > 0 -> "x$speedCharges"
            else -> "0"
        }
        // A hero with an innate speed bonus cannot spend a charge on anything the
        // button would add, so the button is only "usable" when a charge exists.
        val speedUsable = isSpeedActive || speedCharges > 0
        drawControlCircle(canvas, speedButtonRect, false, isSpeedActive, if (speedUsable) Color.parseColor("#FFD43B") else Color.parseColor("#64748B"), Color.parseColor("#FF9F1C"), speedBadge)
        drawSpeedIcon(canvas)

        val shieldBadge = when {
            isShieldActive -> if (shieldHitsRemaining > 1) "${shieldHitsRemaining}x ${shieldTimeRemaining.toInt()}s" else String.format("%.1fs", shieldTimeRemaining)
            shieldCharges > 0 -> "x$shieldCharges"
            else -> "0"
        }
        val shieldUsable = isShieldActive || shieldCharges > 0
        drawControlCircle(canvas, shieldButtonRect, false, isShieldActive, if (shieldUsable) Color.parseColor("#36C96F") else Color.parseColor("#64748B"), Color.parseColor("#059669"), shieldBadge)
        drawShieldIcon(canvas)

        drawTimedBuffs(canvas, textPaint)
    }

    /**
     * High Jump and Power have no touch button, so without this strip the player
     * had no feedback at all that they were running. Drawn as compact pills in
     * the control row, above the power-up column.
     */
    private fun drawTimedBuffs(canvas: Canvas, textPaint: Paint) {
        val buffs = mutableListOf<Triple<String, Float, Int>>()
        if (isHighJumpActive) {
            buffs.add(Triple("HIGH JUMP", highJumpTimeRemaining, Color.parseColor("#A855F7")))
        }
        if (isPowerActive) {
            buffs.add(Triple("POWER", powerTimeRemaining, Color.parseColor("#FFB300")))
        }
        if (buffs.isEmpty()) return

        val density = resources.displayMetrics.density
        val pillHeight = 26f * density
        val gap = 5f * density
        val badgeX = magnetButtonRect.left
        var y = shieldButtonRect.top - gap - pillHeight

        textPaint.textSize = 12f * density
        textPaint.textAlign = Paint.Align.LEFT

        buffs.asReversed().forEach { (label, remaining, color) ->
            val valueText = String.format("%.1fs", remaining.coerceAtLeast(0f))
            val labelWidth = textPaint.measureText(label)
            val valueWidth = textPaint.measureText(valueText)
            val pillWidth = labelWidth + valueWidth + 20f * density

            controlBuffBgPaint.color = color
            controlBuffBgPaint.alpha = 220
            reusableRect.set(badgeX, y, badgeX + pillWidth, y + pillHeight)
            canvas.drawRoundRect(reusableRect, pillHeight / 2f, pillHeight / 2f, controlBuffBgPaint)

            textPaint.color = Color.WHITE
            canvas.drawText(label, badgeX + 9f * density, y + pillHeight * 0.68f, textPaint)
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText(valueText, badgeX + 9f * density + labelWidth, y + pillHeight * 0.68f, textPaint)
            y -= (pillHeight + gap)
        }

        textPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawControlCircle(
        canvas: Canvas,
        rect: RectF,
        pressed: Boolean,
        active: Boolean,
        topColor: Int,
        bottomColor: Int,
        badge: String?
    ) {
        val radius = minOf(rect.width(), rect.height()) * 0.5f
        val cx = rect.centerX()
        val cy = rect.centerY() - if (pressed) 1f else 0f
        canvas.drawCircle(cx, rect.centerY() + if (pressed) 2f else 6f, radius - 1f, controlShadowPaint)

        if (active) {
            controlGlowPaint.color = topColor
            controlGlowPaint.alpha = 105
            controlGlowPaint.strokeWidth = 6f
            canvas.drawCircle(cx, cy, radius + 4f + (sin(animTick * 8.0) * 3.0).toFloat(), controlGlowPaint)
        }

        controlFillPaint.shader = LinearGradient(rect.left, rect.top, rect.left, rect.bottom, if (active) Color.WHITE else topColor, bottomColor, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius - 2f, controlFillPaint)
        controlFillPaint.shader = null

        controlBorderPaint.color = Color.WHITE
        controlBorderPaint.alpha = 230
        controlBorderPaint.strokeWidth = if (active) 5f else 3.5f
        canvas.drawCircle(cx, cy, radius - 2f, controlBorderPaint)

        if (!badge.isNullOrEmpty()) {
            val badgeW = radius * 1.55f
            val badgeH = radius * 0.55f
            val badgeRect = RectF(cx - badgeW / 2f, cy + radius * 0.42f, cx + badgeW / 2f, cy + radius * 0.97f)
            controlBadgeTextPaint.color = if (active) Color.parseColor("#FFD43B") else Color.WHITE
            controlBadgeTextPaint.textSize = badgeH * 0.70f
            canvas.drawRoundRect(badgeRect, 8f, 8f, controlBadgeBgPaint)
            canvas.drawText(badge, cx, badgeRect.centerY() + controlBadgeTextPaint.textSize * 0.35f, controlBadgeTextPaint)
        }
    }

    private fun drawArrow(canvas: Canvas, rect: RectF, right: Boolean) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val s = rect.width() * 0.22f
        reusablePath.reset()
        if (right) {
            reusablePath.moveTo(cx - s * 0.8f, cy - s)
            reusablePath.lineTo(cx + s * 0.9f, cy)
            reusablePath.lineTo(cx - s * 0.8f, cy + s)
        } else {
            reusablePath.moveTo(cx + s * 0.8f, cy - s)
            reusablePath.lineTo(cx - s * 0.9f, cy)
            reusablePath.lineTo(cx + s * 0.8f, cy + s)
        }
        reusablePath.close()
        canvas.drawPath(reusablePath, controlIconPaint)
    }

    private fun drawMagnetIcon(canvas: Canvas) {
        controlMagnetStrokePaint.strokeWidth = magnetButtonRect.width() * 0.12f
        val cx = magnetButtonRect.centerX()
        val cy = magnetButtonRect.centerY()
        val s = magnetButtonRect.width() * 0.26f
        canvas.drawArc(RectF(cx - s, cy - s, cx + s, cy + s * 0.5f), 180f, 180f, false, controlMagnetStrokePaint)
        canvas.drawLine(cx - s, cy, cx - s, cy + s * 0.5f, controlMagnetStrokePaint)
        canvas.drawLine(cx + s, cy, cx + s, cy + s * 0.5f, controlMagnetStrokePaint)
    }

    private fun drawSpeedIcon(canvas: Canvas) {
        val cx = speedButtonRect.centerX()
        val cy = speedButtonRect.centerY()
        val s = speedButtonRect.width() * 0.28f
        reusablePath.reset()
        reusablePath.moveTo(cx - s * 0.4f, cy + s * 0.6f)
        reusablePath.lineTo(cx + s * 0.1f, cy + s * 0.05f)
        reusablePath.lineTo(cx - s * 0.1f, cy + s * 0.05f)
        reusablePath.lineTo(cx + s * 0.4f, cy - s * 0.6f)
        reusablePath.lineTo(cx - s * 0.1f, cy - s * 0.05f)
        reusablePath.lineTo(cx + s * 0.1f, cy - s * 0.05f)
        reusablePath.close()
        canvas.drawPath(reusablePath, controlIconPaint)
    }

    private fun drawShieldIcon(canvas: Canvas) {
        val cx = shieldButtonRect.centerX()
        val cy = shieldButtonRect.centerY()
        val s = shieldButtonRect.width() * 0.28f
        reusablePath.reset()
        reusablePath.moveTo(cx, cy - s * 0.65f)
        reusablePath.lineTo(cx + s * 0.55f, cy - s * 0.35f)
        reusablePath.lineTo(cx + s * 0.45f, cy + s * 0.35f)
        reusablePath.quadTo(cx, cy + s * 0.7f, cx - s * 0.45f, cy + s * 0.35f)
        reusablePath.lineTo(cx - s * 0.55f, cy - s * 0.35f)
        reusablePath.close()
        canvas.drawPath(reusablePath, controlIconPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // While paused or after the run ended the overlay owns input. Returning
        // true consumes the event so it cannot fall through to the Activity.
        if (isPaused || gameOverHandled || levelCompletionHandled) {
            moveLeftPressed = false
            moveRightPressed = false
            activeJumpPointers.clear()
            return true
        }

        val actionIndex = event.actionIndex
        val actionMasked = event.actionMasked

        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = event.getPointerId(actionIndex)
                val x = event.getX(actionIndex)
                val y = event.getY(actionIndex)

                when {
                    jumpButtonRect.contains(x, y) -> {
                        if (activeJumpPointers.add(pointerId)) triggerJump()
                    }
                    magnetButtonRect.contains(x, y) -> tryUseMagnetCharge()
                    speedButtonRect.contains(x, y) -> tryUseSpeedCharge()
                    shieldButtonRect.contains(x, y) -> tryUseShieldCharge()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(actionIndex)
                val wasJumpPointer = activeJumpPointers.remove(pointerId)
                if (wasJumpPointer && activeJumpPointers.isEmpty()) cutJumpShort()
            }

            MotionEvent.ACTION_CANCEL -> {
                activeJumpPointers.clear()
                moveLeftPressed = false
                moveRightPressed = false
                cutJumpShort()
                return true
            }
        }

        var left = false
        var right = false
        for (i in 0 until event.pointerCount) {
            if ((actionMasked == MotionEvent.ACTION_POINTER_UP || actionMasked == MotionEvent.ACTION_UP) && i == actionIndex) continue
            val x = event.getX(i)
            val y = event.getY(i)
            if (leftButtonRect.contains(x, y)) left = true
            if (rightButtonRect.contains(x, y)) right = true
        }
        moveLeftPressed = left
        moveRightPressed = right
        return true
    }
}
