package com.jumpadventure.game.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sin

enum class PowerUpType {
    MAGNET,
    SHIELD,
    SPEED,
    EXTRA_LIFE,
    STAR_BOOST
}

open class GameObject(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float
) {
    var active: Boolean = true

    fun getBounds(): RectF {
        return RectF(x, y, x + width, y + height)
    }

    open fun update(deltaTime: Float) {}
    open fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {}
}

class Player(x: Float, y: Float) : GameObject(x, y, 60f, 90f) {
    var vx: Float = 0f
    var vy: Float = 0f

    var isGrounded: Boolean = false
    var isFacingRight: Boolean = true
    var stateTime: Float = 0f

    var characterId: String = "DEFAULT"
    var skinId: String = "RED_HOODIE"

    var magnetTimer: Float = 0f
    var shieldActive: Boolean = false
    var speedTimer: Float = 0f

    val baseSpeed: Float = 380f
    val jumpVelocity: Float = -850f
    val gravity: Float = 1900f

    fun reset(spawnX: Float, spawnY: Float) {
        x = spawnX
        y = spawnY
        vx = 0f
        vy = 0f
        isGrounded = false
        magnetTimer = 0f
        shieldActive = false
        speedTimer = 0f
        active = true
    }

    override fun update(deltaTime: Float) {
        stateTime += deltaTime

        if (magnetTimer > 0) magnetTimer -= deltaTime
        if (speedTimer > 0) speedTimer -= deltaTime

        if (!isGrounded) {
            vy += gravity * deltaTime
        }

        x += vx * deltaTime
        y += vy * deltaTime
    }

    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        paint.color = 0x40000000.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawOval(RectF(screenX, screenY + height - 10f, screenX + width, screenY + height + 5f), paint)

        if (shieldActive) {
            paint.color = 0x8000E6FF.toInt()
            canvas.drawCircle(screenX + width / 2f, screenY + height / 2f, width * 0.85f, paint)
        }

        if (magnetTimer > 0) {
            paint.color = 0x60FFD700.toInt()
            canvas.drawCircle(screenX + width / 2f, screenY + height / 2f, width * 0.75f, paint)
        }

        val mainColor = when (characterId) {
            "NINJA" -> 0xFF212121.toInt()
            "ROBOT" -> 0xFF78909C.toInt()
            "GIRL" -> 0xFFEC407A.toInt()
            "PIRATE" -> 0xFF5D4037.toInt()
            "COWBOY" -> 0xFF8D6E63.toInt()
            else -> when (skinId) {
                "BLUE_HOODIE" -> 0xFF1E88E5.toInt()
                "GREEN_HOODIE" -> 0xFF43A047.toInt()
                "GOLDEN_OUTFIT" -> 0xFFFFD54F.toInt()
                "CYBER_OUTFIT" -> 0xFF00E676.toInt()
                else -> 0xFFE53935.toInt()
            }
        }

        val outlinePaint = Paint().apply {
            color = 0xFF111111.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }

        paint.color = mainColor
        paint.style = Paint.Style.FILL
        val bodyRect = RectF(screenX, screenY + 25f, screenX + width, screenY + height)
        canvas.drawRoundRect(bodyRect, 16f, 16f, paint)
        canvas.drawRoundRect(bodyRect, 16f, 16f, outlinePaint)

        val headRadius = 22f
        val headCenterX = screenX + width / 2f
        val headCenterY = screenY + 20f

        paint.color = 0xFFFFCC80.toInt()
        canvas.drawCircle(headCenterX, headCenterY, headRadius, paint)
        canvas.drawCircle(headCenterX, headCenterY, headRadius, outlinePaint)

        paint.color = mainColor
        canvas.drawArc(RectF(headCenterX - headRadius - 2f, headCenterY - headRadius - 4f, headCenterX + headRadius + 2f, headCenterY + 4f), 180f, 180f, true, paint)

        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        val eyeOffset = if (isFacingRight) 6f else -6f
        canvas.drawCircle(headCenterX + eyeOffset - 4f, headCenterY - 2f, 3.5f, paint)
        canvas.drawCircle(headCenterX + eyeOffset + 6f, headCenterY - 2f, 3.5f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(headCenterX + eyeOffset - 5f, headCenterY - 3f, 1.2f, paint)
        canvas.drawCircle(headCenterX + eyeOffset + 5f, headCenterY - 3f, 1.2f, paint)

        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(screenX + 2f, screenY + height - 12f, screenX + 24f, screenX + height), 6f, 6f, paint)
        canvas.drawRoundRect(RectF(screenX + width - 24f, screenY + height - 12f, screenX + width - 2f, screenY + height), 6f, 6f, paint)
    }
}

