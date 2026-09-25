package com.jumpadventure.game.model

data class GameSaveData(
    var coins: Int = 1250,
    var gems: Int = 35,
    var currentLevel: Int = 1,
    var highestLevel: Int = 1,
    var playerSpeedMultiplier: Float = 1.15f,
    val levelStars: MutableMap<Int, Int> = mutableMapOf(),
    val unlockedWorlds: MutableSet<Int> = mutableSetOf(1),
    var selectedCharacter: String = "DEFAULT",
    val unlockedCharacters: MutableSet<String> = mutableSetOf("DEFAULT"),
    var selectedSkin: String = "DEFAULT",
    val unlockedSkins: MutableSet<String> = mutableSetOf("DEFAULT"),
    var selectedTrail: String = "NONE",
    val unlockedTrails: MutableSet<String> = mutableSetOf("NONE"),
    var selectedPet: String = "NONE",
    val unlockedPets: MutableSet<String> = mutableSetOf("NONE"),
    val unlockedAchievements: MutableSet<String> = mutableSetOf(),
    var dailyRewardLastClaimTimestamp: Long = 0L,
    var dailyRewardStreak: Int = 0,
    var soundEnabled: Boolean = true,
    var musicEnabled: Boolean = true,
    var vibrationEnabled: Boolean = true,
    var language: String = "en",
    var totalCoinsCollected: Int = 0,
    var totalJumps: Int = 0,
    var totalLevelsCompleted: Int = 0,
    var hasCustomControls: Boolean = false,
    var leftX: Float = -1f,
    var leftY: Float = -1f,
    var leftScale: Float = 1.0f,
    var rightX: Float = -1f,
    var rightY: Float = -1f,
    var rightScale: Float = 1.0f,
    var jumpX: Float = -1f,
    var jumpY: Float = -1f,
    var jumpScale: Float = 1.0f,
    var magnetX: Float = -1f,
    var magnetY: Float = -1f,
    var magnetScale: Float = 1.0f,
    var speedX: Float = -1f,
    var speedY: Float = -1f,
    var speedScale: Float = 1.0f,
    var shieldX: Float = -1f,
    var shieldY: Float = -1f,
    var shieldScale: Float = 1.0f,
    val powerUpLevels: MutableMap<String, Int> = mutableMapOf(
        "MAGNET" to 1,
        "SHIELD" to 1,
        "SPEED" to 1,
        "DOUBLE_COIN" to 1,
        "STAR_BOOST" to 1,
        "JUMP_BOOST" to 1
    )
)

data class CharacterItem(
    val id: String,
    val name: String,
    val priceCoins: Int,
    val priceGems: Int = 0,
    val description: String,
    val primaryColorHex: String,
    val skillName: String = "None",
    val skillDesc: String = "Standard jump & movement"
)

data class SkinCategoryItem(
    val id: String,
    val name: String,
    val category: String, // HOODIES, ARMOR, NINJA, ROBOT, FANTASY, SCI-FI, SPECIAL
    val priceCoins: Int,
    val priceGems: Int = 0,
    val colorHex: String,
    val description: String
)

data class TrailCategoryItem(
    val id: String,
    val name: String,
    val priceCoins: Int,
    val priceGems: Int = 0,
    val colorHex: String,
    val particleType: String,
    val description: String
)

data class PowerUpUpgradeItem(
    val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val basePriceCoins: Int,
    val maxLevel: Int = 5
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val rewardCoins: Int,
    val isUnlocked: (data: GameSaveData) -> Boolean
)

data class WorldInfo(
    val id: Int,
    val name: String,
    val iconEmoji: String,
    val startLevel: Int,
    val endLevel: Int,
    val skyColorHex: String,
    val platformColorHex: String,
    val dangerColorHex: String,
    val requiredStarsToUnlock: Int
)
