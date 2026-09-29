package com.jumpadventure.game.level

import java.util.Random
import kotlin.math.abs

enum class ElementType {
    PLATFORM,
    MOVING_PLATFORM,
    BOX,
    SPIKE,
    COIN,
    STAR,
    GEM,
    POWERUP_MAGNET,
    POWERUP_SHIELD,
    POWERUP_SPEED,
    POWERUP_HIGH_JUMP,
    POWERUP_POWER,
    ENEMY,
    CHECKPOINT,
    FINISH_DOOR
}

data class LevelElement(
    val type: ElementType,
    val x: Float,
    val y: Float,
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

    /** Deterministic procedural generation: the same level number always produces the same map. */
    fun generateLevel(levelNumber: Int): LevelLayout {
        val safeLevel = levelNumber.coerceAtLeast(1)
        val world = WorldRepository.getWorldForLevel(safeLevel)
        val seed = (safeLevel * 31337L) xor 0x5DEECE66DL
        val random = Random(seed)
        val elements = mutableListOf<LevelElement>()
        val starSafePlatforms = mutableListOf<LevelElement>()

        fun addPlatform(
            type: ElementType,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            moveDistanceX: Float = 0f,
            speed: Float = 0f,
            starSafe: Boolean = true
        ): LevelElement {
            val platform = LevelElement(type, x, y, width, height, moveDistanceX, speed)
            elements.add(platform)
            if (starSafe && width >= 90f) starSafePlatforms.add(platform)
            return platform
        }

        var currentX = 0f
        val groundY = 800f
        addPlatform(ElementType.PLATFORM, 0f, groundY, 300f, 40f)
        currentX += 300f

        val numSections = when {
            safeLevel <= 5 -> 4
            safeLevel <= 10 -> 6
            safeLevel <= 25 -> 8
            safeLevel <= 50 -> 12
            safeLevel <= 100 -> 16
            else -> (18 + ((safeLevel - 100) / 50)).coerceAtMost(28)
        }

        var totalStarsPlaced = 0
        val diffScale = (1.0f + (safeLevel / 100f) * 0.15f).coerceAtMost(1.8f)

        for (sectionIdx in 0 until numSections) {
            val sectionType = selectSectionType(safeLevel, random)
            val gap = when {
                safeLevel <= 10 -> 80f + random.nextFloat() * 40f
                safeLevel <= 50 -> 100f + random.nextFloat() * 60f
                else -> (110f + random.nextFloat() * 70f + ((safeLevel - 50) / 250f).coerceAtMost(20f)).coerceAtMost(200f)
            }
            currentX += gap

            when (sectionType) {
                "SECTION_A" -> {
                    for (i in 0..2) {
                        val platY = groundY - (i % 2) * 60f
                        val platW = 120f
                        addPlatform(ElementType.PLATFORM, currentX, platY, platW, 35f)
                        elements.add(LevelElement(ElementType.COIN, currentX + 30f, platY - 40f, 30f, 30f))
                        elements.add(LevelElement(ElementType.COIN, currentX + 70f, platY - 40f, 30f, 30f))
                        currentX += platW + 40f
                    }
                }

                "SECTION_B" -> {
                    val platY = groundY - 100f
                    val platW = 180f
                    addPlatform(ElementType.PLATFORM, currentX, platY, platW, 35f)
                    for (c in 0..2) {
                        elements.add(LevelElement(ElementType.COIN, currentX + 30f + c * 40f, platY - 45f, 30f, 30f))
                    }
                    if (totalStarsPlaced < 3 && random.nextBoolean()) {
                        elements.add(LevelElement(ElementType.STAR, currentX + 90f, platY - 90f, 35f, 35f))
                        totalStarsPlaced++
                    } else if (random.nextFloat() < 0.35f) {
                        elements.add(LevelElement(ElementType.POWERUP_HIGH_JUMP, currentX + 130f, platY - 80f, 32f, 32f))
                    }
                    currentX += platW
                }

                "SECTION_C" -> {
                    val platW = 220f
                    addPlatform(ElementType.PLATFORM, currentX, groundY, platW, 35f)
                    elements.add(LevelElement(ElementType.BOX, currentX + 30f, groundY - 40f, 40f, 40f))
                    elements.add(LevelElement(ElementType.SPIKE, currentX + 110f, groundY - 30f, 40f, 30f))
                    elements.add(LevelElement(ElementType.COIN, currentX + 170f, groundY - 40f, 30f, 30f))
                    currentX += platW
                }

                "SECTION_D" -> {
                    val platY = groundY - 50f
                    val platW = 140f
                    val moveDist = 120f + random.nextInt(80)
                    val platSpeed = ((2f + random.nextFloat() * 1.5f) * diffScale).coerceAtMost(5.0f)
                    addPlatform(
                        ElementType.MOVING_PLATFORM,
                        currentX,
                        platY,
                        platW,
                        35f,
                        moveDistanceX = moveDist,
                        speed = platSpeed,
                        starSafe = false
                    )
                    elements.add(LevelElement(ElementType.COIN, currentX + 30f, platY - 40f, 30f, 30f))
                    elements.add(LevelElement(ElementType.COIN, currentX + 70f, platY - 40f, 30f, 30f))
                    if (random.nextFloat() < 0.4f) {
                        val pool = listOf(ElementType.POWERUP_SHIELD, ElementType.POWERUP_MAGNET)
                        elements.add(LevelElement(pool[random.nextInt(pool.size)], currentX + 105f, platY - 45f, 32f, 32f))
                    }
                    currentX += platW + moveDist
                }

                "SECTION_E" -> {
                    val platW = 200f
                    addPlatform(ElementType.PLATFORM, currentX, groundY, platW, 35f)
                    val enemySpeed = (1.5f * diffScale).coerceAtMost(3.5f)
                    elements.add(
                        LevelElement(
                            ElementType.ENEMY,
                            currentX + 80f,
                            groundY - 35f,
                            35f,
                            35f,
                            moveDistanceX = 70f,
                            speed = enemySpeed
                        )
                    )
                    if (random.nextFloat() < 0.35f) {
                        val pool = listOf(ElementType.POWERUP_POWER, ElementType.GEM)
                        elements.add(LevelElement(pool[random.nextInt(pool.size)], currentX + 150f, groundY - 75f, 32f, 32f))
                    }
                    currentX += platW
                }

                "SECTION_F" -> {
                    var stepY = groundY
                    for (i in 0..2) {
                        stepY -= 70f
                        addPlatform(ElementType.PLATFORM, currentX, stepY, 110f, 35f)
                        elements.add(LevelElement(ElementType.COIN, currentX + 40f, stepY - 40f, 30f, 30f))
                        currentX += 90f
                    }
                    if (totalStarsPlaced < 3) {
                        elements.add(LevelElement(ElementType.STAR, currentX - 50f, stepY - 50f, 35f, 35f))
                        totalStarsPlaced++
                    }
                }

                "SECTION_G" -> {
                    val platW = 250f
                    addPlatform(ElementType.PLATFORM, currentX, groundY - 30f, platW, 35f)
                    for (c in 0..4) {
                        elements.add(LevelElement(ElementType.COIN, currentX + 20f + c * 45f, groundY - 75f, 30f, 30f))
                    }
                    if (random.nextFloat() < 0.45f) {
                        elements.add(LevelElement(ElementType.POWERUP_SPEED, currentX + 215f, groundY - 80f, 32f, 32f))
                    }
                    currentX += platW
                }

                "SECTION_H" -> {
                    val platW = 180f
                    addPlatform(ElementType.PLATFORM, currentX, groundY, platW, 35f)
                    elements.add(LevelElement(ElementType.CHECKPOINT, currentX + 70f, groundY - 50f, 30f, 50f))
                    currentX += platW
                }
            }
        }

        if (totalStarsPlaced < 3) {
            val candidates = starSafePlatforms
                .filter { it.x >= 250f }
                .sortedBy { it.x }
                .ifEmpty { starSafePlatforms.sortedBy { it.x } }
            val usedX = elements.filter { it.type == ElementType.STAR }.map { it.x }.toMutableList()

            while (totalStarsPlaced < 3 && candidates.isNotEmpty()) {
                val desiredFraction = (totalStarsPlaced + 1f) / 4f
                var index = (desiredFraction * (candidates.size - 1)).toInt().coerceIn(0, candidates.lastIndex)
                var platform = candidates[index]

                if (usedX.any { abs(it - (platform.x + platform.width * 0.5f)) < 70f }) {
                    index = candidates.indices.maxByOrNull { i ->
                        val x = candidates[i].x + candidates[i].width * 0.5f
                        usedX.minOfOrNull { abs(it - x) } ?: Float.MAX_VALUE
                    } ?: index
                    platform = candidates[index]
                }

                val starX = platform.x + (platform.width - 35f) * 0.5f
                val starY = platform.y - 72f
                elements.add(LevelElement(ElementType.STAR, starX, starY, 35f, 35f))
                usedX.add(starX)
                totalStarsPlaced++
            }
        }

        currentX += 80f
        addPlatform(ElementType.PLATFORM, currentX, groundY, 250f, 40f)
        elements.add(LevelElement(ElementType.FINISH_DOOR, currentX + 120f, groundY - 70f, 50f, 70f))

        return LevelLayout(
            levelNumber = safeLevel,
            worldId = world.id,
            totalWidth = currentX + 250f,
            elements = elements
        )
    }

    private fun selectSectionType(levelNumber: Int, random: Random): String {
        val pool = when {
            levelNumber <= 5 -> listOf("SECTION_A", "SECTION_B", "SECTION_G")
            levelNumber <= 25 -> listOf("SECTION_A", "SECTION_B", "SECTION_C", "SECTION_G")
            levelNumber <= 50 -> listOf("SECTION_A", "SECTION_B", "SECTION_C", "SECTION_D", "SECTION_G", "SECTION_H")
            else -> listOf("SECTION_A", "SECTION_B", "SECTION_C", "SECTION_D", "SECTION_E", "SECTION_F", "SECTION_G", "SECTION_H")
        }
        return pool[random.nextInt(pool.size)]
    }
}