enum class PlatformType {
    NORMAL,
    MOVING,
    BOUNCY,
    DISAPPEARING
}

class Platform(
    x: Float,
    y: Float,
    width: Float,
    height: Float = 40f,
    val type: PlatformType = PlatformType.NORMAL,
    val moveRangeX: Float = 0f,
    val moveRangeY: Float = 0f,
    val speed: Float = 100f
) : GameObject(x, y, width, height) {

    private val startX = x
    private val startY = y
    private var moveProgress = 0f

    var dx: Float = 0f
    var dy: Float = 0f

    override fun update(deltaTime: Float) {
        if (type == PlatformType.MOVING) {
            moveProgress += speed * deltaTime
            val oldX = x
            val oldY = y

            if (moveRangeX > 0) {
                x = startX + sin(moveProgress * 0.02f) * moveRangeX
                dx = x - oldX
            }
            if (moveRangeY > 0) {
                y = startY + sin(moveProgress * 0.02f) * moveRangeY
                dy = y - oldY
            }
        }
    }

    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        val outlinePaint = Paint().apply {
            color = 0xFF1B5E20.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        val rect = RectF(screenX, screenY, screenX + width, screenY + height)

        when (type) {
            PlatformType.BOUNCY -> {
                paint.color = 0xFFFF9800.toInt()
                canvas.drawRoundRect(rect, 12f, 12f, paint)
                paint.color = 0xFFFFE082.toInt()
                canvas.drawRoundRect(RectF(screenX + 4f, screenY, screenX + width - 4f, screenY + 10f), 6f, 6f, paint)
            }
            else -> {
                paint.color = 0xFF5D4037.toInt()
                canvas.drawRoundRect(rect, 12f, 12f, paint)

                paint.color = 0xFF4CAF50.toInt()
                val topRect = RectF(screenX, screenY, screenX + width, screenY + 14f)
                canvas.drawRoundRect(topRect, 10f, 10f, paint)
            }
        }

        outlinePaint.color = 0xFF263238.toInt()
        canvas.drawRoundRect(rect, 12f, 12f, outlinePaint)
    }
}

class Coin(x: Float, y: Float) : GameObject(x, y, 32f, 32f) {
    var collected: Boolean = false
    private var animTime: Float = (x + y) * 0.01f

    override fun update(deltaTime: Float) {
        animTime += deltaTime * 5f
    }

    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        if (collected) return
        val screenX = x - cameraX
        val screenY = y - cameraY + sin(animTime) * 4f

        val cx = screenX + width / 2f
        val cy = screenY + height / 2f

        paint.color = 0xFFFFD54F.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, width / 2f, paint)

        paint.color = 0xFFFFB300.toInt()
        canvas.drawCircle(cx, cy, width / 2f - 4f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(cx - 4f, cy - 4f, 3f, paint)
    }
}

class Star(x: Float, y: Float) : GameObject(x, y, 42f, 42f) {
    var collected: Boolean = false
    private var animTime: Float = 0f

    override fun update(deltaTime: Float) {
        animTime += deltaTime * 4f
    }

    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        if (collected) return
        val screenX = x - cameraX
        val screenY = y - cameraY + sin(animTime) * 5f

        val cx = screenX + width / 2f
        val cy = screenY + height / 2f

        paint.color = 0xFFFFD700.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, width / 2f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(cx - 5f, cy - 5f, 4f, paint)
    }
}

class Spike(x: Float, y: Float, width: Float = 40f) : GameObject(x, y, width, 30f) {
    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        paint.color = 0xFFCFD8DC.toInt()
        paint.style = Paint.Style.FILL

        val count = (width / 20f).toInt().coerceAtLeast(1)
        val spikeWidth = width / count

