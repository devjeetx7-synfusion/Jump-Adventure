package com.jumpadventure.game.data

import android.content.Context
import android.content.SharedPreferences
import com.jumpadventure.game.model.*
import org.json.JSONArray
import org.json.JSONObject

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jump_adventure_save", Context.MODE_PRIVATE)

    companion object {
        const val KEY_COINS = "coins"
        const val KEY_GEMS = "gems"
        const val KEY_CURRENT_LEVEL = "current_level"
        const val KEY_HIGHEST_LEVEL = "highest_level"
        const val KEY_LEVEL_STARS_JSON = "level_stars_json"
        const val KEY_UNLOCKED_WORLDS_JSON = "unlocked_worlds_json"
        const val KEY_SELECTED_CHAR = "selected_char"
        const val KEY_UNLOCKED_CHARS = "unlocked_chars"
        const val KEY_SELECTED_SKIN = "selected_skin"
        const val KEY_UNLOCKED_SKINS = "unlocked_skins"
        const val KEY_SELECTED_TRAIL = "selected_trail"
        const val KEY_UNLOCKED_TRAILS = "unlocked_trails"
        const val KEY_SELECTED_PET = "selected_pet"
        const val KEY_UNLOCKED_PETS = "unlocked_pets"
        const val KEY_UNLOCKED_ACHIEVEMENTS = "unlocked_achievements"
        const val KEY_DAILY_LAST_CLAIM = "daily_last_claim"
        const val KEY_DAILY_STREAK = "daily_streak"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_MUSIC = "music_enabled"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_LANGUAGE = "language"
        const val KEY_TOTAL_COINS = "total_coins_collected"
        const val KEY_TOTAL_JUMPS = "total_jumps"
        const val KEY_TOTAL_LEVELS = "total_levels_completed"

        const val KEY_HAS_CUSTOM_CONTROLS = "has_custom_controls"
        const val KEY_LEFT_X = "ctrl_left_x"
        const val KEY_LEFT_Y = "ctrl_left_y"
        const val KEY_LEFT_SCALE = "ctrl_left_scale"
        const val KEY_RIGHT_X = "ctrl_right_x"
        const val KEY_RIGHT_Y = "ctrl_right_y"
        const val KEY_RIGHT_SCALE = "ctrl_right_scale"
        const val KEY_JUMP_X = "ctrl_jump_x"
        const val KEY_JUMP_Y = "ctrl_jump_y"
        const val KEY_JUMP_SCALE = "ctrl_jump_scale"
        const val KEY_MAGNET_X = "ctrl_magnet_x"
        const val KEY_MAGNET_Y = "ctrl_magnet_y"
        const val KEY_MAGNET_SCALE = "ctrl_magnet_scale"
        const val KEY_SPEED_X = "ctrl_speed_x"
        const val KEY_SPEED_Y = "ctrl_speed_y"
        const val KEY_SPEED_SCALE = "ctrl_speed_scale"
        const val KEY_SHIELD_X = "ctrl_shield_x"
        const val KEY_SHIELD_Y = "ctrl_shield_y"
        const val KEY_SHIELD_SCALE = "ctrl_shield_scale"

        @Volatile
        private var INSTANCE: SaveManager? = null

        fun getInstance(context: Context): SaveManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SaveManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun loadData(): GameSaveData {
        val data = GameSaveData()
        data.coins = prefs.getInt(KEY_COINS, 1250)
        data.gems = prefs.getInt(KEY_GEMS, 35)
        data.currentLevel = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        data.highestLevel = prefs.getInt(KEY_HIGHEST_LEVEL, 1)

        val starsJson = prefs.getString(KEY_LEVEL_STARS_JSON, null)
        if (!starsJson.isNullOrEmpty()) {
            try {
                val obj = JSONObject(starsJson)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    data.levelStars[k.toInt()] = obj.getInt(k)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val worldsJson = prefs.getString(KEY_UNLOCKED_WORLDS_JSON, null)
        if (!worldsJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(worldsJson)
                for (i in 0 until arr.length()) {
                    data.unlockedWorlds.add(arr.getInt(i))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            data.unlockedWorlds.add(1)
        }

        data.selectedCharacter = prefs.getString(KEY_SELECTED_CHAR, "DEFAULT") ?: "DEFAULT"
        data.unlockedCharacters.addAll(getStringSet(KEY_UNLOCKED_CHARS, setOf("DEFAULT")))

        data.selectedSkin = prefs.getString(KEY_SELECTED_SKIN, "DEFAULT") ?: "DEFAULT"
        data.unlockedSkins.addAll(getStringSet(KEY_UNLOCKED_SKINS, setOf("DEFAULT")))

        data.selectedTrail = prefs.getString(KEY_SELECTED_TRAIL, "NONE") ?: "NONE"
        data.unlockedTrails.addAll(getStringSet(KEY_UNLOCKED_TRAILS, setOf("NONE")))

        data.selectedPet = prefs.getString(KEY_SELECTED_PET, "NONE") ?: "NONE"
        data.unlockedPets.addAll(getStringSet(KEY_UNLOCKED_PETS, setOf("NONE")))

        data.unlockedAchievements.addAll(getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, emptySet()))

        data.dailyRewardLastClaimTimestamp = prefs.getLong(KEY_DAILY_LAST_CLAIM, 0L)
        data.dailyRewardStreak = prefs.getInt(KEY_DAILY_STREAK, 0)

        data.soundEnabled = prefs.getBoolean(KEY_SOUND, true)
        data.musicEnabled = prefs.getBoolean(KEY_MUSIC, true)
        data.vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true)
        data.language = prefs.getString(KEY_LANGUAGE, "en") ?: "en"

        data.totalCoinsCollected = prefs.getInt(KEY_TOTAL_COINS, 0)
        data.totalJumps = prefs.getInt(KEY_TOTAL_JUMPS, 0)
        data.totalLevelsCompleted = prefs.getInt(KEY_TOTAL_LEVELS, 0)

        data.hasCustomControls = prefs.getBoolean(KEY_HAS_CUSTOM_CONTROLS, false)
        data.leftX = prefs.getFloat(KEY_LEFT_X, -1f)
        data.leftY = prefs.getFloat(KEY_LEFT_Y, -1f)
        data.leftScale = prefs.getFloat(KEY_LEFT_SCALE, 1.0f)
        data.rightX = prefs.getFloat(KEY_RIGHT_X, -1f)
        data.rightY = prefs.getFloat(KEY_RIGHT_Y, -1f)
        data.rightScale = prefs.getFloat(KEY_RIGHT_SCALE, 1.0f)
        data.jumpX = prefs.getFloat(KEY_JUMP_X, -1f)
        data.jumpY = prefs.getFloat(KEY_JUMP_Y, -1f)
        data.jumpScale = prefs.getFloat(KEY_JUMP_SCALE, 1.0f)
        data.magnetX = prefs.getFloat(KEY_MAGNET_X, -1f)
        data.magnetY = prefs.getFloat(KEY_MAGNET_Y, -1f)
        data.magnetScale = prefs.getFloat(KEY_MAGNET_SCALE, 1.0f)
        data.speedX = prefs.getFloat(KEY_SPEED_X, -1f)
        data.speedY = prefs.getFloat(KEY_SPEED_Y, -1f)
        data.speedScale = prefs.getFloat(KEY_SPEED_SCALE, 1.0f)
        data.shieldX = prefs.getFloat(KEY_SHIELD_X, -1f)
        data.shieldY = prefs.getFloat(KEY_SHIELD_Y, -1f)
        data.shieldScale = prefs.getFloat(KEY_SHIELD_SCALE, 1.0f)

        return data
    }

    fun saveData(data: GameSaveData) {
        val editor = prefs.edit()
        editor.putInt(KEY_COINS, data.coins)
        editor.putInt(KEY_GEMS, data.gems)
        editor.putInt(KEY_CURRENT_LEVEL, data.currentLevel)
        editor.putInt(KEY_HIGHEST_LEVEL, data.highestLevel)

        val starsObj = JSONObject()
        data.levelStars.forEach { (lvl, stars) ->
            starsObj.put(lvl.toString(), stars)
        }
        editor.putString(KEY_LEVEL_STARS_JSON, starsObj.toString())

        val worldsArr = JSONArray()
        data.unlockedWorlds.forEach { worldsArr.put(it) }
        editor.putString(KEY_UNLOCKED_WORLDS_JSON, worldsArr.toString())

        editor.putString(KEY_SELECTED_CHAR, data.selectedCharacter)
        editor.putStringSet(KEY_UNLOCKED_CHARS, data.unlockedCharacters)

        editor.putString(KEY_SELECTED_SKIN, data.selectedSkin)
        editor.putStringSet(KEY_UNLOCKED_SKINS, data.unlockedSkins)

        editor.putString(KEY_SELECTED_TRAIL, data.selectedTrail)
        editor.putStringSet(KEY_UNLOCKED_TRAILS, data.unlockedTrails)

        editor.putString(KEY_SELECTED_PET, data.selectedPet)
        editor.putStringSet(KEY_UNLOCKED_PETS, data.unlockedPets)

        editor.putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, data.unlockedAchievements)

        editor.putLong(KEY_DAILY_LAST_CLAIM, data.dailyRewardLastClaimTimestamp)
        editor.putInt(KEY_DAILY_STREAK, data.dailyRewardStreak)

        editor.putBoolean(KEY_SOUND, data.soundEnabled)
        editor.putBoolean(KEY_MUSIC, data.musicEnabled)
        editor.putBoolean(KEY_VIBRATION, data.vibrationEnabled)
        editor.putString(KEY_LANGUAGE, data.language)

        editor.putInt(KEY_TOTAL_COINS, data.totalCoinsCollected)
        editor.putInt(KEY_TOTAL_JUMPS, data.totalJumps)
        editor.putInt(KEY_TOTAL_LEVELS, data.totalLevelsCompleted)

        editor.putBoolean(KEY_HAS_CUSTOM_CONTROLS, data.hasCustomControls)
        editor.putFloat(KEY_LEFT_X, data.leftX)
        editor.putFloat(KEY_LEFT_Y, data.leftY)
        editor.putFloat(KEY_LEFT_SCALE, data.leftScale)
        editor.putFloat(KEY_RIGHT_X, data.rightX)
        editor.putFloat(KEY_RIGHT_Y, data.rightY)
        editor.putFloat(KEY_RIGHT_SCALE, data.rightScale)
        editor.putFloat(KEY_JUMP_X, data.jumpX)
        editor.putFloat(KEY_JUMP_Y, data.jumpY)
        editor.putFloat(KEY_JUMP_SCALE, data.jumpScale)
        editor.putFloat(KEY_MAGNET_X, data.magnetX)
        editor.putFloat(KEY_MAGNET_Y, data.magnetY)
        editor.putFloat(KEY_MAGNET_SCALE, data.magnetScale)
        editor.putFloat(KEY_SPEED_X, data.speedX)
        editor.putFloat(KEY_SPEED_Y, data.speedY)
        editor.putFloat(KEY_SPEED_SCALE, data.speedScale)
        editor.putFloat(KEY_SHIELD_X, data.shieldX)
        editor.putFloat(KEY_SHIELD_Y, data.shieldY)
        editor.putFloat(KEY_SHIELD_SCALE, data.shieldScale)

        editor.apply()
    }

    fun resetProgress() {
        prefs.edit().clear().apply()
    }

    private fun getStringSet(key: String, defaultSet: Set<String>): Set<String> {
        val set = prefs.getStringSet(key, null)
        return set ?: defaultSet
    }
}
