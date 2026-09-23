package com.jumpadventure.game.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class SaveManager(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences("jump_adventure_save", Context.MODE_PRIVATE)
    )

    var currentLevel: Int
        get() = prefs.getInt("current_level", 1)
        set(value) = prefs.edit().putInt("current_level", value).apply()

    var highestUnlockedLevel: Int
        get() = prefs.getInt("highest_unlocked_level", 1)
        set(value) = prefs.edit().putInt("highest_unlocked_level", value.coerceAtLeast(1)).apply()

    var coins: Int
        get() = prefs.getInt("coins", 1250)
        set(value) = prefs.edit().putInt("coins", value.coerceAtLeast(0)).apply()

    var gems: Int
        get() = prefs.getInt("gems", 35)
        set(value) = prefs.edit().putInt("gems", value.coerceAtLeast(0)).apply()

    var selectedCharacter: String
        get() = prefs.getString("selected_character", "DEFAULT") ?: "DEFAULT"
        set(value) = prefs.edit().putString("selected_character", value).apply()

    var selectedSkin: String
        get() = prefs.getString("selected_skin", "RED_HOODIE") ?: "RED_HOODIE"
        set(value) = prefs.edit().putString("selected_skin", value).apply()

    var selectedTrail: String
        get() = prefs.getString("selected_trail", "NONE") ?: "NONE"
        set(value) = prefs.edit().putString("selected_trail", value).apply()

    var selectedPet: String
        get() = prefs.getString("selected_pet", "NONE") ?: "NONE"
        set(value) = prefs.edit().putString("selected_pet", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music_enabled", true)
        set(value) = prefs.edit().putBoolean("music_enabled", value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    var language: String
        get() = prefs.getString("language", "English") ?: "English"
        set(value) = prefs.edit().putString("language", value).apply()

    var dailyRewardDay: Int
        get() = prefs.getInt("daily_reward_day", 1)
        set(value) = prefs.edit().putInt("daily_reward_day", value).apply()

    var lastDailyClaimTime: Long
        get() = prefs.getLong("last_daily_claim_time", 0L)
        set(value) = prefs.edit().putLong("last_daily_claim_time", value).apply()

    fun getLevelStars(levelNumber: Int): Int {
        return prefs.getInt("level_stars_$levelNumber", 0)
    }

    fun setLevelStars(levelNumber: Int, stars: Int) {
        val currentStars = getLevelStars(levelNumber)
        if (stars > currentStars) {
            prefs.edit().putInt("level_stars_$levelNumber", stars).apply()
        }
    }

    fun isCharacterUnlocked(characterId: String): Boolean {
        if (characterId == "DEFAULT") return true
        val unlockedJson = prefs.getString("unlocked_characters", "[\"DEFAULT\"]") ?: "[\"DEFAULT\"]"
        val array = JSONArray(unlockedJson)
        for (i in 0 until array.length()) {
            if (array.getString(i) == characterId) return true
        }
        return false
    }

    fun unlockCharacter(characterId: String) {
        val unlockedJson = prefs.getString("unlocked_characters", "[\"DEFAULT\"]") ?: "[\"DEFAULT\"]"
        val array = JSONArray(unlockedJson)
        for (i in 0 until array.length()) {
            if (array.getString(i) == characterId) return
        }
        array.put(characterId)
        prefs.edit().putString("unlocked_characters", array.toString()).apply()
    }

    fun isSkinUnlocked(skinId: String): Boolean {
        if (skinId == "RED_HOODIE") return true
        val unlockedJson = prefs.getString("unlocked_skins", "[\"RED_HOODIE\"]") ?: "[\"RED_HOODIE\"]"
        val array = JSONArray(unlockedJson)
        for (i in 0 until array.length()) {
            if (array.getString(i) == skinId) return true
        }
        return false
    }

    fun unlockSkin(skinId: String) {
        val unlockedJson = prefs.getString("unlocked_skins", "[\"RED_HOODIE\"]") ?: "[\"RED_HOODIE\"]"
        val array = JSONArray(unlockedJson)
        for (i in 0 until array.length()) {
            if (array.getString(i) == skinId) return
        }
        array.put(skinId)
        prefs.edit().putString("unlocked_skins", array.toString()).apply()
    }

    fun isAchievementClaimed(achievementId: String): Boolean {
        return prefs.getBoolean("achievement_claimed_$achievementId", false)
    }

    fun claimAchievement(achievementId: String) {
        prefs.edit().putBoolean("achievement_claimed_$achievementId", true).apply()
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
    }
}
