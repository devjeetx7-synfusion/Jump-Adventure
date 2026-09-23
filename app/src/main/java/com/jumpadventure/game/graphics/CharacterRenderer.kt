package com.jumpadventure.game.graphics

import android.graphics.*
import kotlin.math.abs
import kotlin.math.sin

object CharacterRenderer {

    enum class AnimState { IDLE, RUN, JUMP, FALL }

    /**
     * Renders a full-body illustrated character on [canvas] within [bounds].
     *
     * @param characterId "DEFAULT", "NINJA", "ROBOT", "GIRL", "PIRATE", "COWBOY"
     * @param facingRight Whether the character faces right (true) or left (false)
     * @param animState Current animation state
     * @param animTime Time in seconds (or tick count / 60) for continuous animation cycles
     */
    fun drawCharacter(
        canvas: Canvas,
        bounds: RectF,
        characterId: String,
        facingRight: Boolean = true,
        animState: AnimState = AnimState.IDLE,
        animTime: Float = 0f
    ) {
        val width = bounds.width()
        val height = bounds.height()
        val cx = bounds.centerX()
        val bottom = bounds.bottom

        canvas.save()

        // Flip canvas if facing left
        if (!facingRight) {
            canvas.scale(-1f, 1f, cx, bounds.centerY())
        }

        // Animation offsets
        var bounceY = 0f
        var legAngle1 = 0f
        var legAngle2 = 0f
        var armAngle1 = 0f
        var armAngle2 = 0f

        when (animState) {
            AnimState.IDLE -> {
                bounceY = (sin(animTime * 4f.toDouble()) * (height * 0.03f)).toFloat()
            }
            AnimState.RUN -> {
                bounceY = abs(sin(animTime * 12f.toDouble()) * (height * 0.05f)).toFloat()
                legAngle1 = (sin(animTime * 12f.toDouble()) * 30f).toFloat()
                legAngle2 = (-sin(animTime * 12f.toDouble()) * 30f).toFloat()
                armAngle1 = (-sin(animTime * 12f.toDouble()) * 35f).toFloat()
                armAngle2 = (sin(animTime * 12f.toDouble()) * 35f).toFloat()
            }
            AnimState.JUMP -> {
                bounceY = -height * 0.05f
                legAngle1 = -20f
                legAngle2 = 25f
                armAngle1 = -45f
                armAngle2 = 30f
            }
            AnimState.FALL -> {
                legAngle1 = 15f
                legAngle2 = -15f
                armAngle1 = -60f
                armAngle2 = -40f
            }
        }

        val topY = bounds.top + bounceY
        val bodyY = topY + height * 0.38f
        val headY = topY + height * 0.22f
        val headRadius = height * 0.18f

        // Paints
        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE0B2") }
        val darkOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B1B2F")
            style = Paint.Style.STROKE
            strokeWidth = (width * 0.04f).coerceAtLeast(3f)
            strokeCap = Paint.Cap.ROUND
        }
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B1B2F") }
        val eyeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val shoePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val shoeSolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#37474F") }

        // Color palettes per character
        val (outfitColor, hairHatColor, accentColor) = when (characterId) {
            "NINJA" -> Triple("#263238", "#111111", "#E53935") // Dark suit, red headband
            "ROBOT" -> Triple("#78909C", "#455A64", "#00E676") // Metallic grey, cyan/green visor
            "GIRL" -> Triple("#EC407A", "#F48FB1", "#FFD54F") // Pink outfit, bright hair
            "PIRATE" -> Triple("#D84315", "#3E2723", "#FFD700") // Red coat, dark hat, gold trim
            "COWBOY" -> Triple("#8D6E63", "#5D4037", "#FFB300") // Brown vest, cowboy hat
            else -> Triple("#E53935", "#37474F", "#FFFFFF") // Default Red Hoodie
        }

