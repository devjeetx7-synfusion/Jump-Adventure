package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CharacterCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var characterId: String = "DEFAULT"
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val bounds = RectF(w * 0.1f, h * 0.05f, w * 0.9f, h * 0.92f)

        CharacterRenderer.drawCharacter(
            canvas = canvas,
            bounds = bounds,
            characterId = characterId,
            facingRight = true,
            animState = CharacterRenderer.AnimState.IDLE,
            animTime = 0f
        )
    }
}
