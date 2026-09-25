package com.jumpadventure.game

import android.graphics.*
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jumpadventure.game.audio.SoundManager
import com.jumpadventure.game.data.SaveManager
import com.jumpadventure.game.engine.GameView
import com.jumpadventure.game.level.WorldRepository
import com.jumpadventure.game.model.*

class MainActivity : AppCompatActivity() {

    private val overlayHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private lateinit var saveManager: SaveManager
    private lateinit var saveData: GameSaveData
    private lateinit var soundManager: SoundManager

    private var currentGameView: GameView? = null

    // Layout Containers
    private lateinit var incMainMenu: View
    private lateinit var incLevelMap: View
    private lateinit var incGameplay: View
    private lateinit var incSecondary: View
    private lateinit var incOverlay: View

    // Main Menu Views
    private lateinit var tvCoins: TextView
    private lateinit var tvGems: TextView
    private lateinit var btnPlay: com.jumpadventure.game.graphics.GamePrimaryButton
    private lateinit var charPreviewView: com.jumpadventure.game.graphics.CharacterPreviewView

    private var currentScreenName: String = "MAIN_MENU"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.jumpadventure.game.util.InsetsManager.setupEdgeToEdge(this)

        setContentView(R.layout.activity_main)

        saveManager = SaveManager.getInstance(this)
        saveData = saveManager.loadData()

        soundManager = SoundManager(this).apply {
            soundEnabled = saveData.soundEnabled
            musicEnabled = saveData.musicEnabled
        }

