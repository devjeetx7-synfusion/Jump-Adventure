package com.jumpadventure.game

import android.app.AlertDialog
import android.graphics.*
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
    private lateinit var btnPlay: Button
    private lateinit var charPreviewView: com.jumpadventure.game.graphics.CharacterPreviewView

    private var currentScreenName: String = "MAIN_MENU"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        tvCoins = incMainMenu.findViewById(R.id.tvCoins)
        tvGems = incMainMenu.findViewById(R.id.tvGems)
        btnPlay = incMainMenu.findViewById(R.id.btnPlay)
        charPreviewView = incMainMenu.findViewById(R.id.charPreviewView)
    }

    private fun updateCurrencyHUD() {
        tvCoins.text = "${saveData.coins} Coins"
        tvGems.text = "${saveData.gems} Gems"
        btnPlay.text = "▶ PLAY\nLevel ${saveData.currentLevel}"
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

        incMainMenu.findViewById<Button>(R.id.navShop).setOnClickListener {
            soundManager.playButtonClick()
            openShopScreen()
        }

        incMainMenu.findViewById<Button>(R.id.navCharacters).setOnClickListener {
            soundManager.playButtonClick()
            openCharactersScreen()
        }

        incMainMenu.findViewById<Button>(R.id.navWorlds).setOnClickListener {
            soundManager.playButtonClick()
            openWorldsScreen()
        }

        incMainMenu.findViewById<Button>(R.id.navAchievements).setOnClickListener {
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
        incLevelMap.findViewById<TextView>(R.id.tvMapWorldTitle).text = "World ${currentWorld.id}"
        incLevelMap.findViewById<TextView>(R.id.tvMapWorldSub).text = "${currentWorld.iconEmoji} ${currentWorld.name}"

        incLevelMap.findViewById<Button>(R.id.btnMapBack).setOnClickListener {
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

        // Auto scroll to current level node at bottom/middle of map
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

        tvHudLevel.text = "Level $levelNum"

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

        incGameplay.findViewById<Button>(R.id.btnPause).setOnClickListener {
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

        // Show Overlay
        incOverlay.visibility = View.VISIBLE
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

        // Sequential Animation
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

            // Trigger Confetti Animation Simulation
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
            incOverlay.findViewById<LinearLayout>(R.id.rewardsContainer).addView(confettiContainer)
        }, delayMs * 4)
    }

    private fun handleGameOver() {
        incOverlay.visibility = View.VISIBLE
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
        btnSec.visibility = View.VISIBLE
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
        title.text = "Characters"
        tvCoinsSec.text = "${saveData.coins} Coins"

        incSecondary.findViewById<Button>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        val characterList = listOf(
            CharacterItem("DEFAULT", "Red Hoodie", 0, 0, "Default cute runner", "#E53935"),
            CharacterItem("NINJA", "Shadow Ninja", 3000, 0, "Fast shadow warrior", "#212121"),
            CharacterItem("ROBOT", "Cyber Bot", 5000, 0, "Metallic high jumper", "#78909C"),
            CharacterItem("GIRL", "Pink Runner", 3000, 0, "Stylish cute runner", "#EC407A"),
            CharacterItem("PIRATE", "Captain Red", 4000, 0, "Seafaring adventurer", "#D84315"),
            CharacterItem("COWBOY", "Wild Ranger", 4000, 0, "Outlaw quick jumper", "#8D6E63")
        )

        // 2-Column Grid Layout for Characters
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

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                setBackgroundResource(R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val marginStart = if (index % 2 == 0) 0 else 8
                    val marginEnd = if (index % 2 == 0) 8 else 0
                    setMargins(marginStart, 0, marginEnd, 0)
                }
            }

            val charCardView = com.jumpadventure.game.graphics.CharacterCardView(this).apply {
                characterId = item.id
                layoutParams = LinearLayout.LayoutParams(160, 180)
            }

            val tvName = TextView(this).apply {
                text = item.name
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1B1B2F"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 4) }
            }

            val tvDesc = TextView(this).apply {
                text = item.description
                textSize = 11f
                setTextColor(Color.parseColor("#546E7A"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 10) }
            }

            val actionBtn = Button(this).apply {
                val isUnlocked = saveData.unlockedCharacters.contains(item.id)
                val isSelected = saveData.selectedCharacter == item.id

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

                setTextColor(Color.parseColor("#1B1B2F"))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 110
                )

                setOnClickListener {
                    soundManager.playButtonClick()
                    if (isUnlocked) {
                        saveData.selectedCharacter = item.id
                        saveManager.saveData(saveData)
                        openCharactersScreen()
                    } else if (saveData.coins >= item.priceCoins) {
                        saveData.coins -= item.priceCoins
                        saveData.unlockedCharacters.add(item.id)
                        saveData.selectedCharacter = item.id
                        saveManager.saveData(saveData)
                        openCharactersScreen()
                    } else {
                        Toast.makeText(this@MainActivity, "Not enough coins!", Toast.LENGTH_SHORT).show()
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

    /* ------------------------------------------------------------------------
     * SHOP SCREEN
     * ------------------------------------------------------------------------ */
    private fun openShopScreen() {
        val title = incSecondary.findViewById<TextView>(R.id.tvSecondaryTitle)
        val tvCoinsSec = incSecondary.findViewById<TextView>(R.id.tvSecondaryCoins)
        title.text = "Shop"
        tvCoinsSec.text = "${saveData.coins} Coins"

        incSecondary.findViewById<Button>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        val packs = listOf(
            Triple("Small Coin Pack", 500, "Free Claim"),
            Triple("Medium Coin Pack", 1500, "Earned in game"),
            Triple("Large Coin Pack", 5000, "Master level pack")
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
                ).apply { setMargins(0, 12, 0, 12) }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            infoLayout.addView(TextView(this).apply {
                text = name
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1B1B2F"))
            })
            infoLayout.addView(TextView(this).apply {
                text = desc
                textSize = 12f
                setTextColor(Color.parseColor("#546E7A"))
            })

            val claimBtn = Button(this).apply {
                text = "+$amount Coins"
                setBackgroundResource(R.drawable.bg_button_game_primary)
                setTextColor(Color.parseColor("#1B1B2F"))
                typeface = Typeface.DEFAULT_BOLD

                setOnClickListener {
                    soundManager.playCoin()
                    saveData.coins += amount
                    saveManager.saveData(saveData)
                    tvCoinsSec.text = "🪙 ${saveData.coins}"
                    Toast.makeText(this@MainActivity, "Collected $amount coins!", Toast.LENGTH_SHORT).show()
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
        title.text = "Worlds"
        tvCoinsSec.text = "${saveData.coins} Coins"

        incSecondary.findViewById<Button>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        WorldRepository.worlds.forEach { world ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 28, 28, 28)
                setBackgroundResource(R.drawable.bg_card_glossy)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    300
                ).apply { setMargins(0, 14, 0, 14) }
            }

            // Real artwork placeholder container
            val artView = android.widget.ImageView(this).apply {
                setBackgroundColor(Color.parseColor(world.skyColorHex))
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                // setImageResource(R.drawable.world_placeholder_art)
            }
            artView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            infoLayout.addView(TextView(this).apply {
                text = "World ${world.id}: ${world.name}"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1B1B2F"))
            })
            infoLayout.addView(TextView(this).apply {
                val endLvlText = if (world.endLevel > 10000) "+" else " - ${world.endLevel}"
                text = "Levels ${world.startLevel}$endLvlText"
                textSize = 14f
                setTextColor(Color.parseColor("#546E7A"))
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
                    text = "LOCKED: ${world.requiredStarsToUnlock} Stars"
                    setBackgroundResource(R.drawable.bg_button_game_secondary)
                    isEnabled = false
                }
                setTextColor(Color.parseColor("#1B1B2F"))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120).apply { setMargins(0, 16, 0, 0) }
            }

            card.addView(artView)
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
        title.text = "Achievements"
        tvCoinsSec.text = "${saveData.coins} Coins"

        incSecondary.findViewById<Button>(R.id.btnSecondaryBack).setOnClickListener {
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
                ).apply { setMargins(0, 12, 0, 12) }
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            infoLayout.addView(TextView(this).apply {
                text = item.title
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1B1B2F"))
            })
            infoLayout.addView(TextView(this).apply {
                text = item.description
                textSize = 12f
                setTextColor(Color.parseColor("#546E7A"))
            })

            val isUnlocked = item.isUnlocked(saveData)
            val isClaimed = saveData.unlockedAchievements.contains(item.id)

            val actionBtn = Button(this).apply {
                if (isClaimed) {
                    text = "✓ CLAIMED"
                    setBackgroundResource(R.drawable.bg_button_game_secondary)
                    isEnabled = false
                } else if (isUnlocked) {
                    text = "+${item.rewardCoins} Coins"
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
                setTextColor(Color.parseColor("#1B1B2F"))
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
        title.text = "Settings"
        tvCoinsSec.text = "${saveData.coins} Coins"

        incSecondary.findViewById<Button>(R.id.btnSecondaryBack).setOnClickListener {
            soundManager.playButtonClick()
            showScreen("MAIN_MENU")
        }

        val container = incSecondary.findViewById<LinearLayout>(R.id.secondaryContentContainer)
        container.removeAllViews()

        // Sound Toggle Button
        val soundBtn = Button(this).apply {
            text = "🔊 Sound: " + if (saveData.soundEnabled) "ON" else "OFF"
            setBackgroundResource(if (saveData.soundEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            setTextColor(Color.parseColor("#1B1B2F"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 130
            ).apply { setMargins(0, 12, 0, 12) }

            setOnClickListener {
                saveData.soundEnabled = !saveData.soundEnabled
                soundManager.soundEnabled = saveData.soundEnabled
                saveManager.saveData(saveData)
                text = "🔊 Sound: " + if (saveData.soundEnabled) "ON" else "OFF"
                setBackgroundResource(if (saveData.soundEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            }
        }
        container.addView(soundBtn)

        // Music Toggle Button
        val musicBtn = Button(this).apply {
            text = "🎵 Music: " + if (saveData.musicEnabled) "ON" else "OFF"
            setBackgroundResource(if (saveData.musicEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            setTextColor(Color.parseColor("#1B1B2F"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 130
            ).apply { setMargins(0, 12, 0, 12) }

            setOnClickListener {
                saveData.musicEnabled = !saveData.musicEnabled
                soundManager.musicEnabled = saveData.musicEnabled
                saveManager.saveData(saveData)
                text = "🎵 Music: " + if (saveData.musicEnabled) "ON" else "OFF"
                setBackgroundResource(if (saveData.musicEnabled) R.drawable.bg_button_game_primary else R.drawable.bg_button_game_secondary)
            }
        }
        container.addView(musicBtn)

        // Reset Progress Button
        val resetBtn = Button(this).apply {
            text = "♻ Reset Progress"
            setBackgroundResource(R.drawable.bg_button_game_secondary)
            setTextColor(Color.RED)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 130
            ).apply { setMargins(0, 24, 0, 12) }

            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Reset Progress?")
                    .setMessage("Are you sure you want to reset all game data? This cannot be undone.")
                    .setPositiveButton("Reset") { _, _ ->
                        saveManager.resetProgress()
                        saveData = saveManager.loadData()
                        updateCurrencyHUD()
                        showScreen("MAIN_MENU")
                        Toast.makeText(this@MainActivity, "Progress reset successfully.", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        container.addView(resetBtn)

        showScreen("SECONDARY")
    }
}
