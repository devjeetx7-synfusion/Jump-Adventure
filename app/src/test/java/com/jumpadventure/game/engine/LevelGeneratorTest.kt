package com.jumpadventure.game.engine

import org.junit.Assert.*
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun testLevelGeneratorDeterminism() {
        val level1A = LevelGenerator.generateLevel(1)
        val level1B = LevelGenerator.generateLevel(1)

        assertEquals(level1A.platforms.size, level1B.platforms.size)
        assertEquals(level1A.coins.size, level1B.coins.size)
        assertEquals(level1A.worldWidth, level1B.worldWidth, 0.001f)
        assertEquals(level1A.finishFlag.x, level1B.finishFlag.x, 0.001f)

        val level101A = LevelGenerator.generateLevel(101)
        val level101B = LevelGenerator.generateLevel(101)

        assertEquals(level101A.platforms.size, level101B.platforms.size)
        assertEquals(level101A.coins.size, level101B.coins.size)
    }

    @Test
    fun testInfiniteLevelGeneration() {
        val level500 = LevelGenerator.generateLevel(500)
        val level1000 = LevelGenerator.generateLevel(1000)

        assertTrue(level500.platforms.isNotEmpty())
        assertTrue(level500.coins.isNotEmpty())
        assertTrue(level500.stars.size == 3)
        assertNotNull(level500.finishFlag)

        assertTrue(level1000.platforms.isNotEmpty())
        assertEquals(WorldTheme.FINAL_KINGDOM, level1000.worldTheme)
    }
}
