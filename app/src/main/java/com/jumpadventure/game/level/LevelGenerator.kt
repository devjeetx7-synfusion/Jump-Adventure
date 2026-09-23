package com.jumpadventure.game.level

import java.util.Random

enum class ElementType {
    PLATFORM,
    MOVING_PLATFORM,
    BOX,
    SPIKE,
    COIN,
    STAR,
    ENEMY,
    CHECKPOINT,
    FINISH_DOOR
}

data class LevelElement(
    val type: ElementType,
    val x: Float, // Relative X in world units
    val y: Float, // Relative Y in world units
    val width: Float = 100f,
    val height: Float = 30f,
    val moveDistanceX: Float = 0f,
    val speed: Float = 0f
)

data class LevelLayout(
    val levelNumber: Int,
    val worldId: Int,
    val totalWidth: Float,
    val elements: List<LevelElement>
)

object LevelGenerator {

    /**
     * Deterministic level generation using seed derived from levelNumber.
     */
    fun generateLevel(levelNumber: Int): LevelLayout {
        val world = WorldRepository.getWorldForLevel(levelNumber)
        val seed = (levelNumber * 31337L) xor 0x5DEECE66DL
        val random = Random(seed)

        val elements = mutableListOf<LevelElement>()

        // Starting ground platform
        var currentX = 0f
        val groundY = 800f
        elements.add(LevelElement(ElementType.PLATFORM, x = 0f, y = groundY, width = 300f, height = 40f))

        currentX += 300f

        // Determine section count based on level progression
        val numSections = when {
            levelNumber <= 5 -> 4
            levelNumber <= 10 -> 6
            levelNumber <= 25 -> 8
            levelNumber <= 50 -> 12
            levelNumber <= 100 -> 16
            else -> 20 + (levelNumber % 10)
        }

        var totalStarsPlaced = 0

        for (sectionIdx in 0 until numSections) {
            // Pick section template based on level difficulty
            val sectionType = selectSectionType(levelNumber, random)

            val gap = when {
                levelNumber <= 10 -> 80f + random.nextFloat() * 40f
                levelNumber <= 50 -> 100f + random.nextFloat() * 60f
                else -> 120f + random.nextFloat() * 80f
            }

            currentX += gap

            when (sectionType) {
                // SECTION_A: 3 platforms + coins
                "SECTION_A" -> {
                    for (i in 0..2) {
                        val platY = groundY - (i % 2) * 60f
                        val platW = 120f
                        elements.add(LevelElement(ElementType.PLATFORM, currentX, platY, platW, 35f))
                        // Coins on platform
                        elements.add(LevelElement(ElementType.COIN, currentX + 30f, platY - 40f, 30f, 30f))
                        elements.add(LevelElement(ElementType.COIN, currentX + 70f, platY - 40f, 30f, 30f))
                        currentX += platW + 40f
                    }
                }

                // SECTION_B: Gap + high platform + coins
                "SECTION_B" -> {
                    val platY = groundY - 100f
                    val platW = 180f
                    elements.add(LevelElement(ElementType.PLATFORM, currentX, platY, platW, 35f))
                    // Coins arc
                    for (c in 0..2) {
                        elements.add(LevelElement(ElementType.COIN, currentX + 30f + c * 40f, platY - 45f, 30f, 30f))
                    }
                    if (totalStarsPlaced < 3 && random.nextBoolean()) {
                        elements.add(LevelElement(ElementType.STAR, currentX + 90f, platY - 90f, 35f, 35f))
                        totalStarsPlaced++
                    }
                    currentX += platW
                }

                // SECTION_C: Spikes + safe platform
                "SECTION_C" -> {
                    val platW = 220f
                    elements.add(LevelElement(ElementType.PLATFORM, currentX, groundY, platW, 35f))
                    // Wooden box in middle
                    elements.add(LevelElement(ElementType.BOX, currentX + 30f, groundY - 40f, 40f, 40f))
                    // Spike hazard
                    elements.add(LevelElement(ElementType.SPIKE, currentX + 110f, groundY - 30f, 40f, 30f))
                    // Coins
                    elements.add(LevelElement(ElementType.COIN, currentX + 170f, groundY - 40f, 30f, 30f))
                    currentX += platW
                }

                // SECTION_D: Moving platform
                "SECTION_D" -> {
                    val platY = groundY - 50f
                    val platW = 140f
                    val moveDist = 120f + random.nextInt(80)
                    elements.add(LevelElement(
                        ElementType.MOVING_PLATFORM,
                        currentX, platY, platW, 35f,
                        moveDistanceX = moveDist,
                        speed = 2f + random.nextFloat() * 1.5f
                    ))
                    // Coins above moving platform
                    elements.add(LevelElement(ElementType.COIN, currentX + 40f, platY - 40f, 30f, 30f))
                    elements.add(LevelElement(ElementType.COIN, currentX + 80f, platY - 40f, 30f, 30f))
                    currentX += platW + moveDist
                }

                // SECTION_E: Enemy + platform
                "SECTION_E" -> {
                    val platW = 200f
                    elements.add(LevelElement(ElementType.PLATFORM, currentX, groundY, platW, 35f))
                    // Enemy walking on platform
                    elements.add(LevelElement(
                        ElementType.ENEMY,
                        currentX + 80f, groundY - 35f, 35f, 35f,
                        moveDistanceX = 70f,
                        speed = 1.5f
                    ))
                    currentX += platW
                }

                // SECTION_F: Vertical platform steps
                "SECTION_F" -> {
                    var stepY = groundY
                    for (i in 0..2) {
                        stepY -= 70f
                        elements.add(LevelElement(ElementType.PLATFORM, currentX, stepY, 110f, 35f))
                        elements.add(LevelElement(ElementType.COIN, currentX + 40f, stepY - 40f, 30f, 30f))
                        currentX += 90f
                    }
                    if (totalStarsPlaced < 3) {
                        elements.add(LevelElement(ElementType.STAR, currentX - 50f, stepY - 50f, 35f, 35f))
                        totalStarsPlaced++
                    }
                }

                // SECTION_G: Bonus coin section
                "SECTION_G" -> {
                    val platW = 250f
                    elements.add(LevelElement(ElementType.PLATFORM, currentX, groundY - 30f, platW, 35f))
                    for (c in 0..4) {
                        elements.add(LevelElement(ElementType.COIN, currentX + 20f + c * 45f, groundY - 75f, 30f, 30f))
                    }
                    currentX += platW
                }

                // SECTION_H: Checkpoint / Rest platform
                "SECTION_H" -> {
                    val platW = 180f
                    elements.add(LevelElement(ElementType.PLATFORM, currentX, groundY, platW, 35f))
                    elements.add(LevelElement(ElementType.CHECKPOINT, currentX + 70f, groundY - 50f, 30f, 50f))
                    currentX += platW
                }
            }
        }

        // Ensure exactly 3 stars are placed in every level if not placed already
        while (totalStarsPlaced < 3) {
            val targetX = 200f + totalStarsPlaced * (currentX / 3.5f)
            elements.add(LevelElement(ElementType.STAR, targetX, groundY - 90f, 35f, 35f))
            totalStarsPlaced++
        }

        // Final finish platform and finish door
        currentX += 80f
        elements.add(LevelElement(ElementType.PLATFORM, currentX, groundY, 250f, 40f))
        elements.add(LevelElement(ElementType.FINISH_DOOR, currentX + 120f, groundY - 70f, 50f, 70f))

        val totalWidth = currentX + 250f

        return LevelLayout(
            levelNumber = levelNumber,
            worldId = world.id,
            totalWidth = totalWidth,
            elements = elements
        )
    }

    private fun selectSectionType(levelNumber: Int, random: Random): String {
        return when {
            levelNumber <= 5 -> {
                val pool = listOf("SECTION_A", "SECTION_B", "SECTION_G")
                pool[random.nextInt(pool.size)]
            }
            levelNumber <= 25 -> {
                val pool = listOf("SECTION_A", "SECTION_B", "SECTION_C", "SECTION_G")
                pool[random.nextInt(pool.size)]
            }
            levelNumber <= 50 -> {
                val pool = listOf("SECTION_A", "SECTION_B", "SECTION_C", "SECTION_D", "SECTION_G", "SECTION_H")
                pool[random.nextInt(pool.size)]
            }
            else -> {
                val pool = listOf("SECTION_A", "SECTION_B", "SECTION_C", "SECTION_D", "SECTION_E", "SECTION_F", "SECTION_G", "SECTION_H")
                pool[random.nextInt(pool.size)]
            }
        }
    }
}
