package com.jumpadventure.game.model

data class GameSaveData(
    var coins: Int = 1250,
    var gems: Int = 35,
    var currentLevel: Int = 1,
    var highestLevel: Int = 1,
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
    var totalLevelsCompleted: Int = 0
)

data class CharacterItem(
    val id: String,
    val name: String,
    val priceCoins: Int,
    val priceGems: Int = 0,
    val description: String,
    val primaryColorHex: String
)

data class SkinItem(
    val id: String,
    val name: String,
    val priceCoins: Int,
    val colorHex: String
)

data class TrailItem(
    val id: String,
    val name: String,
    val priceCoins: Int,
    val particleType: String
)

data class PetItem(
    val id: String,
    val name: String,
    val priceCoins: Int,
    val emoji: String
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