        val path = android.graphics.Path()
        for (i in 0 until count) {
            val sx = screenX + i * spikeWidth
            path.reset()
            path.moveTo(sx, screenY + height)
            path.lineTo(sx + spikeWidth / 2f, screenY)
            path.lineTo(sx + spikeWidth, screenY + height)
            path.close()
            canvas.drawPath(path, paint)
        }
    }
}

class Enemy(
    x: Float,
    y: Float,
    val minX: Float,
    val maxX: Float,
    val speed: Float = 120f
) : GameObject(x, y, 50f, 50f) {

    private var direction = 1f

    override fun update(deltaTime: Float) {
        x += speed * direction * deltaTime
        if (x < minX) {
            x = minX
            direction = 1f
        } else if (x > maxX) {
            x = maxX
            direction = -1f
        }
    }

    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        if (!active) return
        val screenX = x - cameraX
        val screenY = y - cameraY

        val cx = screenX + width / 2f
        val cy = screenY + height / 2f

        paint.color = 0xFFD32F2F.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, width / 2f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(cx - 8f, cy - 4f, 6f, paint)
        canvas.drawCircle(cx + 8f, cy - 4f, 6f, paint)

        paint.color = Color.BLACK
        canvas.drawCircle(cx - 8f + direction * 2f, cy - 4f, 3f, paint)
        canvas.drawCircle(cx + 8f + direction * 2f, cy - 4f, 3f, paint)
    }
}

class Checkpoint(x: Float, y: Float) : GameObject(x, y, 40f, 80f) {
    var activated: Boolean = false

    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        paint.color = 0xFF78909C.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(RectF(screenX + 5f, screenY, screenX + 12f, screenY + height), paint)

        paint.color = if (activated) 0xFF4CAF50.toInt() else 0xFFE53935.toInt()
        val flagPath = android.graphics.Path().apply {
            moveTo(screenX + 12f, screenY + 5f)
            lineTo(screenX + width, screenY + 22f)
            lineTo(screenX + 12f, screenY + 40f)
            close()
        }
        canvas.drawPath(flagPath, paint)
    }
}

class FinishFlag(x: Float, y: Float) : GameObject(x, y, 60f, 100f) {
    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        val screenX = x - cameraX
        val screenY = y - cameraY

        paint.color = 0xFFFFD54F.toInt()
        paint.style = Paint.Style.FILL
        canvas.drawRect(RectF(screenX + 8f, screenY, screenX + 18f, screenY + height), paint)

        paint.color = 0xFFFF9800.toInt()
        canvas.drawCircle(screenX + 13f, screenY - 5f, 10f, paint)

        paint.color = 0xFF212121.toInt()
        canvas.drawRect(RectF(screenX + 18f, screenY + 5f, screenX + width, screenY + 45f), paint)

        paint.color = Color.WHITE
        canvas.drawRect(RectF(screenX + 18f, screenY + 5f, screenX + 38f, screenY + 25f), paint)
        canvas.drawRect(RectF(screenX + 38f, screenY + 25f, screenX + width, screenY + 45f), paint)
    }
}

class PowerUpItem(
    x: Float,
    y: Float,
    val powerUpType: PowerUpType
) : GameObject(x, y, 36f, 36f) {
    var collected: Boolean = false
    private var animTime: Float = x * 0.05f

    override fun update(deltaTime: Float) {
        animTime += deltaTime * 4f
    }

    override fun render(canvas: Canvas, paint: Paint, cameraX: Float, cameraY: Float) {
        if (collected) return
        val screenX = x - cameraX
        val screenY = y - cameraY + sin(animTime) * 4f

        val cx = screenX + width / 2f
        val cy = screenY + height / 2f

        paint.color = when (powerUpType) {
            PowerUpType.MAGNET -> 0xFFE53935.toInt()
            PowerUpType.SHIELD -> 0xFF29B6F6.toInt()
            PowerUpType.SPEED -> 0xFFFFB300.toInt()
            PowerUpType.EXTRA_LIFE -> 0xFFE91E63.toInt()
            PowerUpType.STAR_BOOST -> 0xFFAB47BC.toInt()
        }
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, width / 2f, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, width / 4f, paint)
    }
}
