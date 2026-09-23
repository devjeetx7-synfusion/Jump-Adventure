package com.jumpadventure.game.engine

import java.util.Random

class LevelData(
    val levelNumber: Int,
    val worldTheme: WorldTheme,
    val worldWidth: Float,
    val worldHeight: Float,
    val playerSpawnX: Float,
    val playerSpawnY: Float,
    val platforms: List<Platform>,
    val coins: List<Coin>,
    val stars: List<Star>,
    val spikes: List<Spike>,
    val enemies: List<Enemy>,
    val checkpoints: List<Checkpoint>,
    val powerUps: List<PowerUpItem>,
    val finishFlag: FinishFlag,
    val targetTimeSeconds: Float = 45f
)

object LevelGenerator {

    fun generateLevel(levelNumber: Int): LevelData {
        val theme = WorldTheme.getForLevel(levelNumber)
        val seed = levelNumber.toLong() * 31337L + 12345L
        val random = Random(seed)

        val groundY = 800f
        val playerSpawnX = 100f
        val playerSpawnY = groundY - 120f

        val platforms = mutableListOf<Platform>()
        val coins = mutableListOf<Coin>()
        val stars = mutableListOf<Star>()
        val spikes = mutableListOf<Spike>()
        val enemies = mutableListOf<Enemy>()
        val checkpoints = mutableListOf<Checkpoint>()
        val powerUps = mutableListOf<PowerUpItem>()

        platforms.add(Platform(20f, groundY, 350f, 40f, PlatformType.NORMAL))

        var currentX = 370f

        val sectionCount = when {
            levelNumber <= 10 -> 4 + levelNumber / 2
            levelNumber <= 50 -> 8 + levelNumber / 5
            levelNumber <= 250 -> 15 + levelNumber / 10
            else -> 25 + (levelNumber % 15)
        }

        for (i in 0 until sectionCount) {
            val sectionType = when {
                levelNumber <= 10 -> random.nextInt(3)
                levelNumber <= 25 -> random.nextInt(5)
                else -> random.nextInt(8)
            }

            when (sectionType) {
                0 -> {
                    val pWidth = 180f + random.nextInt(100)
                    val pY = groundY - random.nextInt(80)
                    platforms.add(Platform(currentX, pY, pWidth, 40f))

                    for (c in 0..2) {
                        coins.add(Coin(currentX + 30f + c * 40f, pY - 45f))
                    }
                    currentX += pWidth + 80f + random.nextInt(40)
                }
                1 -> {
                    currentX += 120f + random.nextInt(60)
                    val pWidth = 200f
                    val pY = groundY - 120f - random.nextInt(60)
                    platforms.add(Platform(currentX, pY, pWidth, 40f))

                    coins.add(Coin(currentX + 50f, pY - 45f))
                    coins.add(Coin(currentX + 100f, pY - 45f))

                    currentX += pWidth + 90f
                }
                2 -> {
                    val pWidth = 240f
                    val pY = groundY
                    platforms.add(Platform(currentX, pY, pWidth, 40f))
                    spikes.add(Spike(currentX + 80f, pY - 30f, 60f))

                    coins.add(Coin(currentX + 110f, pY - 90f))

                    currentX += pWidth + 80f
                }
                3 -> {
                    val pWidth = 160f
                    val pY = groundY - 60f
                    val moveX = 120f + random.nextInt(80)
                    platforms.add(Platform(currentX, pY, pWidth, 40f, PlatformType.MOVING, moveRangeX = moveX))

                    coins.add(Coin(currentX + 60f, pY - 45f))
                    currentX += pWidth + moveX + 100f
                }
                4 -> {
                    val pWidth = 280f
                    val pY = groundY
                    platforms.add(Platform(currentX, pY, pWidth, 40f))
                    enemies.add(Enemy(currentX + 100f, pY - 50f, currentX + 30f, currentX + pWidth - 30f))

                    currentX += pWidth + 80f
                }
                5 -> {
                    for (v in 0..2) {
                        val pY = groundY - v * 100f
                        platforms.add(Platform(currentX, pY, 140f, 40f))
                        if (v == 1) {
                            powerUps.add(PowerUpItem(currentX + 50f, pY - 45f, PowerUpType.values()[random.nextInt(PowerUpType.values().size)]))
                        }
                        currentX += 140f + 60f
                    }
                }
                6 -> {
                    val pY = groundY - 20f
                    platforms.add(Platform(currentX, pY, 150f, 40f, PlatformType.BOUNCY))

                    for (arc in 0..4) {
                        val cx = currentX + arc * 40f
                        val cy = pY - 80f - (2 - Math.abs(arc - 2)) * 30f
                        coins.add(Coin(cx, cy))
                    }
                    currentX += 200f
                }
                7 -> {
                    val pWidth = 200f
                    val pY = groundY
                    platforms.add(Platform(currentX, pY, pWidth, 40f))
                    checkpoints.add(Checkpoint(currentX + 80f, pY - 80f))
                    currentX += pWidth + 80f
                }
            }
        }

        val totalWidth = currentX
        stars.add(Star(totalWidth * 0.25f, groundY - 140f))
        stars.add(Star(totalWidth * 0.55f, groundY - 160f))
        stars.add(Star(totalWidth * 0.82f, groundY - 140f))

        val finalPlatWidth = 350f
        platforms.add(Platform(currentX, groundY, finalPlatWidth, 40f))
        val finishFlag = FinishFlag(currentX + 200f, groundY - 100f)

        val worldWidth = currentX + finalPlatWidth + 200f
        val worldHeight = groundY + 400f

        return LevelData(
            levelNumber = levelNumber,
            worldTheme = theme,
            worldWidth = worldWidth,
            worldHeight = worldHeight,
            playerSpawnX = playerSpawnX,
            playerSpawnY = playerSpawnY,
            platforms = platforms,
            coins = coins,
            stars = stars,
            spikes = spikes,
            enemies = enemies,
            checkpoints = checkpoints,
            powerUps = powerUps,
            finishFlag = finishFlag,
            targetTimeSeconds = (sectionCount * 3.5f).coerceAtLeast(15f)
        )
    }
}
