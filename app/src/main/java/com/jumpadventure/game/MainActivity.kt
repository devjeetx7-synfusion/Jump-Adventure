package com.jumpadventure.game

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jumpadventure.game.data.SaveManager
import com.jumpadventure.game.engine.*
import com.jumpadventure.game.ui.SettingsDialog
import com.jumpadventure.game.view.GameplayView

class MainActivity : AppCompatActivity(), PhysicsEngine.GamePhysicsCallback {

    private lateinit var saveManager: SaveManager

    private lateinit var mainMenuView: View
    private lateinit var levelMapView: View
    private lateinit var gameplayContainer: View
    private lateinit var charactersView: View
    private lateinit var shopView: View
    private lateinit var achievementsView: View

    private lateinit var gameplayView: GameplayView
    private lateinit var tvHudCoins: TextView
    private lateinit var tvHudStars: TextView
    private lateinit var tvHudLevelTitle: TextView
    private lateinit var pbLevelProgress: ProgressBar

    private var currentPlayingLevelData: LevelData? = null
    private var levelCoinsCollected: Int = 0
    private var levelStarsCollected: Int = 0
    private var levelStartTime: Long = 0L
    private var activeCheckpointX: Float? = null
    private var activeCheckpointY: Float? = null

    private var activeCustomizationTab: String = "HEROES"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveManager = SaveManager(this)

