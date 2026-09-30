package com.jumpadventure.game

import com.jumpadventure.game.level.ElementType
import com.jumpadventure.game.level.LevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelSafetyTest {

    @Test
    fun generatedLevelsAlwaysContainExactlyThreeStars() {
        val levels = listOf(1, 2, 5, 10, 25, 50, 75, 100, 250, 500, 1000, 2500, 5000)
        levels.forEach { levelNumber ->
            val layout = LevelGenerator.generateLevel(levelNumber)
            assertEquals(
                "Level $levelNumber should contain exactly three stars",
                3,
                layout.elements.count { it.type == ElementType.STAR }
            )
        }
    }

    @Test
    fun everyStarHasStaticSupportBelowIt() {
        val levels = listOf(1, 3, 7, 25, 51, 100, 250, 1000, 5000)
        levels.forEach { levelNumber ->
            val layout = LevelGenerator.generateLevel(levelNumber)
            val platforms = layout.elements.filter { it.type == ElementType.PLATFORM }

            layout.elements.filter { it.type == ElementType.STAR }.forEach { star ->
                val starCenterX = star.x + star.width / 2f
                val supported = platforms.any { platform ->
                    val horizontallyAbove = starCenterX >= platform.x && starCenterX <= platform.x + platform.width
                    val verticalGap = platform.y - (star.y + star.height)
                    horizontallyAbove && verticalGap in 0f..140f
                }
                assertTrue(
                    "Star at x=${star.x}, y=${star.y} on level $levelNumber has no reachable static support",
                    supported
                )
            }
        }
    }

    @Test
    fun generatedLevelAlwaysEndsWithFinishDoorOnSupport() {
        listOf(1, 50, 100, 500, 2000, 5000).forEach { levelNumber ->
            val layout = LevelGenerator.generateLevel(levelNumber)
            val door = layout.elements.last { it.type == ElementType.FINISH_DOOR }
            val supported = layout.elements.any { platform ->
                platform.type == ElementType.PLATFORM &&
                    door.x + door.width > platform.x &&
                    door.x < platform.x + platform.width &&
                    door.y + door.height <= platform.y + 1f
            }
            assertTrue("Finish door must be supported on level $levelNumber", supported)
        }
    }
}
