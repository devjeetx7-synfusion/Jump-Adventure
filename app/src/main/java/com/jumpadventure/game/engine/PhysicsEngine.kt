package com.jumpadventure.game.engine

import android.graphics.RectF
import kotlin.math.hypot

class PhysicsEngine {

    interface GamePhysicsCallback {
        fun onCoinCollected(coin: Coin)
        fun onStarCollected(star: Star)
        fun onPowerUpCollected(powerUp: PowerUpItem)
        fun onCheckpointReached(checkpoint: Checkpoint)
        fun onFinishReached()
        fun onPlayerHurt()
    }

    var callback: GamePhysicsCallback? = null

    fun update(
        player: Player,
        platforms: List<Platform>,
        coins: List<Coin>,
        stars: List<Star>,
        spikes: List<Spike>,
        enemies: List<Enemy>,
        checkpoints: List<Checkpoint>,
        finishFlag: FinishFlag?,
        powerUps: List<PowerUpItem>,
        deltaTime: Float,
        worldHeight: Float
    ) {
        platforms.forEach { it.update(deltaTime) }
        enemies.forEach { if (it.active) it.update(deltaTime) }
        coins.forEach { if (!it.collected) it.update(deltaTime) }
        stars.forEach { if (!it.collected) it.update(deltaTime) }
        powerUps.forEach { if (!it.collected) it.update(deltaTime) }

        val effectiveSpeed = if (player.speedTimer > 0) player.baseSpeed * 1.35f else player.baseSpeed
        player.vx = player.vx.coerceIn(-effectiveSpeed, effectiveSpeed)

        val oldY = player.y
        player.update(deltaTime)

        player.isGrounded = false
        val pBounds = player.getBounds()

        for (platform in platforms) {
            val platBounds = platform.getBounds()
            if (RectF.intersects(pBounds, platBounds)) {
                if (oldY + player.height <= platform.y + 16f && player.vy >= 0) {
                    player.y = platform.y - player.height
                    player.vy = 0f
                    player.isGrounded = true

                    if (platform.type == PlatformType.BOUNCY) {
                        player.vy = player.jumpVelocity * 1.35f
                        player.isGrounded = false
                    } else if (platform.type == PlatformType.MOVING) {
                        player.x += platform.dx
                        player.y += platform.dy
                    }
                }
            }
        }

        if (player.magnetTimer > 0) {
            val magnetRadius = 250f
            val playerCenterX = player.x + player.width / 2f
            val playerCenterY = player.y + player.height / 2f

            for (coin in coins) {
                if (!coin.collected) {
                    val coinCX = coin.x + coin.width / 2f
                    val coinCY = coin.y + coin.height / 2f
                    val dist = hypot(playerCenterX - coinCX, playerCenterY - coinCY)
                    if (dist < magnetRadius) {
                        val dx = (playerCenterX - coinCX) * 8f * deltaTime
                        val dy = (playerCenterY - coinCY) * 8f * deltaTime
                        coin.x += dx
                        coin.y += dy
                    }
                }
            }
        }

        for (coin in coins) {
            if (!coin.collected && RectF.intersects(pBounds, coin.getBounds())) {
                coin.collected = true
                callback?.onCoinCollected(coin)
            }
        }

        for (star in stars) {
            if (!star.collected && RectF.intersects(pBounds, star.getBounds())) {
                star.collected = true
                callback?.onStarCollected(star)
            }
        }

        for (powerUp in powerUps) {
            if (!powerUp.collected && RectF.intersects(pBounds, powerUp.getBounds())) {
                powerUp.collected = true
                when (powerUp.powerUpType) {
                    PowerUpType.MAGNET -> player.magnetTimer = 10f
                    PowerUpType.SHIELD -> player.shieldActive = true
                    PowerUpType.SPEED -> player.speedTimer = 8f
                    PowerUpType.EXTRA_LIFE -> {}
                    PowerUpType.STAR_BOOST -> {}
                }
                callback?.onPowerUpCollected(powerUp)
            }
        }

        for (cp in checkpoints) {
            if (!cp.activated && RectF.intersects(pBounds, cp.getBounds())) {
                cp.activated = true
                callback?.onCheckpointReached(cp)
            }
        }

        if (finishFlag != null && RectF.intersects(pBounds, finishFlag.getBounds())) {
            callback?.onFinishReached()
        }

        for (spike in spikes) {
            if (RectF.intersects(pBounds, spike.getBounds())) {
                handleHazardHit(player)
                return
            }
        }

        for (enemy in enemies) {
            if (enemy.active && RectF.intersects(pBounds, enemy.getBounds())) {
                if (oldY + player.height <= enemy.y + 15f && player.vy > 0) {
                    enemy.active = false
                    player.vy = player.jumpVelocity * 0.65f
                } else {
                    handleHazardHit(player)
                    return
                }
            }
        }

        if (player.y > worldHeight + 300f) {
            handleHazardHit(player)
        }
    }

    private fun handleHazardHit(player: Player) {
        if (player.shieldActive) {
            player.shieldActive = false
        } else {
            callback?.onPlayerHurt()
        }
    }
}
