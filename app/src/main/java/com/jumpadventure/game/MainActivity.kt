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

            val playH = dp(76f).coerceIn(dp(68f), (h * 0.11f).toInt())
            val playW = minOf(dp(320f), w - dp(32f))
            val navHeight = dp(72f)
            val logoH = (h * 0.18f).toInt().coerceIn(dp(125f), dp(150f))
            val logoW = minOf(dp(320f), (w * 0.84f).toInt())

            logo.layoutParams = FrameLayout.LayoutParams(logoW, logoH).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dp(2f)
            }

            play.layoutParams = FrameLayout.LayoutParams(playW, playH).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(6f)
            }

            // Keep the mountain lower, with its visible top surface close to the character feet.
            val mountainW = minOf(dp(255f), (w * 0.66f).toInt())
            val mountainRatio = 3264f / 2857f
            val mountainH = minOf(
                (mountainW * mountainRatio).toInt(),
                (h * 0.44f).toInt()
            )
            val playTop = h - playH - dp(6f)
            val mountainBottom = playTop + dp(34f)
            val mountainTop = (mountainBottom - mountainH).coerceAtLeast(logoH + dp(10f))

            mountain.layoutParams = FrameLayout.LayoutParams(mountainW, mountainH).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = mountainTop
            }

            // Character sits on the mountain surface, not in the air.
            val characterW = minOf(dp(185f), (w * 0.46f).toInt())
            val characterH = minOf(dp(205f), (h * 0.225f).toInt())
            val feetY = mountainTop + dp(30f)
            character.layoutParams = FrameLayout.LayoutParams(characterW, characterH).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = (feetY - characterH).coerceAtLeast(logoH + dp(8f))
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
        saveData.currentLevel = levelNum
        saveManager.saveData(saveData)

        val gameContainer = incGameplay.findViewById<FrameLayout>(R.id.gameViewContainer)
        gameContainer.removeAllViews()

        val tvHudCoins = incGameplay.findViewById<TextView>(R.id.tvHudCoins)
        val tvHudStars = incGameplay.findViewById<TextView>(R.id.tvHudStars)
        val tvHudLevel = incGameplay.findViewById<TextView>(R.id.tvHudLevel)
        val pbLevelProgress = incGameplay.findViewById<ProgressBar>(R.id.pbLevelProgress)

        tvHudLevel.text = "LEVEL $levelNum"

        currentGameView = GameView(
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

        gameContainer.addView(currentGameView)

        incGameplay.findViewById<ImageButton>(R.id.btnPause).setOnClickListener {
            soundManager.playButtonClick()
            showPauseOverlay()
        }

        incGameplay.findViewById<Button>(R.id.btnPowerUpMagnet).setOnClickListener {
            currentGameView?.activateMagnetPowerUp()
        }
        incGameplay.findViewById<Button>(R.id.btnPowerUpShield).setOnClickListener {
            currentGameView?.activateShieldPowerUp()
        }
        incGameplay.findViewById<Button>(R.id.btnPowerUpSpeed).setOnClickListener {
            currentGameView?.activateSpeedPowerUp()
        }

        showScreen("GAMEPLAY")
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
        val board = incOverlay.findViewById<ImageView>(R.id.ivPauseBoard)
        val header = incOverlay.findViewById<TextView>(R.id.tvOverlayHeader)
        val sub = incOverlay.findViewById<TextView>(R.id.tvOverlaySub)
        val stars = incOverlay.findViewById<TextView>(R.id.tvOverlayStars)
        val coins = incOverlay.findViewById<TextView>(R.id.tvOverlayCoins)
        val time = incOverlay.findViewById<TextView>(R.id.tvOverlayTime)
        val primary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        val secondary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary)
        val home = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome)

        board.setImageResource(R.drawable.winner_bg)
        header.text = "LEVEL COMPLETE!"
        sub.text = "LEVEL ${saveData.currentLevel}"
        val earnedStars = starsEarned.coerceIn(0, 3)
        stars.text = "★".repeat(earnedStars) + "☆".repeat(3 - earnedStars)
        coins.text = "+${coinsEarned} COINS   •   +${starsEarned} STARS"
        time.text = "TIME  ${String.format("%.1fs", timeTakenSec)}"

        primary.visibility = View.VISIBLE
        secondary.visibility = View.VISIBLE
        home.visibility = View.VISIBLE
        primary.mainText = "NEXT LEVEL"; primary.subText = ""
        secondary.mainText = "RESTART"; secondary.subText = ""
        home.mainText = "HOME"; home.subText = ""

        primary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel + 1)
        }
        secondary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel)
        }
        home.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            showScreen("MAIN_MENU")
        }
    }
    private fun handleGameOver() {
        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).visibility = View.VISIBLE

        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = "GAME OVER"
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = "Level ${saveData.currentLevel}"
        incOverlay.findViewById<TextView>(R.id.tvOverlayStars).text = "FAILED"
        incOverlay.findViewById<TextView>(R.id.tvOverlayCoins).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayTime).text = ""

        val btnPrimary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        btnPrimary.mainText = "RETRY"
        btnPrimary.subText = ""
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).subText = ""
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel)
        }

        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary).visibility = View.GONE

        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }
    }

    private fun showPauseOverlay() {
        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary).visibility = View.VISIBLE
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).visibility = View.VISIBLE

        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = "PAUSED"
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = "Level ${saveData.currentLevel}"
        incOverlay.findViewById<TextView>(R.id.tvOverlayStars).text = "PAUSED"
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary).mainText = "RESUME"
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary).subText = ""
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary).mainText = "RESTART"
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary).subText = ""
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).mainText = "HOME"
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).subText = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayCoins).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayTime).text = ""

        val btnPrimary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        btnPrimary.mainText = "RESUME"
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
        }

        val btnSec = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary)
        btnSec.mainText = "RESTART"
        btnSec.subText = ""
        btnSec.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel)
        }

        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }
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

        characterList.forEachIndexed { index, item ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 10, 0, 10) }
                }
                container.addView(currentRow)
            }

            val isSelected = saveData.selectedCharacter == item.id
            val isUnlocked = saveData.unlockedCharacters.contains(item.id)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundResource(if (isSelected) R.drawable.bg_card_selected else R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val marginStart = if (index % 2 == 0) 0 else 8
                    val marginEnd = if (index % 2 == 0) 8 else 0
                    setMargins(marginStart, 0, marginEnd, 0)
                }
            }

            val density = resources.displayMetrics.density
            val charCardView = com.jumpadventure.game.graphics.CharacterCardView(this).apply {
                characterId = item.id
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (130 * density).toInt()
                )
            }

            val tvName = TextView(this).apply {
                text = item.name
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F3045"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 4) }
            }

            val tvDesc = TextView(this).apply {
                text = item.description
                textSize = 11f
                setTextColor(Color.parseColor("#6B7C93"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 10) }
            }

            val actionBtn = Button(this).apply {
                if (isSelected) {
                    text = "✓ SELECTED"
                    setBackgroundResource(R.drawable.bg_button_game_primary)
                } else if (isUnlocked) {
                    text = "SELECT"
                    setBackgroundResource(R.drawable.bg_button_game_secondary)
                } else {
                    text = "${item.priceCoins} Coins"
                    setBackgroundResource(R.drawable.bg_button_game_primary)
                }

                setTextColor(Color.parseColor("#1F3045"))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (48 * density).toInt()
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
        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = item.name.uppercase()
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = item.description
        incOverlay.findViewById<TextView>(R.id.tvOverlayStars).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayCoins).text = "Price: ${item.priceCoins} Coins"
        incOverlay.findViewById<TextView>(R.id.tvOverlayTime).text = ""

        val btnPrimary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        btnPrimary.mainText = "UNLOCK FOR ${item.priceCoins} COINS"
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            if (saveData.coins >= item.priceCoins) {
                saveData.coins -= item.priceCoins
                saveData.unlockedCharacters.add(item.id)
                saveData.selectedCharacter = item.id
                saveManager.saveData(saveData)
                incOverlay.visibility = View.GONE
                openCharactersScreen()
            } else {
                showCustomGameDialog("INSUFFICIENT COINS", "You need ${item.priceCoins - saveData.coins} more coins to unlock ${item.name}.")
            }
        }

        val btnSec = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary)
        btnSec.visibility = View.VISIBLE
        btnSec.text = "CLOSE"
        btnSec.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
        }

        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).visibility = View.GONE
    }

    private fun showCustomGameDialog(title: String, message: String, onConfirm: (() -> Unit)? = null) {
        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = title
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = message
        incOverlay.findViewById<TextView>(R.id.tvOverlayStars).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayCoins).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayTime).text = ""

        val btnPrimary = incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayPrimary)
        btnPrimary.mainText = "OK"
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            onConfirm?.invoke()
        }

        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlaySecondary).visibility = View.GONE
        incOverlay.findViewById<com.jumpadventure.game.graphics.GamePrimaryButton>(R.id.btnOverlayHome).visibility = View.GONE
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
            Triple("Small Coin Pack", 500, "Free daily claim reward"),
            Triple("Medium Coin Pack", 1500, "Bonus adventure pack"),
            Triple("Large Coin Pack", 5000, "Master explorer chest")
        )

        packs.forEach { (name, amount, desc) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 24, 24, 24)
                setBackgroundResource(R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 10, 0, 10) }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            infoLayout.addView(TextView(this).apply {
                text = name
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F3045"))
            })
            infoLayout.addView(TextView(this).apply {
                text = desc
                textSize = 12f
                setTextColor(Color.parseColor("#6B7C93"))
            })

            val claimBtn = Button(this).apply {
                text = "+$amount COINS"
                setBackgroundResource(R.drawable.bg_button_game_primary)
                setTextColor(Color.parseColor("#1F3045"))
                typeface = Typeface.DEFAULT_BOLD

                setOnClickListener {
                    soundManager.playCoin()
                    saveData.coins += amount
                    saveManager.saveData(saveData)
                    tvCoinsSec.text = "${saveData.coins}"
                    showCustomGameDialog("REWARD CLAIMED!", "You earned +$amount Coins!")
                }
            }

            card.addView(infoLayout)
            card.addView(claimBtn)
            container.addView(card)
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

        WorldRepository.worlds.forEach { world ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
                setBackgroundResource(R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 12, 0, 12) }
            }

            val density = resources.displayMetrics.density
            val bannerView = View(this).apply {
                setBackgroundColor(Color.parseColor(world.skyColorHex))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (120 * density).toInt()).apply { setMargins(0, 0, 0, 12) }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            infoLayout.addView(TextView(this).apply {
                text = "WORLD ${world.id}: ${world.name.uppercase()}"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F3045"))
            })
            infoLayout.addView(TextView(this).apply {
                val endLvlText = if (world.endLevel > 10000) "+" else " - ${world.endLevel}"
                text = "Levels ${world.startLevel}$endLvlText"
                textSize = 13f
                setTextColor(Color.parseColor("#6B7C93"))
            })

            val totalStars = saveData.levelStars.values.sum()
            val isUnlocked = saveData.unlockedWorlds.contains(world.id) || totalStars >= world.requiredStarsToUnlock || saveData.highestLevel >= world.startLevel

            val statusBtn = Button(this).apply {
                if (isUnlocked) {
                    text = "EXPLORE"
                    setBackgroundResource(R.drawable.bg_button_game_primary)
                    setOnClickListener {
                        soundManager.playButtonClick()
                        saveData.currentLevel = world.startLevel
                        saveManager.saveData(saveData)
                        openLevelMapScreen()
                    }
                } else {
                    text = "LOCKED: ${world.requiredStarsToUnlock} Stars Required"
                    setBackgroundResource(R.drawable.bg_button_game_secondary)
                    isEnabled = false
                }
                setTextColor(Color.parseColor("#1F3045"))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * density).toInt()).apply { setMargins(0, 14, 0, 0) }
            }

            card.addView(bannerView)
            card.addView(infoLayout)
            card.addView(statusBtn)
            container.addView(card)
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

        achievements.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 24, 24, 24)
                setBackgroundResource(R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 10, 0, 10) }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            infoLayout.addView(TextView(this).apply {
                text = item.title
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F3045"))
            })
            infoLayout.addView(TextView(this).apply {
                text = item.description
                textSize = 12f
                setTextColor(Color.parseColor("#6B7C93"))
            })

            val isUnlocked = item.isUnlocked(saveData)
            val isClaimed = saveData.unlockedAchievements.contains(item.id)

            val actionBtn = Button(this).apply {
                if (isClaimed) {
                    text = "✓ CLAIMED"
                    setBackgroundResource(R.drawable.bg_button_game_secondary)
                    isEnabled = false
                } else if (isUnlocked) {
                    text = "+${item.rewardCoins} COINS"
                    setBackgroundResource(R.drawable.bg_button_game_primary)
                    setOnClickListener {
                        soundManager.playCoin()
                        saveData.coins += item.rewardCoins
                        saveData.unlockedAchievements.add(item.id)
                        saveManager.saveData(saveData)
                        openAchievementsScreen()
                    }
                } else {
                    text = "LOCKED"
                    setBackgroundResource(R.drawable.bg_button_game_secondary)
                    isEnabled = false
                }
                setTextColor(Color.parseColor("#1F3045"))
                typeface = Typeface.DEFAULT_BOLD
            }

            card.addView(infoLayout)
            card.addView(actionBtn)
            container.addView(card)
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
        val soundBtn = Button(this).apply {
            text = "🔊 Sound Effects: " + if (saveData.soundEnabled) "ON" else "OFF"
            setBackgroundResource(if (saveData.soundEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            setTextColor(Color.parseColor("#1F3045"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (52 * density).toInt()
            ).apply { setMargins(0, 10, 0, 10) }

            setOnClickListener {
                saveData.soundEnabled = !saveData.soundEnabled
                soundManager.soundEnabled = saveData.soundEnabled
                saveManager.saveData(saveData)
                text = "🔊 Sound Effects: " + if (saveData.soundEnabled) "ON" else "OFF"
                setBackgroundResource(if (saveData.soundEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            }
        }
        container.addView(soundBtn)

        // Music Toggle
        val musicBtn = Button(this).apply {
            text = "🎵 Background Music: " + if (saveData.musicEnabled) "ON" else "OFF"
            setBackgroundResource(if (saveData.musicEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            setTextColor(Color.parseColor("#1F3045"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (52 * density).toInt()
            ).apply { setMargins(0, 10, 0, 10) }

            setOnClickListener {
                saveData.musicEnabled = !saveData.musicEnabled
                soundManager.musicEnabled = saveData.musicEnabled
                saveManager.saveData(saveData)
                text = "🎵 Background Music: " + if (saveData.musicEnabled) "ON" else "OFF"
                setBackgroundResource(if (saveData.musicEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            }
        }
        container.addView(musicBtn)

        // Reset Progress
        val resetBtn = Button(this).apply {
            text = "♻ RESET ALL PROGRESS"
            setBackgroundResource(R.drawable.bg_button_game_danger)
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (52 * density).toInt()
            ).apply { setMargins(0, 20, 0, 10) }

            setOnClickListener {
                soundManager.playButtonClick()
                showCustomGameDialog("RESET PROGRESS?", "Are you sure you want to reset all game data? This cannot be undone.") {
                    saveManager.resetProgress()
                    saveData = saveManager.loadData()
                    updateCurrencyHUD()
                    showScreen("MAIN_MENU")
                }
            }
        }
        container.addView(resetBtn)

        showScreen("SECONDARY")
    }
}
