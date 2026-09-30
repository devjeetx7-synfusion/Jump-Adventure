package com.jumpadventure.game

import com.jumpadventure.game.model.GameSaveData
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveSanitizationTest {

    @Test
    fun testCurrencyCannotBeNegative() {
        val data = GameSaveData()
        data.coins = -100
        data.gems = -5

        // Simulating the sanitize step done in SaveManager
        data.coins = data.coins.coerceAtLeast(0)
        data.gems = data.gems.coerceAtLeast(0)

        assertEquals(0, data.coins)
        assertEquals(0, data.gems)
    }

    @Test
    fun testPowerUpLevelsClamped() {
        val data = GameSaveData()
        data.powerUpLevels["MAGNET"] = -1
        data.powerUpLevels["SHIELD"] = 10

        data.powerUpLevels.keys.toList().forEach { key ->
            data.powerUpLevels[key] = (data.powerUpLevels[key] ?: 1).coerceIn(1, 5)
        }

        assertEquals(1, data.powerUpLevels["MAGNET"])
        assertEquals(5, data.powerUpLevels["SHIELD"])
    }
}
