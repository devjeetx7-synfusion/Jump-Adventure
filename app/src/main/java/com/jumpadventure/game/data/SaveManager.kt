package com.jumpadventure.game.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jumpadventure.game.model.GameSaveData
import org.json.JSONArray
import org.json.JSONObject

class SaveManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jump_adventure_save", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SaveManager"
        private const val CURRENT_SCHEMA_VERSION = 2

        const val KEY_SAVE_SCHEMA_VERSION = "save_schema_version"
        const val KEY_COINS = "coins"
        const val KEY_GEMS = "gems"
        const val KEY_CURRENT_LEVEL = "current_level"
        const val KEY_HIGHEST_LEVEL = "highest_level"
        const val KEY_SPEED_MULTIPLIER = "player_speed_multiplier"
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
        const val KEY_POWERUP_LEVELS_JSON = "powerup_levels_json"

        @Volatile
        private var INSTANCE: SaveManager? = null

        fun getInstance(context: Context): SaveManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SaveManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun loadData(): GameSaveData {
        migrateIfNeeded()

        val data = GameSaveData()
        data.coins = prefs.getInt(KEY_COINS, 1250)
        data.gems = prefs.getInt(KEY_GEMS, 35)
        data.currentLevel = prefs.getInt(KEY_CURRENT_LEVEL, 1).coerceAtLeast(1)
        data.highestLevel = prefs.getInt(KEY_HIGHEST_LEVEL, 1).coerceAtLeast(1)
        data.playerSpeedMultiplier = prefs.getFloat(KEY_SPEED_MULTIPLIER, 1.15f).coerceIn(0.75f, 1.50f)

        prefs.getString(KEY_LEVEL_STARS_JSON, null)?.takeIf { it.isNotBlank() }?.let { json ->
            runCatching {
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    data.levelStars[key.toInt()] = obj.getInt(key).coerceIn(0, 3)
                }
            }.onFailure { Log.w(TAG, "Ignoring malformed level stars save", it) }
        }

        prefs.getString(KEY_UNLOCKED_WORLDS_JSON, null)?.takeIf { it.isNotBlank() }?.let { json ->
            runCatching {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) data.unlockedWorlds.add(arr.getInt(i).coerceAtLeast(1))
            }.onFailure { Log.w(TAG, "Ignoring malformed unlocked worlds save", it) }
        }
        data.unlockedWorlds.add(1)

        data.selectedCharacter = prefs.getString(KEY_SELECTED_CHAR, "DEFAULT") ?: "DEFAULT"
        data.unlockedCharacters.addAll(getStringSet(KEY_UNLOCKED_CHARS, setOf("DEFAULT")))
        data.unlockedCharacters.add("DEFAULT")

        data.selectedSkin = prefs.getString(KEY_SELECTED_SKIN, "DEFAULT") ?: "DEFAULT"
        data.unlockedSkins.addAll(getStringSet(KEY_UNLOCKED_SKINS, setOf("DEFAULT")))
        data.unlockedSkins.add("DEFAULT")

        data.selectedTrail = prefs.getString(KEY_SELECTED_TRAIL, "NONE") ?: "NONE"
        data.unlockedTrails.addAll(getStringSet(KEY_UNLOCKED_TRAILS, setOf("NONE")))
        data.unlockedTrails.add("NONE")

        data.selectedPet = prefs.getString(KEY_SELECTED_PET, "NONE") ?: "NONE"
        data.unlockedPets.addAll(getStringSet(KEY_UNLOCKED_PETS, setOf("NONE")))
        data.unlockedPets.add("NONE")

        data.unlockedAchievements.addAll(getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, emptySet()))
        data.dailyRewardLastClaimTimestamp = prefs.getLong(KEY_DAILY_LAST_CLAIM, 0L).coerceAtLeast(0L)
        data.dailyRewardStreak = prefs.getInt(KEY_DAILY_STREAK, 0).coerceAtLeast(0)

        data.soundEnabled = prefs.getBoolean(KEY_SOUND, true)
        data.musicEnabled = prefs.getBoolean(KEY_MUSIC, true)
        data.vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true)
        data.language = prefs.getString(KEY_LANGUAGE, "en") ?: "en"

        data.totalCoinsCollected = prefs.getInt(KEY_TOTAL_COINS, 0).coerceAtLeast(0)
        data.totalJumps = prefs.getInt(KEY_TOTAL_JUMPS, 0).coerceAtLeast(0)
        data.totalLevelsCompleted = prefs.getInt(KEY_TOTAL_LEVELS, 0).coerceAtLeast(0)

        data.hasCustomControls = prefs.getBoolean(KEY_HAS_CUSTOM_CONTROLS, false)
        data.leftX = prefs.getFloat(KEY_LEFT_X, -1f)
        data.leftY = prefs.getFloat(KEY_LEFT_Y, -1f)
        data.leftScale = prefs.getFloat(KEY_LEFT_SCALE, 1.0f).coerceIn(0.6f, 1.6f)
        data.rightX = prefs.getFloat(KEY_RIGHT_X, -1f)
        data.rightY = prefs.getFloat(KEY_RIGHT_Y, -1f)
        data.rightScale = prefs.getFloat(KEY_RIGHT_SCALE, 1.0f).coerceIn(0.6f, 1.6f)
        data.jumpX = prefs.getFloat(KEY_JUMP_X, -1f)
        data.jumpY = prefs.getFloat(KEY_JUMP_Y, -1f)
        data.jumpScale = prefs.getFloat(KEY_JUMP_SCALE, 1.0f).coerceIn(0.6f, 1.6f)
        data.magnetX = prefs.getFloat(KEY_MAGNET_X, -1f)
        data.magnetY = prefs.getFloat(KEY_MAGNET_Y, -1f)
        data.magnetScale = prefs.getFloat(KEY_MAGNET_SCALE, 1.0f).coerceIn(0.6f, 1.6f)
        data.speedX = prefs.getFloat(KEY_SPEED_X, -1f)
        data.speedY = prefs.getFloat(KEY_SPEED_Y, -1f)
        data.speedScale = prefs.getFloat(KEY_SPEED_SCALE, 1.0f).coerceIn(0.6f, 1.6f)
        data.shieldX = prefs.getFloat(KEY_SHIELD_X, -1f)
        data.shieldY = prefs.getFloat(KEY_SHIELD_Y, -1f)
        data.shieldScale = prefs.getFloat(KEY_SHIELD_SCALE, 1.0f).coerceIn(0.6f, 1.6f)

        prefs.getString(KEY_POWERUP_LEVELS_JSON, null)?.takeIf { it.isNotBlank() }?.let { json ->
            runCatching {
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    data.powerUpLevels[key] = obj.getInt(key).coerceIn(1, 5)
                }
            }.onFailure { Log.w(TAG, "Ignoring malformed power-up levels save", it) }
        }

        sanitizeSelections(data)
        return data
    }

    @Synchronized
    fun saveData(data: GameSaveData) {
        sanitizeSelections(data)
        rejectUnsupportedCurrencyGrants(data)

        val editor = prefs.edit()
        editor.putInt(KEY_SAVE_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
        editor.putInt(KEY_COINS, data.coins.coerceAtLeast(0))
        editor.putInt(KEY_GEMS, data.gems.coerceAtLeast(0))
        editor.putInt(KEY_CURRENT_LEVEL, data.currentLevel.coerceAtLeast(1))
        editor.putInt(KEY_HIGHEST_LEVEL, data.highestLevel.coerceAtLeast(1))
        editor.putFloat(KEY_SPEED_MULTIPLIER, data.playerSpeedMultiplier.coerceIn(0.75f, 1.50f))

        val starsObj = JSONObject()
        data.levelStars.forEach { (level, stars) -> starsObj.put(level.toString(), stars.coerceIn(0, 3)) }
        editor.putString(KEY_LEVEL_STARS_JSON, starsObj.toString())

        val worldsArr = JSONArray()
        data.unlockedWorlds.sorted().forEach { worldsArr.put(it.coerceAtLeast(1)) }
        editor.putString(KEY_UNLOCKED_WORLDS_JSON, worldsArr.toString())

        editor.putString(KEY_SELECTED_CHAR, data.selectedCharacter)
        editor.putStringSet(KEY_UNLOCKED_CHARS, data.unlockedCharacters.toSet())
        editor.putString(KEY_SELECTED_SKIN, data.selectedSkin)
        editor.putStringSet(KEY_UNLOCKED_SKINS, data.unlockedSkins.toSet())
        editor.putString(KEY_SELECTED_TRAIL, data.selectedTrail)
        editor.putStringSet(KEY_UNLOCKED_TRAILS, data.unlockedTrails.toSet())
        editor.putString(KEY_SELECTED_PET, data.selectedPet)
        editor.putStringSet(KEY_UNLOCKED_PETS, data.unlockedPets.toSet())
        editor.putStringSet(KEY_UNLOCKED_ACHIEVEMENTS, data.unlockedAchievements.toSet())

        editor.putLong(KEY_DAILY_LAST_CLAIM, data.dailyRewardLastClaimTimestamp.coerceAtLeast(0L))
        editor.putInt(KEY_DAILY_STREAK, data.dailyRewardStreak.coerceAtLeast(0))
        editor.putBoolean(KEY_SOUND, data.soundEnabled)
        editor.putBoolean(KEY_MUSIC, data.musicEnabled)
        editor.putBoolean(KEY_VIBRATION, data.vibrationEnabled)
        editor.putString(KEY_LANGUAGE, data.language)

        editor.putInt(KEY_TOTAL_COINS, data.totalCoinsCollected.coerceAtLeast(0))
        editor.putInt(KEY_TOTAL_JUMPS, data.totalJumps.coerceAtLeast(0))
        editor.putInt(KEY_TOTAL_LEVELS, data.totalLevelsCompleted.coerceAtLeast(0))

        editor.putBoolean(KEY_HAS_CUSTOM_CONTROLS, data.hasCustomControls)
        editor.putFloat(KEY_LEFT_X, data.leftX)
        editor.putFloat(KEY_LEFT_Y, data.leftY)
        editor.putFloat(KEY_LEFT_SCALE, data.leftScale.coerceIn(0.6f, 1.6f))
        editor.putFloat(KEY_RIGHT_X, data.rightX)
        editor.putFloat(KEY_RIGHT_Y, data.rightY)
        editor.putFloat(KEY_RIGHT_SCALE, data.rightScale.coerceIn(0.6f, 1.6f))
        editor.putFloat(KEY_JUMP_X, data.jumpX)
        editor.putFloat(KEY_JUMP_Y, data.jumpY)
        editor.putFloat(KEY_JUMP_SCALE, data.jumpScale.coerceIn(0.6f, 1.6f))
        editor.putFloat(KEY_MAGNET_X, data.magnetX)
        editor.putFloat(KEY_MAGNET_Y, data.magnetY)
        editor.putFloat(KEY_MAGNET_SCALE, data.magnetScale.coerceIn(0.6f, 1.6f))
        editor.putFloat(KEY_SPEED_X, data.speedX)
        editor.putFloat(KEY_SPEED_Y, data.speedY)
        editor.putFloat(KEY_SPEED_SCALE, data.speedScale.coerceIn(0.6f, 1.6f))
        editor.putFloat(KEY_SHIELD_X, data.shieldX)
        editor.putFloat(KEY_SHIELD_Y, data.shieldY)
        editor.putFloat(KEY_SHIELD_SCALE, data.shieldScale.coerceIn(0.6f, 1.6f))

        val powerupsObj = JSONObject()
        data.powerUpLevels.forEach { (key, level) -> powerupsObj.put(key, level.coerceIn(1, 5)) }
        editor.putString(KEY_POWERUP_LEVELS_JSON, powerupsObj.toString())
        editor.apply()
    }

    /**
     * Current UI still contains a legacy CURRENCY tab whose CLAIM buttons mutate coins/gems
     * directly. Until a billing/reward provider is wired, currency increases are accepted only
     * when another trusted progression signal changed in the same save transaction.
     *
     * Legitimate examples:
     * - level reward: totalCoinsCollected / totalLevelsCompleted changes
     * - achievement reward: unlockedAchievements changes
     * - daily reward: daily claim timestamp changes
     * - world progression reward: unlockedWorlds/highestLevel changes
     *
     * A bare currency increase is rolled back to the persisted value, preventing unlimited
     * free-currency claims from becoming durable game state.
     */
    private fun rejectUnsupportedCurrencyGrants(data: GameSaveData) {
        if (!prefs.contains(KEY_COINS) && !prefs.contains(KEY_GEMS)) return

        val oldCoins = prefs.getInt(KEY_COINS, 1250)
        val oldGems = prefs.getInt(KEY_GEMS, 35)
        val oldTotalCoins = prefs.getInt(KEY_TOTAL_COINS, 0)
        val oldTotalLevels = prefs.getInt(KEY_TOTAL_LEVELS, 0)
        val oldDailyClaim = prefs.getLong(KEY_DAILY_LAST_CLAIM, 0L)
        val oldHighestLevel = prefs.getInt(KEY_HIGHEST_LEVEL, 1)
        val oldAchievements = prefs.getStringSet(KEY_UNLOCKED_ACHIEVEMENTS, emptySet()) ?: emptySet()
        val oldUnlockedWorldCount = readUnlockedWorldCount()

        val progressionSignal =
            data.totalCoinsCollected > oldTotalCoins ||
                data.totalLevelsCompleted > oldTotalLevels ||
                data.dailyRewardLastClaimTimestamp > oldDailyClaim ||
                data.highestLevel > oldHighestLevel ||
                data.unlockedAchievements.size > oldAchievements.size ||
                data.unlockedWorlds.size > oldUnlockedWorldCount

        if (data.coins > oldCoins && !progressionSignal) {
            Log.w(TAG, "Rejected unsupported coin grant: ${data.coins - oldCoins}")
            data.coins = oldCoins
        }

        if (data.gems > oldGems && !progressionSignal) {
            Log.w(TAG, "Rejected unsupported gem grant: ${data.gems - oldGems}")
            data.gems = oldGems
        }
    }

    private fun readUnlockedWorldCount(): Int {
        val json = prefs.getString(KEY_UNLOCKED_WORLDS_JSON, null) ?: return 1
        return runCatching { JSONArray(json).length().coerceAtLeast(1) }.getOrDefault(1)
    }

    private fun sanitizeSelections(data: GameSaveData) {
        data.coins = data.coins.coerceAtLeast(0)
        data.gems = data.gems.coerceAtLeast(0)
        data.currentLevel = data.currentLevel.coerceAtLeast(1)
        data.highestLevel = data.highestLevel.coerceAtLeast(1)
        data.playerSpeedMultiplier = data.playerSpeedMultiplier.coerceIn(0.75f, 1.50f)

        data.unlockedWorlds.add(1)
        data.unlockedCharacters.add("DEFAULT")
        data.unlockedSkins.add("DEFAULT")
        data.unlockedTrails.add("NONE")
        data.unlockedPets.add("NONE")

        if (!data.unlockedCharacters.contains(data.selectedCharacter)) data.selectedCharacter = "DEFAULT"
        if (!data.unlockedSkins.contains(data.selectedSkin)) data.selectedSkin = "DEFAULT"
        if (!data.unlockedTrails.contains(data.selectedTrail)) data.selectedTrail = "NONE"
        if (!data.unlockedPets.contains(data.selectedPet)) data.selectedPet = "NONE"

        data.powerUpLevels.keys.toList().forEach { key ->
            data.powerUpLevels[key] = (data.powerUpLevels[key] ?: 1).coerceIn(1, 5)
        }
    }

    private fun migrateIfNeeded() {
        val version = prefs.getInt(KEY_SAVE_SCHEMA_VERSION, 1)
        if (version >= CURRENT_SCHEMA_VERSION) return

        // v2 introduces schema tracking and stricter value sanitization. Existing keys remain
        // backwards compatible, so no destructive migration is required.
        prefs.edit().putInt(KEY_SAVE_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION).apply()
    }

    fun resetProgress() {
        prefs.edit().clear().apply()
    }

    private fun getStringSet(key: String, defaultSet: Set<String>): Set<String> {
        return prefs.getStringSet(key, null)?.toSet() ?: defaultSet
    }
}
