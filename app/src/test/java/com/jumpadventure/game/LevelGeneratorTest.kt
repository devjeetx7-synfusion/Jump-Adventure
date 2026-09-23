package com.jumpadventure.game

import com.jumpadventure.game.level.LevelGenerator
import com.jumpadventure.game.level.WorldRepository
import org.junit.Assert.*
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun testLevelGenerationIsDeterministic() {
        val level1A = LevelGenerator.generateLevel(1)
        val level1B = LevelGenerator.generateLevel(1)

        assertEquals(level1A.elements.size, level1B.elements.size)
        assertEquals(level1A.totalWidth, level1B.totalWidth, 0.001f)

        for (i in level1A.elements.indices) {
            assertEquals(level1A.elements[i].type, level1B.elements[i].type)
            assertEquals(level1A.elements[i].x, level1B.elements[i].x, 0.001f)
            assertEquals(level1A.elements[i].y, level1B.elements[i].y, 0.001f)
        }
    }

    @Test
    fun testUnlimitedLevelsCanBeGenerated() {
        val level1000 = LevelGenerator.generateLevel(1000)
        assertTrue(level1000.elements.isNotEmpty())
        assertTrue(level1000.totalWidth > 1000f)
    }

    @Test
    fun testWorldRepositoryMapping() {
        val w1 = WorldRepository.getWorldForLevel(1)
        assertEquals(1, w1.id)

        val w5 = WorldRepository.getWorldForLevel(150)
        assertEquals(5, w5.id)

        val w10 = WorldRepository.getWorldForLevel(2000)
        assertEquals(10, w10.id)
    }
}