        initViews()
        setupMainMenuListeners()
        updateCurrencyHUD()
        showScreen("MAIN_MENU")
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (incOverlay.visibility == View.VISIBLE) {
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            return
        }
        if (currentScreenName != "MAIN_MENU") {
            showScreen("MAIN_MENU")
            return
        }
        super.onBackPressed()
    }

    private fun initViews() {
        incMainMenu = findViewById(R.id.incMainMenu)
        incLevelMap = findViewById(R.id.incLevelMap)
        incGameplay = findViewById(R.id.incGameplay)
        incSecondary = findViewById(R.id.incSecondary)
        incOverlay = findViewById(R.id.incOverlay)

        setupWindowInsets()

        tvCoins = incMainMenu.findViewById(R.id.tvCoins)
        tvGems = incMainMenu.findViewById(R.id.tvGems)
        btnPlay = incMainMenu.findViewById(R.id.btnPlay)
        charPreviewView = incMainMenu.findViewById(R.id.charPreviewView)
        setupResponsiveHomeLayout()
    }

    private fun setupResponsiveHomeLayout() {
        val container = incMainMenu.findViewById<FrameLayout>(R.id.homeContentContainer)
        val logo = incMainMenu.findViewById<ImageView>(R.id.ivHomeLogo)
        val mountain = incMainMenu.findViewById<ImageView>(R.id.ivMountainPlatform)
        val character = incMainMenu.findViewById<com.jumpadventure.game.graphics.CharacterPreviewView>(R.id.charPreviewView)
        val play = incMainMenu.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnPlay)
        val density = resources.displayMetrics.density
        val dp = { value: Float -> (value * density).toInt() }

        fun applyLayout() {
            val w = container.width
            val h = container.height
            if (w <= 0 || h <= 0) return

            val logoH = (h * 0.18f).toInt().coerceIn(dp(100f), dp(135f))
            val logoW = minOf(dp(290f), (w * 0.82f).toInt())

            logo.layoutParams = FrameLayout.LayoutParams(logoW, logoH).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dp(4f)
            }

            val playH = dp(78f).coerceIn(dp(68f), (h * 0.12f).toInt())
            val playW = minOf(dp(320f), w - dp(28f))

            play.layoutParams = FrameLayout.LayoutParams(playW, playH).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(4f)
            }

            // Move mountain slightly lower (~25-35dp lower than original)
            val mountainW = minOf(dp(250f), (w * 0.70f).toInt())
            val mountainRatio = 3264f / 2857f
            val mountainH = (mountainW * mountainRatio).toInt().coerceAtMost((h * 0.40f).toInt())

            // Shift mountain downward slightly
            val mountainBottom = h + dp(18f)
            val mountainTop = (mountainBottom - mountainH).coerceAtLeast(logoH + dp(100f))

            mountain.layoutParams = FrameLayout.LayoutParams(mountainW, mountainH).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = mountainTop
            }

            // Character feet sit directly on the top grass surface of the mountain (no gap)
            val characterW = minOf(dp(160f), (w * 0.45f).toInt())
            val characterH = minOf(dp(180f), (h * 0.23f).toInt())
            // Top grass surface is ~16% down from asset top
            val mountainSurfaceY = mountainTop + (mountainH * 0.16f).toInt()
            val charTopMargin = (mountainSurfaceY - characterH + dp(10f)).toInt().coerceAtLeast(logoH + dp(4f))

            character.layoutParams = FrameLayout.LayoutParams(characterW, characterH).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = charTopMargin
            }
        }

        container.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            kotlin.runCatching { applyLayout() }
        }
        container.post { kotlin.runCatching { applyLayout() } }
    }

    private fun setupWindowInsets() {
        val root = findViewById<View>(R.id.rootLayout) ?: return

        val topViews = listOfNotNull(
            incMainMenu.findViewById<View>(R.id.topBar),
            incLevelMap.findViewById<View>(R.id.mapTopBar),
            incGameplay.findViewById<View>(R.id.hudTopBar),
            incSecondary.findViewById<View>(R.id.secondaryTopBar)
        )

        val bottomMarginViews = listOfNotNull(
            incMainMenu.findViewById<View>(R.id.bottomNavContainer)
        )

        val bottomPadViews = listOfNotNull(
            incSecondary.findViewById<View>(R.id.secondaryScrollView),
            incLevelMap.findViewById<View>(R.id.mapScrollView)
        )

        val overlayViews = listOfNotNull(
            incOverlay
        )

        com.jumpadventure.game.util.InsetsManager.applySystemWindowInsets(
            rootView = root,
            topViewsToPad = topViews,
            bottomViewsToMargin = bottomMarginViews,
            bottomViewsToPad = bottomPadViews,
            overlayViewsToPad = overlayViews
        )
    }

    private fun updateCurrencyHUD() {
        tvCoins.text = "${saveData.coins}"
        tvGems.text = "${saveData.gems}"
        btnPlay.setPlayInfo(saveData.currentLevel)
        charPreviewView.selectedCharacterId = saveData.selectedCharacter
    }

    private fun setupMainMenuListeners() {
        incMainMenu.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            soundManager.playButtonClick()
            openSettingsScreen()
        }

        btnPlay.setOnClickListener {
            soundManager.playButtonClick()
            openLevelMapScreen()
        }

        incMainMenu.findViewById<View>(R.id.navShop).setOnClickListener {
            soundManager.playButtonClick()
            openShopScreen()
        }

        incMainMenu.findViewById<View>(R.id.navCharacters).setOnClickListener {
            soundManager.playButtonClick()
            openCharactersScreen()
        }

        incMainMenu.findViewById<View>(R.id.navWorlds).setOnClickListener {
            soundManager.playButtonClick()
            openWorldsScreen()
        }

        incMainMenu.findViewById<View>(R.id.navAchievements).setOnClickListener {
            soundManager.playButtonClick()
            openAchievementsScreen()
        }
    }

    private fun updateBottomNavSelection(activeTab: String) {
        val navItems = mapOf(
            "SHOP" to Pair(incMainMenu.findViewById<View>(R.id.navShop), Pair(incMainMenu.findViewById<ImageView>(R.id.ivNavShop), incMainMenu.findViewById<TextView>(R.id.tvNavShop))),
            "CHARACTERS" to Pair(incMainMenu.findViewById<View>(R.id.navCharacters), Pair(incMainMenu.findViewById<ImageView>(R.id.ivNavCharacters), incMainMenu.findViewById<TextView>(R.id.tvNavCharacters))),
            "WORLDS" to Pair(incMainMenu.findViewById<View>(R.id.navWorlds), Pair(incMainMenu.findViewById<ImageView>(R.id.ivNavWorlds), incMainMenu.findViewById<TextView>(R.id.tvNavWorlds))),
            "ACHIEVEMENTS" to Pair(incMainMenu.findViewById<View>(R.id.navAchievements), Pair(incMainMenu.findViewById<ImageView>(R.id.ivNavAchievements), incMainMenu.findViewById<TextView>(R.id.tvNavAchievements)))
        )

        navItems.forEach { (key, views) ->
            val container = views.first ?: return@forEach
            val icon = views.second.first ?: return@forEach
            val text = views.second.second ?: return@forEach

            if (key == activeTab) {
                container.setBackgroundResource(R.drawable.bg_nav_active_capsule)
                icon.setColorFilter(Color.WHITE)
                text.setTextColor(Color.WHITE)
            } else {
                container.background = null
                icon.setColorFilter(Color.parseColor("#1F3045"))
                text.setTextColor(Color.parseColor("#1F3045"))
            }
        }
    }

    private fun showScreen(screenName: String) {
        currentScreenName = screenName
        incMainMenu.visibility = View.GONE
        incLevelMap.visibility = View.GONE
        incGameplay.visibility = View.GONE
        incSecondary.visibility = View.GONE
        incOverlay.visibility = View.GONE
        overlayHandler.removeCallbacksAndMessages(null)

        when (screenName) {
            "MAIN_MENU" -> {
                updateCurrencyHUD()
                updateBottomNavSelection("")
                incMainMenu.visibility = View.VISIBLE
            }
            "LEVEL_MAP" -> incLevelMap.visibility = View.VISIBLE
            "GAMEPLAY" -> incGameplay.visibility = View.VISIBLE
            "SECONDARY" -> incSecondary.visibility = View.VISIBLE
            "OVERLAY" -> incOverlay.visibility = View.VISIBLE
        }
    }

    /* ------------------------------------------------------------------------
     * LEVEL MAP SCREEN
     * ------------------------------------------------------------------------ */
    private fun openLevelMapScreen() {
        val currentWorld = WorldRepository.getWorldForLevel(saveData.currentLevel)
        incLevelMap.findViewById<TextView>(R.id.tvMapWorldTitle).text = "WORLD ${currentWorld.id}"
        incLevelMap.findViewById<TextView>(R.id.tvMapWorldSub).text = currentWorld.name

        incLevelMap.findViewById<ImageButton>(R.id.btnMapBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incLevelMap.findViewById<FrameLayout>(R.id.mapNodesContainer)
        container.removeAllViews()

        val mapView = com.jumpadventure.game.graphics.LevelMapView(this).apply {
            selectedCharacterId = saveData.selectedCharacter
            setupMap(
                worldInfo = currentWorld,
                highestLevel = saveData.highestLevel,
                currentLevel = saveData.currentLevel,
                levelStars = saveData.levelStars
            )
            onNodeClicked = { levelNum ->
                soundManager.playButtonClick()
                startLevelGameplay(levelNum)
            }
        }

        container.addView(mapView)

        showScreen("LEVEL_MAP")

        val scrollView = incLevelMap.findViewById<ScrollView>(R.id.mapScrollView)
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    /* ------------------------------------------------------------------------
     * GAMEPLAY ENGINE STARTER
     * ------------------------------------------------------------------------ */
    private fun startLevelGameplay(levelNum: Int) {
        android.util.Log.d("JUMP_DEBUG", "GAMEPLAY_START: levelNum=$levelNum")
        saveData.currentLevel = levelNum
        saveManager.saveData(saveData)

        val gameContainer = incGameplay.findViewById<FrameLayout>(R.id.gameViewContainer)
        currentGameView?.stopGameLoop()
        currentGameView = null
        gameContainer.removeAllViews()

        val tvHudCoins = incGameplay.findViewById<TextView>(R.id.tvHudCoins)
        val tvHudStars = incGameplay.findViewById<TextView>(R.id.tvHudStars)
        val tvHudLevel = incGameplay.findViewById<TextView>(R.id.tvHudLevel)
        val pbLevelProgress = incGameplay.findViewById<ProgressBar>(R.id.pbLevelProgress)

        tvHudLevel.text = "LEVEL $levelNum"
        tvHudCoins.text = "0"
        tvHudStars.text = "0"
        pbLevelProgress.progress = 0

        val currentWorld = WorldRepository.getWorldForLevel(levelNum)
        android.util.Log.d("JUMP_DEBUG", "LEVEL_ID=$levelNum, WORLD_ID=${currentWorld.id}, BACKGROUND=${WorldRepository.getWorldBackgroundRes(currentWorld.id)}, CHARACTER=${saveData.selectedCharacter}")

        val newGameView = GameView(
            context = this,
            saveData = saveData,
            soundManager = soundManager,
            onLevelCompleted = { coins, stars, timeSec ->
                runOnUiThread { handleLevelCompleted(coins, stars, timeSec) }
            },
            onGameOver = {
                runOnUiThread { handleGameOver() }
            },
            onPauseClicked = {
                runOnUiThread { showPauseOverlay() }
            },
            onProgressUpdated = { coins, stars, progress ->
                runOnUiThread {
                    tvHudCoins.text = "$coins"
                    tvHudStars.text = "$stars"
                    pbLevelProgress.progress = (progress * 100).toInt()
                }
            }
        )

        android.util.Log.d("JUMP_DEBUG", "GAMEVIEW_CREATED")
        currentGameView = newGameView
        gameContainer.addView(
            newGameView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        android.util.Log.d("JUMP_DEBUG", "GAMEVIEW_ATTACHED")

        incGameplay.findViewById<ImageButton>(R.id.btnPause).setOnClickListener {
            soundManager.playButtonClick()
            showPauseOverlay()
        }

        val magnetButton = incGameplay.findViewById<com.jumpadventure.game.graphics.GamePowerUpButton>(R.id.btnPowerUpMagnet)
        val speedButton = incGameplay.findViewById<com.jumpadventure.game.graphics.GamePowerUpButton>(R.id.btnPowerUpSpeed)
        val shieldButton = incGameplay.findViewById<com.jumpadventure.game.graphics.GamePowerUpButton>(R.id.btnPowerUpShield)

        magnetButton.type = com.jumpadventure.game.graphics.GamePowerUpButton.Type.MAGNET
        speedButton.type = com.jumpadventure.game.graphics.GamePowerUpButton.Type.SPEED
        shieldButton.type = com.jumpadventure.game.graphics.GamePowerUpButton.Type.SHIELD

        magnetButton.setOnClickListener {
            soundManager.playButtonClick()
            currentGameView?.activateMagnetPowerUp()
            magnetButton.active = true
        }
        speedButton.setOnClickListener {
            soundManager.playButtonClick()
            currentGameView?.activateSpeedPowerUp()
            speedButton.active = true
        }
        shieldButton.setOnClickListener {
            soundManager.playButtonClick()
            currentGameView?.activateShieldPowerUp()
            shieldButton.active = true
        }

        showScreen("GAMEPLAY")
    }

    private fun applyOverlayResponsiveSizing(isLevelComplete: Boolean) {
        val displayMetrics = resources.displayMetrics
        val windowWidth = displayMetrics.widthPixels
        val windowHeight = displayMetrics.heightPixels

        val insets = androidx.core.view.ViewCompat.getRootWindowInsets(findViewById(R.id.rootLayout))
        val topInset = insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())?.top ?: 0
        val bottomInset = insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        val usableHeight = windowHeight - topInset - bottomInset

        val density = displayMetrics.density
        val targetWidth = (windowWidth * 0.88f).toInt().coerceIn((290 * density).toInt(), (520 * density).toInt())
        val isCompact = usableHeight < (720 * density).toInt()

        val boardContainer = incOverlay.findViewById<FrameLayout>(R.id.overlayBoardContainer)
        boardContainer.layoutParams = (boardContainer.layoutParams as RelativeLayout.LayoutParams).apply {
            width = targetWidth
            height = RelativeLayout.LayoutParams.WRAP_CONTENT
            addRule(RelativeLayout.CENTER_IN_PARENT)
        }

        val contentLayout = incOverlay.findViewById<LinearLayout>(R.id.overlayContent)
        val padStartEnd = (20 * density).toInt()
        val padTop = if (isCompact) (28 * density).toInt() else (36 * density).toInt()
        val padBottom = if (isCompact) (10 * density).toInt() else (14 * density).toInt()
        contentLayout.setPadding(padStartEnd, padTop, padStartEnd, padBottom)

        val crownView = incOverlay.findViewById<ImageView>(R.id.ivOverlayCrown)
        val crownSize = if (isCompact) (40 * density).toInt() else (52 * density).toInt()
        crownView.layoutParams = (crownView.layoutParams as LinearLayout.LayoutParams).apply {
            width = crownSize
            height = crownSize
        }

        val curvedTitle = incOverlay.findViewById<com.jumpadventure.game.graphics.CurvedTitleView>(R.id.tvOverlayCurvedTitle)
        val titleH = if (isCompact) (34 * density).toInt() else (42 * density).toInt()
        curvedTitle.layoutParams = (curvedTitle.layoutParams as LinearLayout.LayoutParams).apply {
            width = (targetWidth * 0.85f).toInt()
            height = titleH
        }

        val stars3D = incOverlay.findViewById<com.jumpadventure.game.graphics.Stars3DView>(R.id.vOverlayStars3D)
        val starsH = if (isCompact) (36 * density).toInt() else (44 * density).toInt()
        stars3D.layoutParams = (stars3D.layoutParams as LinearLayout.LayoutParams).apply {
            width = (targetWidth * 0.65f).toInt()
            height = starsH
            setMargins(0, (2 * density).toInt(), 0, (4 * density).toInt())
        }

        val rewardSummary = incOverlay.findViewById<com.jumpadventure.game.graphics.RewardSummaryView>(R.id.vOverlayRewardSummary)
        val rewardH = if (isCompact) (48 * density).toInt() else (56 * density).toInt()
        rewardSummary.layoutParams = (rewardSummary.layoutParams as LinearLayout.LayoutParams).apply {
            width = LinearLayout.LayoutParams.MATCH_PARENT
            height = rewardH
            setMargins(0, 0, 0, if (isCompact) (8 * density).toInt() else (12 * density).toInt())
        }

        val primaryBtn = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        val primaryH = if (isCompact) (42 * density).toInt() else (48 * density).toInt()
        primaryBtn.layoutParams = (primaryBtn.layoutParams as LinearLayout.LayoutParams).apply {
            width = LinearLayout.LayoutParams.MATCH_PARENT
            height = primaryH
            setMargins(0, 0, 0, if (isCompact) (4 * density).toInt() else (8 * density).toInt())
        }

        val secondaryBtn = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary)
        val homeBtn = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome)
        val secH = if (isCompact) (40 * density).toInt() else (44 * density).toInt()

        secondaryBtn.layoutParams = (secondaryBtn.layoutParams as LinearLayout.LayoutParams).apply {
            width = 0
            height = secH
            weight = 1f
            setMargins(0, 0, (4 * density).toInt(), 0)
        }
        homeBtn.layoutParams = (homeBtn.layoutParams as LinearLayout.LayoutParams).apply {
            width = 0
            height = secH
            weight = 1f
            setMargins((4 * density).toInt(), 0, 0, 0)
        }

        stars3D.visibility = if (isLevelComplete) View.VISIBLE else View.GONE
        rewardSummary.visibility = if (isLevelComplete) View.VISIBLE else View.GONE
    }

    private fun handleLevelCompleted(coinsEarned: Int, starsEarned: Int, timeTakenSec: Float) {
        saveData.coins += coinsEarned
        saveData.totalCoinsCollected += coinsEarned
        saveData.totalLevelsCompleted++
        val prevStars = saveData.levelStars[saveData.currentLevel] ?: 0
        if (starsEarned > prevStars) saveData.levelStars[saveData.currentLevel] = starsEarned
        if (saveData.currentLevel >= saveData.highestLevel) saveData.highestLevel = saveData.currentLevel + 1
        saveManager.saveData(saveData)

        incOverlay.visibility = View.VISIBLE
        incOverlay.bringToFront()

        val customDialog = incOverlay.findViewById<com.jumpadventure.game.graphics.GameDialogView>(R.id.customGameDialog)
        customDialog.visibility = View.GONE

        val boardContainer = incOverlay.findViewById<FrameLayout>(R.id.overlayBoardContainer)
        boardContainer.visibility = View.VISIBLE
        boardContainer.bringToFront()

        applyOverlayResponsiveSizing(isLevelComplete = true)

        val board = incOverlay.findViewById<ImageView>(R.id.ivPauseBoard)
        val curvedTitle = incOverlay.findViewById<com.jumpadventure.game.graphics.CurvedTitleView>(R.id.tvOverlayCurvedTitle)
        val sub = incOverlay.findViewById<TextView>(R.id.tvOverlaySub)
        val stars3D = incOverlay.findViewById<com.jumpadventure.game.graphics.Stars3DView>(R.id.vOverlayStars3D)
        val rewardSummary = incOverlay.findViewById<com.jumpadventure.game.graphics.RewardSummaryView>(R.id.vOverlayRewardSummary)

        val primary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        val secondary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary)
        val home = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome)

        board.setImageResource(R.drawable.winner_bg)
        curvedTitle.titleText = "LEVEL COMPLETE!"
        sub.text = "LEVEL ${saveData.currentLevel}"

        stars3D.starsEarned = starsEarned
        rewardSummary.setRewardData(coinsEarned, starsEarned, timeTakenSec)

        primary.visibility = View.VISIBLE
        secondary.visibility = View.VISIBLE
        home.visibility = View.VISIBLE

        primary.variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN
        secondary.variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
        home.variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.ORANGE

        primary.iconType = com.jumpadventure.game.graphics.GamePrimaryButton.IconType.PLAY
        secondary.iconType = com.jumpadventure.game.graphics.GamePrimaryButton.IconType.RESTART
        home.iconType = com.jumpadventure.game.graphics.GamePrimaryButton.IconType.HOME

        primary.mainText = "NEXT LEVEL"; primary.subText = ""
        secondary.mainText = "RESTART"; secondary.subText = ""
        home.mainText = "HOME"; home.subText = ""

        var isActionClicked = false

        primary.setOnClickListener {
            if (isActionClicked) return@setOnClickListener
            isActionClicked = true
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            startLevelGameplay(saveData.currentLevel + 1)
        }
        secondary.setOnClickListener {
            if (isActionClicked) return@setOnClickListener
            isActionClicked = true
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            startLevelGameplay(saveData.currentLevel)
        }
        home.setOnClickListener {
            if (isActionClicked) return@setOnClickListener
            isActionClicked = true
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            currentGameView?.stopGameLoop()
            showScreen("MAIN_MENU")
        }

        // Remove the unused separate hero/logo slot; the board artwork already contains the decoration.
        incOverlay.findViewById<android.widget.ImageView>(R.id.ivOverlayCrown).visibility = View.GONE

        // Trigger entrance animation & sparkles
        boardContainer.scaleX = 0.92f
        boardContainer.scaleY = 0.92f
        boardContainer.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(220)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        incOverlay.findViewById<com.jumpadventure.game.graphics.SparkleView>(R.id.sparkleOverlay).triggerBurst()
    }

    private fun handleGameOver() {
        // Freeze the failed run before opening the dialog so the game surface
        // cannot keep updating underneath and repeatedly recreate the dialog.
        currentGameView?.stopGameLoop()
        showCustomGameDialog(
            title = "GAME OVER",
            message = "You didn't make it on Level ${saveData.currentLevel}!",
            primaryBtnText = "RETRY",
            secondaryBtnText = "HOME",
            onConfirm = {
                startLevelGameplay(saveData.currentLevel)
            },
            onCancel = {
                currentGameView?.stopGameLoop()
                showScreen("MAIN_MENU")
            }
        )
    }

    private fun showPauseOverlay() {
        currentGameView?.pauseGame()
        incOverlay.visibility = View.VISIBLE
        incOverlay.bringToFront()

        val customDialog = incOverlay.findViewById<com.jumpadventure.game.graphics.GameDialogView>(R.id.customGameDialog)
        customDialog.visibility = View.GONE

        val boardContainer = incOverlay.findViewById<FrameLayout>(R.id.overlayBoardContainer)
        boardContainer.visibility = View.VISIBLE
        boardContainer.bringToFront()

        applyOverlayResponsiveSizing(isLevelComplete = false)

        val board = incOverlay.findViewById<ImageView>(R.id.ivPauseBoard)
        val curvedTitle = incOverlay.findViewById<com.jumpadventure.game.graphics.CurvedTitleView>(R.id.tvOverlayCurvedTitle)
        val sub = incOverlay.findViewById<TextView>(R.id.tvOverlaySub)

        val primary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        val secondary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary)
        val home = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome)

        board.setImageResource(R.drawable.pause_board)
        // Do not overlay the generic hero/logo icon on the pause board.
        incOverlay.findViewById<android.widget.ImageView>(R.id.ivOverlayCrown).visibility = View.GONE
        curvedTitle.titleText = "GAME PAUSED"
        sub.text = "LEVEL ${saveData.currentLevel}"

        primary.visibility = View.VISIBLE
        secondary.visibility = View.VISIBLE
        home.visibility = View.VISIBLE

        primary.variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN
        secondary.variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
        home.variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.ORANGE

        primary.iconType = com.jumpadventure.game.graphics.GamePrimaryButton.IconType.PLAY
        secondary.iconType = com.jumpadventure.game.graphics.GamePrimaryButton.IconType.RESTART
        home.iconType = com.jumpadventure.game.graphics.GamePrimaryButton.IconType.HOME

        primary.mainText = "RESUME"; primary.subText = ""
        secondary.mainText = "RESTART"; secondary.subText = ""
        home.mainText = "HOME"; home.subText = ""

        var isActionClicked = false

        primary.setOnClickListener {
            if (isActionClicked) return@setOnClickListener
            isActionClicked = true
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            currentGameView?.resumeGame()
        }
        secondary.setOnClickListener {
            if (isActionClicked) return@setOnClickListener
            isActionClicked = true
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            startLevelGameplay(saveData.currentLevel)
        }
        home.setOnClickListener {
            if (isActionClicked) return@setOnClickListener
            isActionClicked = true
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            currentGameView?.stopGameLoop()
            showScreen("MAIN_MENU")
        }

        boardContainer.scaleX = 0.95f
        boardContainer.scaleY = 0.95f
        boardContainer.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(160)
            .start()
    }

    /* ------------------------------------------------------------------------
     * CHARACTERS SCREEN
     * ------------------------------------------------------------------------ */
    private fun openCharactersScreen() {
        updateBottomNavSelection("CHARACTERS")
        val title = incSecondary.findViewById<TextView>(R.id.tvSecondaryTitle)
        val tvCoinsSec = incSecondary.findViewById<TextView>(R.id.tvSecondaryCoins)
        title.text = "HEROES"
        tvCoinsSec.text = "${saveData.coins}"

        incSecondary.findViewById<ImageButton>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        val characterList = listOf(
            CharacterItem("DEFAULT", "Red Hoodie", 0, 0, "Default adventurous hero", "#E53935"),
            CharacterItem("NINJA", "Shadow Ninja", 3000, 0, "Fast shadow warrior", "#212121"),
            CharacterItem("ROBOT", "Cyber Bot", 5000, 0, "Metallic high jumper", "#78909C"),
            CharacterItem("GIRL", "Pink Runner", 3000, 0, "Stylish cute runner", "#EC407A"),
            CharacterItem("PIRATE", "Captain Red", 4000, 0, "Seafaring adventurer", "#D84315"),
            CharacterItem("COWBOY", "Wild Ranger", 4000, 0, "Outlaw quick jumper", "#8D6E63")
        )

        var currentRow: LinearLayout? = null
        val density = resources.displayMetrics.density

        characterList.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, (6 * density).toInt(), 0, (6 * density).toInt()) }
                }
                container.addView(currentRow)
            }

            val isSelected = saveData.selectedCharacter == item.id
            val isUnlocked = saveData.unlockedCharacters.contains(item.id)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val pad = (14 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setBackgroundResource(if (isSelected) R.drawable.bg_card_selected else R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val marginStart = if (index % 2 == 0) 0 else (6 * density).toInt()
                    val marginEnd = if (index % 2 == 0) (6 * density).toInt() else 0
                    setMargins(marginStart, 0, marginEnd, 0)
                }
            }

            val charCardView = com.jumpadventure.game.graphics.CharacterCardView(this).apply {
                characterId = item.id
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (120 * density).toInt()
                )
            }

            val tvName = TextView(this).apply {
                text = item.name
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F3045"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, (6 * density).toInt(), 0, (2 * density).toInt()) }
            }

            val tvDesc = TextView(this).apply {
                text = item.description
                textSize = 10f
                setTextColor(Color.parseColor("#6B7C93"))
                gravity = Gravity.CENTER
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, (8 * density).toInt()) }
            }

            val actionBtn = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
                when {
                    isSelected -> {
                        mainText = "SELECTED"
                        subText = ""
                        variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.ORANGE
                    }
                    isUnlocked -> {
                        mainText = "SELECT"
                        subText = ""
                        variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
                    }
                    else -> {
                        mainText = "UNLOCK"
                        subText = "${item.priceCoins} Coins"
                        variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN
                    }
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (44 * density).toInt()
                )

                setOnClickListener {
                    soundManager.playButtonClick()
                    if (isUnlocked) {
                        saveData.selectedCharacter = item.id
                        saveManager.saveData(saveData)
                        openCharactersScreen()
                    } else {
                        showCustomCharacterPreviewDialog(item)
                    }
                }
            }

            card.addView(charCardView)
            card.addView(tvName)
            card.addView(tvDesc)
            card.addView(actionBtn)
            currentRow?.addView(card)
        }

        showScreen("SECONDARY")
    }

    private fun showCustomCharacterPreviewDialog(item: CharacterItem) {
        showCustomGameDialog(
            title = item.name.uppercase(),
            message = "${item.description}\n\nPrice: ${item.priceCoins} Coins",
            primaryBtnText = "UNLOCK (${item.priceCoins} COINS)",
            secondaryBtnText = "CANCEL",
            onConfirm = {
                if (saveData.coins >= item.priceCoins) {
                    saveData.coins -= item.priceCoins
                    saveData.unlockedCharacters.add(item.id)
                    saveData.selectedCharacter = item.id
                    saveManager.saveData(saveData)
                    openCharactersScreen()
                } else {
                    showCustomGameDialog(
                        title = "INSUFFICIENT COINS",
                        message = "You need ${item.priceCoins - saveData.coins} more coins to unlock ${item.name}!",
                        primaryBtnText = "GO TO SHOP",
                        secondaryBtnText = "CANCEL",
                        onConfirm = { openShopScreen() }
                    )
                }
            }
        )
    }

    private fun showCustomGameDialog(
        title: String,
        message: String,
        primaryBtnText: String = "OK",
        secondaryBtnText: String? = null,
        onConfirm: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        incOverlay.visibility = View.VISIBLE
        incOverlay.bringToFront()
        val boardContainer = incOverlay.findViewById<FrameLayout>(R.id.overlayBoardContainer)
        boardContainer.visibility = View.GONE

        val customDialog = incOverlay.findViewById<com.jumpadventure.game.graphics.GameDialogView>(R.id.customGameDialog)
        customDialog.visibility = View.VISIBLE
        customDialog.isClickable = true
        customDialog.isFocusable = true
        customDialog.bringToFront()

        customDialog.titleView.text = title
        customDialog.messageView.text = message
        customDialog.buttonContainer.removeAllViews()

        val density = resources.displayMetrics.density

        val btnPrimary = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
            mainText = primaryBtnText
            subText = ""
            variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.ORANGE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (50 * density).toInt()
            ).apply { setMargins(0, 0, 0, (6 * density).toInt()) }
            setOnClickListener {
                soundManager.playButtonClick()
                customDialog.visibility = View.GONE
                incOverlay.visibility = View.GONE
                onConfirm?.invoke()
            }
        }
        customDialog.buttonContainer.addView(btnPrimary)

        if (secondaryBtnText != null) {
            val btnSec = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
                mainText = secondaryBtnText
                subText = ""
                variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (50 * density).toInt()
                )
                setOnClickListener {
                    soundManager.playButtonClick()
                    customDialog.visibility = View.GONE
                    incOverlay.visibility = View.GONE
                    onCancel?.invoke()
                }
            }
            customDialog.buttonContainer.addView(btnSec)
        }

        customDialog.requestLayout()
        customDialog.invalidate()
    }

    /* ------------------------------------------------------------------------
     * SHOP SCREEN
     * ------------------------------------------------------------------------ */
    private fun openShopScreen() {
        updateBottomNavSelection("SHOP")
        val title = incSecondary.findViewById<TextView>(R.id.tvSecondaryTitle)
        val tvCoinsSec = incSecondary.findViewById<TextView>(R.id.tvSecondaryCoins)
        title.text = "SHOP"
        tvCoinsSec.text = "${saveData.coins}"

        incSecondary.findViewById<ImageButton>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        val packs = listOf(
            Triple("Small Coin Pack", 500, "Daily Explorer Boost"),
            Triple("Medium Coin Pack", 1500, "Adventurer Chest"),
            Triple("Large Coin Pack", 5000, "Treasure Hoard"),
            Triple("Mega Gem Pack", 50, "Shiny Gem Stash")
        )

        var currentRow: LinearLayout? = null
        val density = resources.displayMetrics.density

        packs.forEachIndexed { index, (name, amount, desc) ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, (6 * density).toInt(), 0, (6 * density).toInt()) }
                }
                container.addView(currentRow)
            }

            val isGem = name.contains("Gem")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val pad = (14 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setBackgroundResource(R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val marginStart = if (index % 2 == 0) 0 else (6 * density).toInt()
                    val marginEnd = if (index % 2 == 0) (6 * density).toInt() else 0
                    setMargins(marginStart, 0, marginEnd, 0)
                }
            }

            val iconView = ImageView(this).apply {
                setImageResource(if (isGem) R.drawable.ic_gem else R.drawable.ic_coin)
                layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt()).apply {
                    setMargins(0, 0, 0, (6 * density).toInt())
                }
            }

            val tvName = TextView(this).apply {
                text = name
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F3045"))
                gravity = Gravity.CENTER
            }

            val tvDesc = TextView(this).apply {
                text = desc
                textSize = 10f
                setTextColor(Color.parseColor("#6B7C93"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, (2 * density).toInt(), 0, (10 * density).toInt()) }
            }

            val claimBtn = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
                mainText = "CLAIM"
                subText = if (isGem) "+$amount GEMS" else "+$amount COINS"
                variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (46 * density).toInt()
                )

                setOnClickListener {
                    soundManager.playCoin()
                    if (isGem) {
                        saveData.gems += amount
                    } else {
                        saveData.coins += amount
                    }
                    saveManager.saveData(saveData)
                    tvCoinsSec.text = "${saveData.coins}"
                    showCustomGameDialog(
                        title = "REWARD CLAIMED!",
                        message = "You received +$amount ${if (isGem) "Gems" else "Coins"}!"
                    )
                }
            }

            card.addView(iconView)
            card.addView(tvName)
            card.addView(tvDesc)
            card.addView(claimBtn)
            currentRow?.addView(card)
        }

        showScreen("SECONDARY")
    }

    /* ------------------------------------------------------------------------
     * WORLDS SCREEN
     * ------------------------------------------------------------------------ */
    private fun openWorldsScreen() {
        updateBottomNavSelection("WORLDS")
        val title = incSecondary.findViewById<TextView>(R.id.tvSecondaryTitle)
        val tvCoinsSec = incSecondary.findViewById<TextView>(R.id.tvSecondaryCoins)
        title.text = "WORLDS"
        tvCoinsSec.text = "${saveData.coins}"

        incSecondary.findViewById<ImageButton>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        val density = resources.displayMetrics.density

        WorldRepository.worlds.forEach { world ->
            // World card container where artwork fills full background edge-to-edge
            val cardFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (200 * density).toInt()
                ).apply { setMargins(0, (10 * density).toInt(), 0, (10 * density).toInt()) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 20f * density
                }
                clipToOutline = true
            }

            // 1. Full artwork background filling card
            val bgResId = WorldRepository.getWorldBackgroundRes(world.id)
            val bgImageView = ImageView(this).apply {
                setImageResource(bgResId)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            // 2. Dark translucent overlay for contrast & readability
            val overlayView = View(this).apply {
                background = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        Color.parseColor("#400F172A"),
                        Color.parseColor("#B00B1426")
                    )
                )
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            // 3. Card Content: Title, Levels, and 3D Glass Explore button
            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                val pad = (16 * density).toInt()
                setPadding(pad, pad, pad, pad)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            val tvWorldTitle = TextView(this).apply {
                text = "WORLD ${world.id}: ${world.name.uppercase()}"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setShadowLayer(6f, 0f, 4f, Color.parseColor("#0F172A"))
                gravity = Gravity.CENTER
            }

            val tvWorldLevels = TextView(this).apply {
                val endLvlText = if (world.endLevel > 10000) "+" else " - ${world.endLevel}"
                text = "LEVELS ${world.startLevel}$endLvlText"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#FFD43B"))
                setShadowLayer(4f, 0f, 2f, Color.BLACK)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, (4 * density).toInt(), 0, (14 * density).toInt()) }
            }

            val totalStars = saveData.levelStars.values.sum()
            val isUnlocked = saveData.unlockedWorlds.contains(world.id) || totalStars >= world.requiredStarsToUnlock || saveData.highestLevel >= world.startLevel

            val statusBtn = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
                if (isUnlocked) {
                    mainText = "EXPLORE"
                    subText = ""
                    variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GLASS
                    setOnClickListener {
                        soundManager.playButtonClick()
                        saveData.currentLevel = world.startLevel
                        saveManager.saveData(saveData)
                        openLevelMapScreen()
                    }
                } else {
                    mainText = "LOCKED"
                    subText = "${world.requiredStarsToUnlock} STARS REQUIRED"
                    variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
                    setOnClickListener {
                        soundManager.playButtonClick()
                        showCustomGameDialog(
                            title = "WORLD LOCKED",
                            message = "Earn ${world.requiredStarsToUnlock} stars across levels to unlock ${world.name}!"
                        )
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    minOf((220 * density).toInt(), (resources.displayMetrics.widthPixels * 0.7f).toInt()),
                    (54 * density).toInt()
                )
            }

            contentLayout.addView(tvWorldTitle)
            contentLayout.addView(tvWorldLevels)
            contentLayout.addView(statusBtn)

            cardFrame.addView(bgImageView)
            cardFrame.addView(overlayView)
            cardFrame.addView(contentLayout)

            container.addView(cardFrame)
        }

        showScreen("SECONDARY")
    }

    /* ------------------------------------------------------------------------
     * ACHIEVEMENTS SCREEN
     * ------------------------------------------------------------------------ */
    private fun openAchievementsScreen() {
        updateBottomNavSelection("ACHIEVEMENTS")
        val title = incSecondary.findViewById<TextView>(R.id.tvSecondaryTitle)
        val tvCoinsSec = incSecondary.findViewById<TextView>(R.id.tvSecondaryCoins)
        title.text = "TROPHIES"
        tvCoinsSec.text = "${saveData.coins}"

        incSecondary.findViewById<ImageButton>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        val achievements = listOf(
            AchievementItem("A1", "First Jump", "Complete Level 1", "", 100) { it.highestLevel > 1 },
            AchievementItem("A2", "Explorer", "Complete 10 levels", "", 300) { it.highestLevel > 10 },
            AchievementItem("A3", "Collector", "Collect 1,000 coins", "", 500) { it.totalCoinsCollected >= 1000 },
            AchievementItem("A4", "Star Player", "Earn 50 stars", "", 500) { it.levelStars.values.sum() >= 50 },
            AchievementItem("A5", "World Traveler", "Unlock 5 worlds", "", 1000) { it.unlockedWorlds.size >= 5 },
            AchievementItem("A6", "Master", "Complete 100 levels", "", 2000) { it.highestLevel > 100 }
        )

        var currentRow: LinearLayout? = null
        val density = resources.displayMetrics.density

        achievements.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, (6 * density).toInt(), 0, (6 * density).toInt()) }
                }
                container.addView(currentRow)
            }

            val isUnlocked = item.isUnlocked(saveData)
            val isClaimed = saveData.unlockedAchievements.contains(item.id)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val pad = (14 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setBackgroundResource(R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val marginStart = if (index % 2 == 0) 0 else (6 * density).toInt()
                    val marginEnd = if (index % 2 == 0) (6 * density).toInt() else 0
                    setMargins(marginStart, 0, marginEnd, 0)
                }
            }

            val iconView = ImageView(this).apply {
                setImageResource(R.drawable.ic_trophy)
                layoutParams = LinearLayout.LayoutParams((42 * density).toInt(), (42 * density).toInt()).apply {
                    setMargins(0, 0, 0, (6 * density).toInt())
                }
            }

            val tvTitle = TextView(this).apply {
                text = item.title
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F3045"))
                gravity = Gravity.CENTER
            }

            val tvDesc = TextView(this).apply {
                text = item.description
                textSize = 10f
                setTextColor(Color.parseColor("#6B7C93"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, (2 * density).toInt(), 0, (10 * density).toInt()) }
            }

            val actionBtn = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
                when {
                    isClaimed -> {
                        mainText = "CLAIMED"
                        subText = ""
                        variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
                        isEnabled = false
                    }
                    isUnlocked -> {
                        mainText = "CLAIM"
                        subText = "+${item.rewardCoins} COINS"
                        variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN
                        setOnClickListener {
                            soundManager.playCoin()
                            saveData.coins += item.rewardCoins
                            saveData.unlockedAchievements.add(item.id)
                            saveManager.saveData(saveData)
                            openAchievementsScreen()
                        }
                    }
                    else -> {
                        mainText = "LOCKED"
                        subText = "+${item.rewardCoins} COINS"
                        variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
                        isEnabled = false
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (46 * density).toInt()
                )
            }

            card.addView(iconView)
            card.addView(tvTitle)
            card.addView(tvDesc)
            card.addView(actionBtn)
            currentRow?.addView(card)
        }

        showScreen("SECONDARY")
    }

    /* ------------------------------------------------------------------------
     * SETTINGS SCREEN
     * ------------------------------------------------------------------------ */
    private fun openSettingsScreen() {
        val title = incSecondary.findViewById<TextView>(R.id.tvSecondaryTitle)
        val tvCoinsSec = incSecondary.findViewById<TextView>(R.id.tvSecondaryCoins)
        title.text = "SETTINGS"
        tvCoinsSec.text = "${saveData.coins}"

        incSecondary.findViewById<ImageButton>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        val density = resources.displayMetrics.density

        // Sound Toggle
        val soundBtn = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
            mainText = "SOUND EFFECTS"
            subText = if (saveData.soundEnabled) "STATE: ON" else "STATE: OFF"
            variant = if (saveData.soundEnabled) com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN else com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (54 * density).toInt()
            ).apply { setMargins(0, (8 * density).toInt(), 0, (8 * density).toInt()) }

            setOnClickListener {
                saveData.soundEnabled = !saveData.soundEnabled
                soundManager.soundEnabled = saveData.soundEnabled
                saveManager.saveData(saveData)
                subText = if (saveData.soundEnabled) "STATE: ON" else "STATE: OFF"
                variant = if (saveData.soundEnabled) com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN else com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
            }
        }
        container.addView(soundBtn)

        // Music Toggle
        val musicBtn = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
            mainText = "BACKGROUND MUSIC"
            subText = if (saveData.musicEnabled) "STATE: ON" else "STATE: OFF"
            variant = if (saveData.musicEnabled) com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN else com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (54 * density).toInt()
            ).apply { setMargins(0, (8 * density).toInt(), 0, (8 * density).toInt()) }

            setOnClickListener {
                saveData.musicEnabled = !saveData.musicEnabled
                soundManager.musicEnabled = saveData.musicEnabled
                saveManager.saveData(saveData)
                subText = if (saveData.musicEnabled) "STATE: ON" else "STATE: OFF"
                variant = if (saveData.musicEnabled) com.jumpadventure.game.graphics.GamePrimaryButton.Variant.GREEN else com.jumpadventure.game.graphics.GamePrimaryButton.Variant.BLUE
            }
        }
        container.addView(musicBtn)

        // Reset Progress
        val resetBtn = com.jumpadventure.game.graphics.GamePrimaryButton(this).apply {
            mainText = "RESET ALL PROGRESS"
            subText = "ERASE ALL DATA"
            variant = com.jumpadventure.game.graphics.GamePrimaryButton.Variant.ORANGE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (54 * density).toInt()
            ).apply { setMargins(0, (20 * density).toInt(), 0, (8 * density).toInt()) }

            setOnClickListener {
                soundManager.playButtonClick()
                showCustomGameDialog(
                    title = "RESET PROGRESS?",
                    message = "Are you sure you want to reset all game progress, unlocked characters, and stars? This cannot be undone.",
                    primaryBtnText = "RESET",
                    secondaryBtnText = "CANCEL",
                    onConfirm = {
                        saveManager.resetProgress()
                        saveData = saveManager.loadData()
                        updateCurrencyHUD()
                        showScreen("MAIN_MENU")
                    }
                )
            }
        }
        container.addView(resetBtn)

        showScreen("SECONDARY")
    }
}
