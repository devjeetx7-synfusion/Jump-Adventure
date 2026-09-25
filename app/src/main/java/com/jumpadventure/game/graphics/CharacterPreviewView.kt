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

    var onCharacterTappedListener: (() -> Unit)? = null

    private var animTime = 0f
    private val frameRate = 60f

    private var jumpState = JumpState.IDLE_WALK
    private var jumpStateTime = 0f
    private var jumpYOffset = 0f

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#40000000") }
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private val charBounds = RectF()
    private val shadowOval = RectF()

    private val animRunnable = object : Runnable {
        override fun run() {
            val dt = 1f / frameRate
            animTime += dt
            updateJumpState(dt)
            invalidate()
            postDelayed(this, (1000 / frameRate).toLong())
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

        // 2. Ambient Sparkles around Hero
        for (i in 0..2) {
            val sparkX = cx + (if (i == 0) -w * 0.32f else if (i == 1) w * 0.32f else w * 0.22f)
            val sparkY = h * 0.25f + (i * 36f) + (sin((animTime * 3.5f + i).toDouble()) * 8f).toFloat()
            val size = (5f + sin((animTime * 5f + i).toDouble()) * 2f).toFloat()
            canvas.drawCircle(sparkX, sparkY, size, sparklePaint)
        }

        // 3. Draw Character Hero Full-Body
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
            JumpState.IDLE_WALK -> CharacterRenderer.AnimState.IDLE
            JumpState.JUMP_ANTICIPATION -> CharacterRenderer.AnimState.IDLE
            JumpState.JUMP_RISING -> CharacterRenderer.AnimState.JUMP
            JumpState.JUMP_FALLING -> CharacterRenderer.AnimState.FALL
            JumpState.JUMP_LANDING -> CharacterRenderer.AnimState.IDLE
        }

        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = charBounds,
            characterId = selectedCharacterId,
            facingRight = true,
            animState = renderAnimState,
            animTime = animTime
        )
    }
}