        setupUI()
        showScreen(SCREEN_MAIN_MENU)
    }

    private fun setupUI() {
        val rootLayout = FrameLayout(this)

        mainMenuView = layoutInflater.inflate(R.layout.activity_main_menu, rootLayout, false)
        levelMapView = layoutInflater.inflate(R.layout.activity_level_map, rootLayout, false)
        gameplayContainer = layoutInflater.inflate(R.layout.activity_gameplay, rootLayout, false)
        charactersView = layoutInflater.inflate(R.layout.activity_characters, rootLayout, false)
        shopView = layoutInflater.inflate(R.layout.activity_shop, rootLayout, false)
        achievementsView = layoutInflater.inflate(R.layout.activity_achievements, rootLayout, false)

        rootLayout.addView(mainMenuView)
        rootLayout.addView(levelMapView)
        rootLayout.addView(gameplayContainer)
        rootLayout.addView(charactersView)
        rootLayout.addView(shopView)
        rootLayout.addView(achievementsView)

        setContentView(rootLayout)

        gameplayView = gameplayContainer.findViewById(R.id.gameplayView)
        gameplayView.physicsEngine.callback = this
        tvHudCoins = gameplayContainer.findViewById(R.id.tvHudCoins)
        tvHudStars = gameplayContainer.findViewById(R.id.tvHudStars)
        tvHudLevelTitle = gameplayContainer.findViewById(R.id.tvHudLevelTitle)
        pbLevelProgress = gameplayContainer.findViewById(R.id.pbLevelProgress)

        gameplayContainer.findViewById<View>(R.id.btnPause).setOnClickListener {
            showPauseDialog()
        }

        setupMainMenuBindings()
        setupLevelMapBindings()
        setupCharactersBindings()
        setupShopBindings()
        setupAchievementsBindings()
    }

    private fun showScreen(screenId: Int) {
        mainMenuView.visibility = if (screenId == SCREEN_MAIN_MENU) View.VISIBLE else View.GONE
        levelMapView.visibility = if (screenId == SCREEN_LEVEL_MAP) View.VISIBLE else View.GONE
        gameplayContainer.visibility = if (screenId == SCREEN_GAMEPLAY) View.VISIBLE else View.GONE
        charactersView.visibility = if (screenId == SCREEN_CHARACTERS) View.VISIBLE else View.GONE
        shopView.visibility = if (screenId == SCREEN_SHOP) View.VISIBLE else View.GONE
        achievementsView.visibility = if (screenId == SCREEN_ACHIEVEMENTS) View.VISIBLE else View.GONE

        if (screenId == SCREEN_MAIN_MENU) {
            updateMainMenuUI()
        } else if (screenId == SCREEN_LEVEL_MAP) {
            populateLevelMap()
        } else if (screenId == SCREEN_CHARACTERS) {
            populateCustomizationGrid()
        } else if (screenId == SCREEN_ACHIEVEMENTS) {
            populateAchievementsList()
        }
    }

    private fun updateMainMenuUI() {
        mainMenuView.findViewById<TextView>(R.id.tvCoins).text = saveManager.coins.toString()
        mainMenuView.findViewById<TextView>(R.id.tvGems).text = saveManager.gems.toString()

        val btnPlay = mainMenuView.findViewById<Button>(R.id.btnPlay)
        btnPlay.text = "▶ PLAY\nLevel ${saveManager.currentLevel}"

        val previewView = mainMenuView.findViewById<GameplayView>(R.id.characterPreviewView)
        previewView.player.characterId = saveManager.selectedCharacter
        previewView.player.skinId = saveManager.selectedSkin
        previewView.isPaused = true
        previewView.invalidate()
    }

    private fun setupMainMenuBindings() {
        mainMenuView.findViewById<View>(R.id.btnSettings).setOnClickListener {
            SettingsDialog(this, saveManager) {
                updateMainMenuUI()
            }.show()
        }

        mainMenuView.findViewById<View>(R.id.btnPlay).setOnClickListener {
            startLevel(saveManager.currentLevel)
        }

        mainMenuView.findViewById<View>(R.id.btnNavShop).setOnClickListener {
            showScreen(SCREEN_SHOP)
        }

        mainMenuView.findViewById<View>(R.id.btnNavCharacters).setOnClickListener {
            showScreen(SCREEN_CHARACTERS)
        }

        mainMenuView.findViewById<View>(R.id.btnNavWorlds).setOnClickListener {
            showScreen(SCREEN_LEVEL_MAP)
        }

        mainMenuView.findViewById<View>(R.id.btnNavAchievements).setOnClickListener {
            showScreen(SCREEN_ACHIEVEMENTS)
        }
    }

    private fun setupLevelMapBindings() {
        levelMapView.findViewById<View>(R.id.btnMapBack).setOnClickListener {
            showScreen(SCREEN_MAIN_MENU)
        }
    }

    private fun populateLevelMap() {
        val container = levelMapView.findViewById<LinearLayout>(R.id.levelNodesContainer)
        container.removeAllViews()

        val worldTheme = WorldTheme.getForLevel(saveManager.currentLevel)
        levelMapView.findViewById<TextView>(R.id.tvMapWorldTitle).text = worldTheme.worldName

        val startLevel = (worldTheme.worldId - 1) * 20 + 1
        val endLevel = startLevel + 19

        for (lvl in startLevel..endLevel) {
            val isUnlocked = lvl <= saveManager.highestUnlockedLevel
            val stars = saveManager.getLevelStars(lvl)

            val nodeBtn = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    120
                ).apply {
                    setMargins(0, 16, 0, 16)
                }

                if (isUnlocked) {
                    text = if (lvl % 10 == 0) "👹 BOSS LEVEL $lvl  [ ${"⭐".repeat(stars)} ]" else "Level $lvl   ${"⭐".repeat(stars)}"
                    setBackgroundResource(R.drawable.bg_btn_primary)
                    setTextColor(Color.parseColor("#111111"))
                    setOnClickListener { startLevel(lvl) }
                } else {
                    text = "🔒 Level $lvl"
                    setBackgroundResource(R.drawable.bg_card_rounded)
                    setTextColor(Color.parseColor("#888888"))
                    isEnabled = false
                }
            }
            container.addView(nodeBtn)
        }
    }

    private fun startLevel(levelNumber: Int) {
        val levelData = LevelGenerator.generateLevel(levelNumber)
        currentPlayingLevelData = levelData
        levelCoinsCollected = 0
        levelStarsCollected = 0
        levelStartTime = System.currentTimeMillis()
        activeCheckpointX = null
        activeCheckpointY = null

        saveManager.currentLevel = levelNumber

        val player = Player(levelData.playerSpawnX, levelData.playerSpawnY).apply {
            characterId = saveManager.selectedCharacter
            skinId = saveManager.selectedSkin
        }

        gameplayView.loadLevelData(
            theme = levelData.worldTheme,
            width = levelData.worldWidth,
            height = levelData.worldHeight,
            p = player,
            plats = levelData.platforms,
            cns = levelData.coins,
            strs = levelData.stars,
            spks = levelData.spikes,
            enms = levelData.enemies,
            cps = levelData.checkpoints,
            pups = levelData.powerUps,
            finish = levelData.finishFlag
        )

        gameplayView.isPaused = false
        tvHudLevelTitle.text = "Level $levelNumber"
        tvHudCoins.text = "0"
        tvHudStars.text = "⭐ 0/3"
        pbLevelProgress.progress = 0

        showScreen(SCREEN_GAMEPLAY)
    }

    override fun onCoinCollected(coin: Coin) {
        levelCoinsCollected += 10
        saveManager.coins += 10
        tvHudCoins.text = levelCoinsCollected.toString()
    }

    override fun onStarCollected(star: Star) {
        levelStarsCollected += 1
        tvHudStars.text = "⭐ $levelStarsCollected/3"
    }

    override fun onPowerUpCollected(powerUp: PowerUpItem) {}

    override fun onCheckpointReached(checkpoint: Checkpoint) {
        activeCheckpointX = checkpoint.x
        activeCheckpointY = checkpoint.y - 50f
    }

    override fun onFinishReached() {
        if (gameplayView.isPaused) return
        gameplayView.isPaused = true

        val timeTakenSec = (System.currentTimeMillis() - levelStartTime) / 1000f
        val currentLvl = currentPlayingLevelData?.levelNumber ?: 1

        val starsEarned = levelStarsCollected.coerceIn(1, 3)
        saveManager.setLevelStars(currentLvl, starsEarned)

        if (currentLvl == saveManager.highestUnlockedLevel) {
            saveManager.highestUnlockedLevel = currentLvl + 1
        }

        showLevelCompleteDialog(currentLvl, levelCoinsCollected, starsEarned, timeTakenSec)
    }

    override fun onPlayerHurt() {
        if (activeCheckpointX != null && activeCheckpointY != null) {
            gameplayView.player.reset(activeCheckpointX!!, activeCheckpointY!!)
        } else {
            val currentLvl = currentPlayingLevelData?.levelNumber ?: 1
            startLevel(currentLvl)
        }
    }

    private fun showPauseDialog() {
        gameplayView.isPaused = true
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_pause)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<View>(R.id.btnResume).setOnClickListener {
            dialog.dismiss()
            gameplayView.isPaused = false
            gameplayView.postInvalidateOnAnimation()
        }

        dialog.findViewById<View>(R.id.btnRestart).setOnClickListener {
            dialog.dismiss()
            currentPlayingLevelData?.let { startLevel(it.levelNumber) }
        }

        dialog.findViewById<View>(R.id.btnPauseSettings).setOnClickListener {
            SettingsDialog(this, saveManager) {}.show()
        }

        dialog.findViewById<View>(R.id.btnExitHome).setOnClickListener {
            dialog.dismiss()
            showScreen(SCREEN_MAIN_MENU)
        }

        dialog.show()
    }

    private fun showLevelCompleteDialog(levelNum: Int, coinsEarned: Int, starsEarned: Int, timeTakenSec: Float) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_level_complete)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(R.id.tvStarsDisplay).text = "⭐".repeat(starsEarned)
        dialog.findViewById<TextView>(R.id.tvCompleteLevelName).text = "Level $levelNum"
        dialog.findViewById<TextView>(R.id.tvRewardCoins).text = "🪙 Coins Earned: +$coinsEarned"
        dialog.findViewById<TextView>(R.id.tvRewardStars).text = "⭐ Stars Collected: $starsEarned/3"
        dialog.findViewById<TextView>(R.id.tvRewardTime).text = "⏱ Time: ${String.format("%.1fs", timeTakenSec)}"

        dialog.findViewById<View>(R.id.btnNextLevel).setOnClickListener {
            dialog.dismiss()
            startLevel(levelNum + 1)
        }

        dialog.findViewById<View>(R.id.btnReplay).setOnClickListener {
            dialog.dismiss()
            startLevel(levelNum)
        }

        dialog.findViewById<View>(R.id.btnHome).setOnClickListener {
            dialog.dismiss()
            showScreen(SCREEN_MAIN_MENU)
        }

        dialog.show()
    }

    private fun setupCharactersBindings() {
        charactersView.findViewById<View>(R.id.btnCharactersBack).setOnClickListener {
            showScreen(SCREEN_MAIN_MENU)
        }

        charactersView.findViewById<View>(R.id.tabCharacters).setOnClickListener {
            activeCustomizationTab = "HEROES"
            populateCustomizationGrid()
        }
        charactersView.findViewById<View>(R.id.tabSkins).setOnClickListener {
            activeCustomizationTab = "SKINS"
            populateCustomizationGrid()
        }
        charactersView.findViewById<View>(R.id.tabTrails).setOnClickListener {
            activeCustomizationTab = "TRAILS"
            populateCustomizationGrid()
        }
        charactersView.findViewById<View>(R.id.tabPets).setOnClickListener {
            activeCustomizationTab = "PETS"
            populateCustomizationGrid()
        }
    }

    private fun populateCustomizationGrid() {
        val grid = charactersView.findViewById<GridLayout>(R.id.gridCustomization)
        grid.removeAllViews()

        val items = when (activeCustomizationTab) {
            "HEROES" -> listOf(
                CustomItem("DEFAULT", "Red Hoodie", 0, "DEFAULT"),
                CustomItem("NINJA", "Ninja Shadow", 3000, "HERO"),
                CustomItem("ROBOT", "Cyber Bot", 5000, "HERO"),
                CustomItem("GIRL", "Runner Girl", 3000, "HERO"),
                CustomItem("PIRATE", "Captain Hook", 4000, "HERO"),
                CustomItem("COWBOY", "Wild West", 4000, "HERO")
            )
            "SKINS" -> listOf(
                CustomItem("RED_HOODIE", "Red Hoodie", 0, "SKIN"),
                CustomItem("BLUE_HOODIE", "Blue Hoodie", 1000, "SKIN"),
                CustomItem("GREEN_HOODIE", "Green Hoodie", 1000, "SKIN"),
                CustomItem("GOLDEN_OUTFIT", "Golden Outfit", 2500, "SKIN"),
                CustomItem("CYBER_OUTFIT", "Neon Outfit", 2500, "SKIN")
            )
            "TRAILS" -> listOf(
                CustomItem("FIRE", "Fire Trail", 1500, "TRAIL"),
                CustomItem("LIGHTNING", "Lightning Trail", 2000, "TRAIL"),
                CustomItem("RAINBOW", "Rainbow Trail", 2500, "TRAIL")
            )
            else -> listOf(
                CustomItem("ROBOT_PET", "Mini Bot", 2000, "PET"),
                CustomItem("FOX_PET", "Foxy", 3000, "PET"),
                CustomItem("DRAGON_PET", "Baby Dragon", 5000, "PET")
            )
        }

        for (item in items) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_card_rounded)
                setPadding(16, 16, 16, 16)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(12, 12, 12, 12)
                }
            }

            val tvTitle = TextView(this).apply {
                text = item.title
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
            }

            val isUnlocked = when (item.category) {
                "HERO", "DEFAULT" -> saveManager.isCharacterUnlocked(item.id)
                "SKIN" -> saveManager.isSkinUnlocked(item.id)
                else -> true
            }

            val isEquipped = when (item.category) {
                "HERO", "DEFAULT" -> saveManager.selectedCharacter == item.id
                "SKIN" -> saveManager.selectedSkin == item.id
                "TRAIL" -> saveManager.selectedTrail == item.id
                else -> saveManager.selectedPet == item.id
            }

            val btnAction = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 80
                ).apply { topMargin = 12 }

                if (isEquipped) {
                    text = "✓ EQUIPPED"
                    setBackgroundColor(Color.parseColor("#4CAF50"))
                    setTextColor(Color.WHITE)
                } else if (isUnlocked) {
                    text = "EQUIP"
                    setBackgroundResource(R.drawable.bg_btn_primary)
                    setTextColor(Color.BLACK)
                    setOnClickListener {
                        when (item.category) {
                            "HERO", "DEFAULT" -> saveManager.selectedCharacter = item.id
                            "SKIN" -> saveManager.selectedSkin = item.id
                            "TRAIL" -> saveManager.selectedTrail = item.id
                            else -> saveManager.selectedPet = item.id
                        }
                        populateCustomizationGrid()
                    }
                } else {
                    text = "🪙 ${item.price}"
                    setBackgroundResource(R.drawable.bg_btn_primary)
                    setTextColor(Color.BLACK)
                    setOnClickListener {
                        if (saveManager.coins >= item.price) {
                            saveManager.coins -= item.price
                            when (item.category) {
                                "HERO" -> saveManager.unlockCharacter(item.id)
                                "SKIN" -> saveManager.unlockSkin(item.id)
                            }
                            populateCustomizationGrid()
                        } else {
                            Toast.makeText(this@MainActivity, "Not enough coins!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            card.addView(tvTitle)
            card.addView(btnAction)
            grid.addView(card)
        }
    }

    private fun setupShopBindings() {
        shopView.findViewById<View>(R.id.btnShopBack).setOnClickListener {
            showScreen(SCREEN_MAIN_MENU)
        }

        shopView.findViewById<View>(R.id.btnBuyCoinsSmall).setOnClickListener {
            saveManager.coins += 500
            Toast.makeText(this, "+500 Coins added!", Toast.LENGTH_SHORT).show()
        }

        shopView.findViewById<View>(R.id.btnBuyCoinsLarge).setOnClickListener {
            saveManager.coins += 5000
            Toast.makeText(this, "+5000 Coins added!", Toast.LENGTH_SHORT).show()
        }

        shopView.findViewById<View>(R.id.btnBuyMagnet).setOnClickListener {
            if (saveManager.coins >= 200) {
                saveManager.coins -= 200
                Toast.makeText(this, "Magnet Boost Unlocked!", Toast.LENGTH_SHORT).show()
            }
        }

        shopView.findViewById<View>(R.id.btnBuyShield).setOnClickListener {
            if (saveManager.coins >= 300) {
                saveManager.coins -= 300
                Toast.makeText(this, "Hazard Shield Unlocked!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAchievementsBindings() {
        achievementsView.findViewById<View>(R.id.btnAchievementsBack).setOnClickListener {
            showScreen(SCREEN_MAIN_MENU)
        }
    }

    private fun populateAchievementsList() {
        val container = achievementsView.findViewById<LinearLayout>(R.id.containerAchievements)
        container.removeAllViews()

        val achievements = listOf(
            Achievement("A1", "🏆 First Jump", "Complete Level 1", 100),
            Achievement("A2", "🏆 Explorer", "Complete 10 levels", 250),
            Achievement("A3", "🏆 Coin Collector", "Collect 1,000 coins", 500),
            Achievement("A4", "🏆 Star Player", "Earn 50 stars", 1000)
        )

        for (ach in achievements) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_card_rounded)
                setPadding(20, 20, 20, 20)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 10, 0, 10) }
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTitle = TextView(this).apply {
                text = ach.title
                setTextColor(Color.WHITE)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            }

            val tvDesc = TextView(this).apply {
                text = ach.desc
                setTextColor(Color.parseColor("#CCCCCC"))
                textSize = 12f
            }

            textLayout.addView(tvTitle)
            textLayout.addView(tvDesc)

            val isClaimed = saveManager.isAchievementClaimed(ach.id)
            val btnClaim = Button(this).apply {
                layoutParams = LinearLayout.LayoutParams(220, 90)
                if (isClaimed) {
                    text = "CLAIMED"
                    isEnabled = false
                } else {
                    text = "🪙 ${ach.reward}"
                    setBackgroundResource(R.drawable.bg_btn_primary)
                    setOnClickListener {
                        saveManager.coins += ach.reward
                        saveManager.claimAchievement(ach.id)
                        populateAchievementsList()
                    }
                }
            }

            card.addView(textLayout)
            card.addView(btnClaim)
            container.addView(card)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (gameplayContainer.visibility == View.VISIBLE) {
            showPauseDialog()
        } else if (mainMenuView.visibility != View.VISIBLE) {
            showScreen(SCREEN_MAIN_MENU)
        } else {
            super.onBackPressed()
        }
    }

    private data class CustomItem(val id: String, val title: String, val price: Int, val category: String)
    private data class Achievement(val id: String, val title: String, val desc: String, val reward: Int)

    companion object {
        const val SCREEN_MAIN_MENU = 1
        const val SCREEN_LEVEL_MAP = 2
        const val SCREEN_GAMEPLAY = 3
        const val SCREEN_CHARACTERS = 4
        const val SCREEN_SHOP = 5
        const val SCREEN_ACHIEVEMENTS = 6
    }
}