        val outfitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(outfitColor) }
        val hairHatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hairHatColor) }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(accentColor) }

        // 1. LEGS & SHOES (drawn behind torso)
        val hipX = cx
        val hipY = topY + height * 0.65f
        val legLength = height * 0.28f
        val shoeWidth = width * 0.22f
        val shoeHeight = height * 0.08f

        // Back Leg
        canvas.save()
        canvas.rotate(legAngle2, hipX, hipY)
        val leg2Bottom = hipY + legLength
        val leg2Paint = Paint(outfitPaint).apply { color = darkenColor(outfitPaint.color) }
        canvas.drawRoundRect(RectF(hipX - width * 0.08f, hipY, hipX + width * 0.08f, leg2Bottom), 8f, 8f, leg2Paint)
        canvas.drawRoundRect(RectF(hipX - width * 0.08f, hipY, hipX + width * 0.08f, leg2Bottom), 8f, 8f, darkOutlinePaint)
        // Back Shoe
        val backShoeRect = RectF(hipX - shoeWidth * 0.3f, leg2Bottom - 4f, hipX + shoeWidth * 0.7f, leg2Bottom + shoeHeight)
        canvas.drawRoundRect(backShoeRect, 8f, 8f, shoePaint)
        canvas.drawRoundRect(backShoeRect, 8f, 8f, darkOutlinePaint)
        canvas.restore()

        // Front Leg
        canvas.save()
        canvas.rotate(legAngle1, hipX, hipY)
        val leg1Bottom = hipY + legLength
        canvas.drawRoundRect(RectF(hipX - width * 0.08f, hipY, hipX + width * 0.08f, leg1Bottom), 8f, 8f, outfitPaint)
        canvas.drawRoundRect(RectF(hipX - width * 0.08f, hipY, hipX + width * 0.08f, leg1Bottom), 8f, 8f, darkOutlinePaint)
        // Front Shoe
        val frontShoeRect = RectF(hipX - shoeWidth * 0.3f, leg1Bottom - 4f, hipX + shoeWidth * 0.7f, leg1Bottom + shoeHeight)
        canvas.drawRoundRect(frontShoeRect, 8f, 8f, shoePaint)
        canvas.drawRoundRect(frontShoeRect, 8f, 8f, darkOutlinePaint)
        // Shoe accent lines
        canvas.drawRect(RectF(frontShoeRect.left, frontShoeRect.bottom - 4f, frontShoeRect.right, frontShoeRect.bottom), shoeSolePaint)
        canvas.restore()

        // 2. BACK ARM
        val shoulderX = cx
        val shoulderY = topY + height * 0.40f
        val armLength = height * 0.22f
        canvas.save()
        canvas.rotate(armAngle2, shoulderX, shoulderY)
        val arm2Rect = RectF(shoulderX - width * 0.07f, shoulderY, shoulderX + width * 0.07f, shoulderY + armLength)
        canvas.drawRoundRect(arm2Rect, 6f, 6f, leg2Paint)
        canvas.drawRoundRect(arm2Rect, 6f, 6f, darkOutlinePaint)
        // Hand
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, skinPaint)
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, darkOutlinePaint)
        canvas.restore()

        // 3. TORSO / CLOTHING
        val torsoRect = RectF(cx - width * 0.22f, topY + height * 0.35f, cx + width * 0.22f, topY + height * 0.68f)
        canvas.drawRoundRect(torsoRect, 16f, 16f, outfitPaint)
        canvas.drawRoundRect(torsoRect, 16f, 16f, darkOutlinePaint)

        // Clothing Details / Decals
        when (characterId) {
            "DEFAULT" -> { // Hoodie zipper & pocket
                val zipPaint = Paint(darkOutlinePaint).apply { strokeWidth = 3f }
                canvas.drawLine(cx, torsoRect.top + 10f, cx, torsoRect.bottom - 10f, zipPaint)
                canvas.drawRoundRect(RectF(cx - width * 0.12f, torsoRect.bottom - height * 0.12f, cx + width * 0.12f, torsoRect.bottom - 6f), 6f, 6f, accentPaint)
            }
            "NINJA" -> { // Belt & Red sash
                canvas.drawRect(RectF(torsoRect.left, torsoRect.centerY() - 6f, torsoRect.right, torsoRect.centerY() + 6f), accentPaint)
            }
            "ROBOT" -> { // Chest LED
                canvas.drawCircle(cx, torsoRect.centerY(), width * 0.08f, accentPaint)
                canvas.drawCircle(cx, torsoRect.centerY(), width * 0.08f, darkOutlinePaint)
            }
            "GIRL" -> { // Heart decal
                val heartPaint = Paint(accentPaint)
                canvas.drawCircle(cx - 6f, torsoRect.centerY() - 4f, 8f, heartPaint)
                canvas.drawCircle(cx + 6f, torsoRect.centerY() - 4f, 8f, heartPaint)
            }
            "PIRATE" -> { // Gold buttons & vest
                canvas.drawCircle(cx - 10f, torsoRect.top + 20f, 5f, accentPaint)
                canvas.drawCircle(cx + 10f, torsoRect.top + 20f, 5f, accentPaint)
                canvas.drawCircle(cx - 10f, torsoRect.top + 40f, 5f, accentPaint)
                canvas.drawCircle(cx + 10f, torsoRect.top + 40f, 5f, accentPaint)
            }
            "COWBOY" -> { // Star badge
                canvas.drawCircle(cx - 12f, torsoRect.top + 18f, 7f, accentPaint)
            }
        }

        // 4. FRONT ARM
        canvas.save()
        canvas.rotate(armAngle1, shoulderX, shoulderY)
        val arm1Rect = RectF(shoulderX - width * 0.07f, shoulderY, shoulderX + width * 0.07f, shoulderY + armLength)
        canvas.drawRoundRect(arm1Rect, 6f, 6f, outfitPaint)
        canvas.drawRoundRect(arm1Rect, 6f, 6f, darkOutlinePaint)
        // Hand
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, skinPaint)
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, darkOutlinePaint)
        canvas.restore()

        // 5. HEAD & FACE
        val headCX = cx
        val headCY = headY

        // Robot has metallic square-ish head
        if (characterId == "ROBOT") {
            val headRect = RectF(headCX - headRadius, headCY - headRadius, headCX + headRadius, headCY + headRadius)
            canvas.drawRoundRect(headRect, 14f, 14f, hairHatPaint)
            canvas.drawRoundRect(headRect, 14f, 14f, darkOutlinePaint)
            // Visor
            val visorRect = RectF(headCX - headRadius * 0.7f, headCY - headRadius * 0.3f, headCX + headRadius * 0.7f, headCY + headRadius * 0.3f)
            canvas.drawRoundRect(visorRect, 8f, 8f, accentPaint)
            canvas.drawRoundRect(visorRect, 8f, 8f, darkOutlinePaint)
        } else {
            // Standard head
            canvas.drawCircle(headCX, headCY, headRadius, skinPaint)
            canvas.drawCircle(headCX, headCY, headRadius, darkOutlinePaint)

            // Eyes (Blinking on idle cycle)
            val isBlinking = animState == AnimState.IDLE && (animTime % 3.5f > 3.3f)
            val eyeX1 = headCX + headRadius * 0.2f
            val eyeX2 = headCX + headRadius * 0.65f
            val eyeY = headCY - headRadius * 0.05f

            if (isBlinking) {
                canvas.drawLine(eyeX1 - 5f, eyeY, eyeX1 + 5f, eyeY, darkOutlinePaint)
                canvas.drawLine(eyeX2 - 5f, eyeY, eyeX2 + 5f, eyeY, darkOutlinePaint)
            } else {
                canvas.drawCircle(eyeX1, eyeY, width * 0.05f, eyePaint)
                canvas.drawCircle(eyeX2, eyeY, width * 0.05f, eyePaint)
                // Eye shine highlights
                canvas.drawCircle(eyeX1 + 2f, eyeY - 2f, width * 0.018f, eyeHighlightPaint)
                canvas.drawCircle(eyeX2 + 2f, eyeY - 2f, width * 0.018f, eyeHighlightPaint)
            }

            // Cheeks
            val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#40FF8A80") }
            canvas.drawCircle(eyeX1, eyeY + 12f, 6f, cheekPaint)
            canvas.drawCircle(eyeX2, eyeY + 12f, 6f, cheekPaint)

            // Hair / Headgear
            when (characterId) {
                "DEFAULT" -> { // Red Hoodie Hood
                    val hoodPath = Path().apply {
                        addCircle(headCX, headCY, headRadius + 4f, Path.Direction.CW)
                    }
                    canvas.drawPath(hoodPath, outfitPaint)
                    canvas.drawPath(hoodPath, darkOutlinePaint)
                    // Inner face cutout
                    canvas.drawCircle(headCX + 6f, headCY + 2f, headRadius - 3f, skinPaint)
                    // Re-draw face elements inside hood
                    if (!isBlinking) {
                        canvas.drawCircle(eyeX1, eyeY, width * 0.05f, eyePaint)
                        canvas.drawCircle(eyeX2, eyeY, width * 0.05f, eyePaint)
                        canvas.drawCircle(eyeX1 + 2f, eyeY - 2f, width * 0.018f, eyeHighlightPaint)
                        canvas.drawCircle(eyeX2 + 2f, eyeY - 2f, width * 0.018f, eyeHighlightPaint)
                    }
                }
                "NINJA" -> { // Ninja Mask & Headband
                    val maskRect = RectF(headCX - headRadius, headCY, headCX + headRadius, headCY + headRadius)
                    canvas.drawRoundRect(maskRect, 10f, 10f, outfitPaint)
                    canvas.drawRoundRect(maskRect, 10f, 10f, darkOutlinePaint)
                    // Red Headband
                    val bandRect = RectF(headCX - headRadius - 2f, headCY - headRadius * 0.6f, headCX + headRadius + 2f, headCY - headRadius * 0.1f)
                    canvas.drawRect(bandRect, accentPaint)
                    canvas.drawRect(bandRect, darkOutlinePaint)
                }
                "GIRL" -> { // Pink Hair & Bow
                    canvas.drawCircle(headCX - headRadius * 0.4f, headCY - headRadius * 0.5f, headRadius * 0.8f, hairHatPaint)
                    canvas.drawCircle(headCX - headRadius * 0.4f, headCY - headRadius * 0.5f, headRadius * 0.8f, darkOutlinePaint)
                    // Bow
                    canvas.drawCircle(headCX - headRadius * 0.5f, headCY - headRadius * 0.8f, 10f, accentPaint)
                }
                "PIRATE" -> { // Pirate Hat
                    val hatPath = Path().apply {
                        moveTo(headCX - headRadius * 1.4f, headCY - headRadius * 0.3f)
                        lineTo(headCX + headRadius * 1.4f, headCY - headRadius * 0.3f)
                        lineTo(headCX, headCY - headRadius * 1.5f)
                        close()
                    }
                    canvas.drawPath(hatPath, hairHatPaint)
                    canvas.drawPath(hatPath, darkOutlinePaint)
                    // Gold Trim
                    canvas.drawLine(headCX - headRadius * 1.4f, headCY - headRadius * 0.3f, headCX + headRadius * 1.4f, headCY - headRadius * 0.3f, accentPaint)
                }
                "COWBOY" -> { // Cowboy Hat
                    val brimRect = RectF(headCX - headRadius * 1.5f, headCY - headRadius * 0.5f, headCX + headRadius * 1.5f, headCY - headRadius * 0.2f)
                    canvas.drawRoundRect(brimRect, 10f, 10f, hairHatPaint)
                    canvas.drawRoundRect(brimRect, 10f, 10f, darkOutlinePaint)
                    val crownRect = RectF(headCX - headRadius * 0.8f, headCY - headRadius * 1.3f, headCX + headRadius * 0.8f, headCY - headRadius * 0.4f)
                    canvas.drawRoundRect(crownRect, 12f, 12f, hairHatPaint)
                    canvas.drawRoundRect(crownRect, 12f, 12f, darkOutlinePaint)
                }
            }
        }

        canvas.restore()
    }

    private fun darkenColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.75f
        return Color.HSVToColor(hsv)
    }
}
