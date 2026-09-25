package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class CharacterPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class JumpState { IDLE_WALK, JUMP_ANTICIPATION, JUMP_RISING, JUMP_FALLING, JUMP_LANDING }

    var selectedCharacterId: String = "DEFAULT"
        set(value) {
            field = value
            invalidate()
        }

    var selectedSkinId: String = "DEFAULT"
        set(value) {
            field = value
            invalidate()
        }

    var selectedTrailId: String = "NONE"
        set(value) {
            field = value
            invalidate()
        }

    var playerSpeedMultiplier: Float = 1.15f
        set(value) {
            field = value.coerceIn(0.75f, 1.50f)
            invalidate()
        }

    var onCharacterTappedListener: (() -> Unit)? = null

    private var animTime = 0f
    private val frameRate = 60f

    private var jumpState = JumpState.IDLE_WALK
    private var jumpStateTime = 0f
    private var jumpYOffset = 0f

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#40000000") }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private class PreviewParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var alpha: Int,
        val color: Int,
        val size: Float
    )
    private val trailParticles = mutableListOf<PreviewParticle>()

    private val charBounds = RectF()
    private val shadowOval = RectF()

    private val animRunnable = object : Runnable {
        override fun run() {
            val speedFactor = playerSpeedMultiplier.coerceIn(0.75f, 1.50f)
            val dt = (1f / frameRate) * speedFactor
            animTime += dt
            updateJumpState(1f / frameRate)
            updateParticles()
            invalidate()
            postDelayed(this, (1000 / frameRate).toLong())
        }
    }

    private fun updateParticles() {
        if (selectedTrailId != "NONE") {
            val trailColor = when (selectedTrailId) {
                "FIRE" -> listOf(Color.YELLOW, Color.RED, Color.parseColor("#FF9F1C")).random()
                "ICE" -> listOf(Color.CYAN, Color.WHITE, Color.parseColor("#00E5FF")).random()
                "LIGHTNING" -> listOf(Color.YELLOW, Color.CYAN, Color.WHITE).random()
                "RAINBOW" -> Color.HSVToColor(floatArrayOf((animTime * 180f) % 360f, 1f, 1f))
                "SHADOW" -> listOf(Color.parseColor("#2D3748"), Color.BLACK, Color.parseColor("#4A5568")).random()
                "GOLD" -> listOf(Color.parseColor("#FFD700"), Color.parseColor("#FFD43B"), Color.WHITE).random()
                "NEON" -> listOf(Color.parseColor("#00E676"), Color.parseColor("#E040FB"), Color.CYAN).random()
                "GALAXY" -> listOf(Color.parseColor("#9C27B0"), Color.parseColor("#3F51B5"), Color.WHITE).random()
                "LEAVES" -> listOf(Color.parseColor("#4CAF50"), Color.parseColor("#81C784")).random()
                "SNOW" -> Color.WHITE
                "LAVA" -> listOf(Color.parseColor("#FF3D00"), Color.parseColor("#D84315")).random()
                else -> Color.YELLOW
            }
            if (trailParticles.size < 12 && Math.random() < 0.45) {
                trailParticles.add(
                    PreviewParticle(
                        x = (width / 2f) + (Math.random().toFloat() - 0.5f) * 30f,
                        y = (height - 25f) + (Math.random().toFloat() - 0.5f) * 10f,
                        vx = (Math.random().toFloat() - 0.5f) * 0.8f,
                        vy = -(0.5f + Math.random().toFloat() * 0.8f),
                        alpha = 180,
                        color = trailColor,
                        size = 5f + Math.random().toFloat() * 6f
                    )
                )
            }
        }
        val pIter = trailParticles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx
            p.y += p.vy
            p.alpha -= 8
            if (p.alpha <= 0) pIter.remove()
        }
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animRunnable)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            triggerJump()
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    fun triggerJump() {
        if (jumpState == JumpState.IDLE_WALK) {
            jumpState = JumpState.JUMP_ANTICIPATION
            jumpStateTime = 0f
            onCharacterTappedListener?.invoke()
        }
    }

    private fun updateJumpState(dt: Float) {
        if (jumpState == JumpState.IDLE_WALK) {
            jumpYOffset = 0f
            return
        }

        jumpStateTime += dt

        when (jumpState) {
            JumpState.JUMP_ANTICIPATION -> {
                jumpYOffset = 6f
                if (jumpStateTime >= 0.08f) {
                    jumpState = JumpState.JUMP_RISING
                    jumpStateTime = 0f
                }
            }
            JumpState.JUMP_RISING -> {
                val progress = (jumpStateTime / 0.28f).coerceIn(0f, 1f)
                // Parabolic upward arc
                jumpYOffset = -sin(progress * Math.PI / 2.0).toFloat() * 72f
                if (jumpStateTime >= 0.28f) {
                    jumpState = JumpState.JUMP_FALLING
                    jumpStateTime = 0f
                }
            }
            JumpState.JUMP_FALLING -> {
                val progress = (jumpStateTime / 0.22f).coerceIn(0f, 1f)
                // Parabolic downward arc back to ground
                jumpYOffset = -cos(progress * Math.PI / 2.0).toFloat() * 72f
                if (jumpStateTime >= 0.22f) {
                    jumpState = JumpState.JUMP_LANDING
                    jumpStateTime = 0f
                }
            }
            JumpState.JUMP_LANDING -> {
                jumpYOffset = 4f
                if (jumpStateTime >= 0.10f) {
                    jumpState = JumpState.IDLE_WALK
                    jumpStateTime = 0f
                    jumpYOffset = 0f
                }
            }
            else -> {}
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val desiredWidth = (280 * density).toInt()
        val desiredHeight = (220 * density).toInt()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val w = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val h = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val feetY = h - 12f

        // 1. Foot Shadow on Mountain surface (shrinks slightly as character jumps up)
        val shadowScale = (1f - (-jumpYOffset / 120f)).coerceIn(0.4f, 1f)
        val shadowW = w * 0.38f * shadowScale
        shadowOval.set(cx - shadowW / 2f, feetY - 6f, cx + shadowW / 2f, feetY + 8f)
        canvas.drawOval(shadowOval, shadowPaint)

        // 2. Trail Effect Particles
        trailParticles.forEach { p ->
            particlePaint.color = p.color
            particlePaint.alpha = p.alpha.coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y + jumpYOffset, p.size, particlePaint)
        }

        // 3. Draw Character Hero Full-Body with natural walking cycle
        val charW = w * 0.55f
        val charH = h * 0.88f
        val currentFeetY = feetY + jumpYOffset

        charBounds.set(
            cx - charW / 2f,
            currentFeetY - charH,
            cx + charW / 2f,
            currentFeetY
        )

        val renderAnimState = when (jumpState) {
            JumpState.IDLE_WALK -> CharacterRenderer.AnimState.RUN
            JumpState.JUMP_ANTICIPATION -> CharacterRenderer.AnimState.IDLE
            JumpState.JUMP_RISING -> CharacterRenderer.AnimState.JUMP
            JumpState.JUMP_FALLING -> CharacterRenderer.AnimState.FALL
            JumpState.JUMP_LANDING -> CharacterRenderer.AnimState.IDLE
        }

        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = charBounds,
            characterId = selectedCharacterId,
            skinId = selectedSkinId,
            facingRight = true,
            animState = renderAnimState,
            animTime = animTime
        )
    }
}
