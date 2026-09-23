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
            incLevelMap.findViewById<View>(R.id.mapScrollView),
            incMainMenu.findViewById<View>(R.id.homeScrollView)
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
        if (starsEarned > prevStars) {
            saveData.levelStars[saveData.currentLevel] = starsEarned
        }

        if (saveData.currentLevel >= saveData.highestLevel) {
            saveData.highestLevel = saveData.currentLevel + 1
        }

        saveManager.saveData(saveData)

        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<Button>(R.id.btnOverlaySecondary).visibility = View.VISIBLE
        incOverlay.findViewById<Button>(R.id.btnOverlayHome).visibility = View.VISIBLE

        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = "LEVEL COMPLETE"
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = "Level ${saveData.currentLevel}"
        val tvOverlayStars = incOverlay.findViewById<TextView>(R.id.tvOverlayStars)
        val tvOverlayCoins = incOverlay.findViewById<TextView>(R.id.tvOverlayCoins)
        val tvOverlayTime = incOverlay.findViewById<TextView>(R.id.tvOverlayTime)
        tvOverlayStars.text = ""
        tvOverlayCoins.text = ""
        tvOverlayTime.text = ""

        val btnPrimary = incOverlay.findViewById<Button>(R.id.btnOverlayPrimary)
        btnPrimary.text = "▶ NEXT LEVEL"
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel + 1)
        }

        incOverlay.findViewById<Button>(R.id.btnOverlaySecondary).setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel)
        }

        incOverlay.findViewById<Button>(R.id.btnOverlayHome).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val delayMs = 400L
        overlayHandler.postDelayed({
            soundManager.playCoin()
            tvOverlayStars.text = "★"
        }, delayMs)

        if (starsEarned >= 2) {
            overlayHandler.postDelayed({
                soundManager.playCoin()
                tvOverlayStars.text = "★★"
            }, delayMs * 2)
        }

        if (starsEarned >= 3) {
            overlayHandler.postDelayed({
                soundManager.playCoin()
                tvOverlayStars.text = "★★★"
            }, delayMs * 3)
        }

        overlayHandler.postDelayed({
            soundManager.playCoin()
            tvOverlayCoins.text = "+$coinsEarned Coins"
            tvOverlayTime.text = "Time: ${String.format("%.1fs", timeTakenSec)}"

            val rewardsContainer = incOverlay.findViewById<LinearLayout>(R.id.rewardsContainer)
            val confettiContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val colors = listOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA)
            for (i in 0..10) {
                val confetti = View(this@MainActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(16, 16).apply { setMargins(8, 0, 8, 0) }
                    setBackgroundColor(colors[i % colors.size])
                    rotation = (Math.random() * 360).toFloat()
                    animate().translationYBy(200f).rotationBy(360f).setDuration(1000).start()
                }
                confettiContainer.addView(confetti)
            }
            rewardsContainer.addView(confettiContainer)
        }, delayMs * 4)
    }

    private fun handleGameOver() {
        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<Button>(R.id.btnOverlayHome).visibility = View.VISIBLE

        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = "GAME OVER"
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = "Level ${saveData.currentLevel}"
        incOverlay.findViewById<TextView>(R.id.tvOverlayStars).text = "FAILED"
        incOverlay.findViewById<TextView>(R.id.tvOverlayCoins).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayTime).text = ""

        val btnPrimary = incOverlay.findViewById<Button>(R.id.btnOverlayPrimary)
        btnPrimary.text = "↻ RETRY"
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel)
        }

        incOverlay.findViewById<Button>(R.id.btnOverlaySecondary).visibility = View.GONE

        incOverlay.findViewById<Button>(R.id.btnOverlayHome).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }
    }

    private fun showPauseOverlay() {
        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<Button>(R.id.btnOverlaySecondary).visibility = View.VISIBLE
        incOverlay.findViewById<Button>(R.id.btnOverlayHome).visibility = View.VISIBLE

        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = "PAUSED"
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = "Level ${saveData.currentLevel}"
        incOverlay.findViewById<TextView>(R.id.tvOverlayStars).text = "PAUSED"
        incOverlay.findViewById<TextView>(R.id.tvOverlayCoins).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayTime).text = ""

        val btnPrimary = incOverlay.findViewById<Button>(R.id.btnOverlayPrimary)
        btnPrimary.text = "▶ RESUME"
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
        }

        val btnSec = incOverlay.findViewById<Button>(R.id.btnOverlaySecondary)
        btnSec.text = "↻ RESTART"
        btnSec.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            overlayHandler.removeCallbacksAndMessages(null)
            startLevelGameplay(saveData.currentLevel)
        }

        incOverlay.findViewById<Button>(R.id.btnOverlayHome).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }
    }

    /* ------------------------------------------------------------------------
     * CHARACTERS SCREEN
     * ------------------------------------------------------------------------ */
    private fun openCharactersScreen() {
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

        val btnPrimary = incOverlay.findViewById<Button>(R.id.btnOverlayPrimary)
        btnPrimary.text = "UNLOCK FOR ${item.priceCoins} COINS"
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

        val btnSec = incOverlay.findViewById<Button>(R.id.btnOverlaySecondary)
        btnSec.visibility = View.VISIBLE
        btnSec.text = "CLOSE"
        btnSec.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
        }

        incOverlay.findViewById<Button>(R.id.btnOverlayHome).visibility = View.GONE
    }

    private fun showCustomGameDialog(title: String, message: String, onConfirm: (() -> Unit)? = null) {
        incOverlay.visibility = View.VISIBLE
        incOverlay.findViewById<TextView>(R.id.tvOverlayHeader).text = title
        incOverlay.findViewById<TextView>(R.id.tvOverlaySub).text = message
        incOverlay.findViewById<TextView>(R.id.tvOverlayStars).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayCoins).text = ""
        incOverlay.findViewById<TextView>(R.id.tvOverlayTime).text = ""

        val btnPrimary = incOverlay.findViewById<Button>(R.id.btnOverlayPrimary)
        btnPrimary.text = "OK"
        btnPrimary.setOnClickListener {
            soundManager.playButtonClick()
            incOverlay.visibility = View.GONE
            onConfirm?.invoke()
        }

        incOverlay.findViewById<Button>(R.id.btnOverlaySecondary).visibility = View.GONE
        incOverlay.findViewById<Button>(R.id.btnOverlayHome).visibility = View.GONE
    }

    /* ------------------------------------------------------------------------
     * SHOP SCREEN
     * ------------------------------------------------------------------------ */
    private fun openShopScreen() {
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
