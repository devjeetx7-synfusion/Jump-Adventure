package com.jumpadventure.game.graphics

import android.graphics.*
import kotlin.math.abs
import kotlin.math.sin

object CharacterRenderer {

    enum class AnimState { IDLE, RUN, JUMP, FALL }

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

        canvas.save()

        if (!facingRight) {
            canvas.scale(-1f, 1f, cx, bounds.centerY())
        }

        var bounceY = 0f
        var scaleY = 1f
        var scaleX = 1f
        var legAngle1 = 0f
        var legAngle2 = 0f
        var armAngle1 = 0f
        var armAngle2 = 0f

        when (animState) {
            AnimState.IDLE -> {
                // Natural standing / walking loop without vertical bouncing on the platform
                bounceY = 0f
                legAngle1 = (sin(animTime * 3.5f.toDouble()) * 12f).toFloat()
                legAngle2 = (-sin(animTime * 3.5f.toDouble()) * 12f).toFloat()
                armAngle1 = (-sin(animTime * 3.5f.toDouble()) * 14f).toFloat()
                armAngle2 = (sin(animTime * 3.5f.toDouble()) * 14f).toFloat()
            }
            AnimState.RUN -> {
                bounceY = abs(sin(animTime * 12f.toDouble()) * (height * 0.04f)).toFloat()
                legAngle1 = (sin(animTime * 12f.toDouble()) * 30f).toFloat()
                legAngle2 = (-sin(animTime * 12f.toDouble()) * 30f).toFloat()
                armAngle1 = (-sin(animTime * 12f.toDouble()) * 35f).toFloat()
                armAngle2 = (sin(animTime * 12f.toDouble()) * 35f).toFloat()
            }
            AnimState.JUMP -> {
                bounceY = 0f
                scaleY = 1.15f
                scaleX = 0.9f
                legAngle1 = -22f
                legAngle2 = 25f
                armAngle1 = -50f
                armAngle2 = 35f
            }
            AnimState.FALL -> {
                bounceY = 0f
                scaleY = 0.95f
                scaleX = 1.05f
                legAngle1 = 15f
                legAngle2 = -15f
                armAngle1 = -60f
                armAngle2 = -40f
            }
        }

        // Apply global squash/stretch
        canvas.translate(cx, bounds.bottom)
        canvas.scale(scaleX, scaleY)
        canvas.translate(-cx, -bounds.bottom)

        val topY = bounds.top + bounceY
        val headY = topY + height * 0.22f
        val headRadius = height * 0.18f

        val skinColor = Color.parseColor("#FFDAB9")
        val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = skinColor }

        val darkOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B1B2F")
            style = Paint.Style.STROKE
            strokeWidth = (width * 0.04f).coerceAtLeast(3f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B1B2F") }
        val eyeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        val (outfitColor, hairHatColor, accentColor, shoeColorStr) = when (characterId) {
            "NINJA" -> listOf("#263238", "#111111", "#E53935", "#111111")
            "ROBOT" -> listOf("#78909C", "#455A64", "#00E676", "#37474F")
            "GIRL" -> listOf("#EC407A", "#F48FB1", "#FFD54F", "#FFFFFF")
            "PIRATE" -> listOf("#D84315", "#3E2723", "#FFD700", "#3E2723")
            "COWBOY" -> listOf("#8D6E63", "#5D4037", "#FFB300", "#5D4037")
            "ICE" -> listOf("#00ACC1", "#B2EBF2", "#00E5FF", "#FFFFFF")
            "DESERT" -> listOf("#FB8C00", "#FFE082", "#FF6D00", "#5D4037")
            "LAVA" -> listOf("#D84315", "#212121", "#FF3D00", "#212121")
            "NEON" -> listOf("#00E676", "#212121", "#00E5FF", "#00E676")
            "FOREST" -> listOf("#4CAF50", "#1B5E20", "#81C784", "#33691E")
            "GALAXY" -> listOf("#7B1FA2", "#1A237E", "#E040FB", "#4A148C")
            else -> listOf("#E53935", "#37474F", "#FFFFFF", "#FFFFFF")
        }

        val outfitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(outfitColor) }
        val hairHatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(hairHatColor) }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(accentColor) }
        val shoePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(shoeColorStr) }
        val shoeSolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#37474F") }

        val hipX = cx
        val hipY = topY + height * 0.65f
        val legLength = height * 0.28f
        val shoeWidth = width * 0.22f
        val shoeHeight = height * 0.08f

        // Back Leg
        canvas.save()
        canvas.rotate(legAngle2, hipX, hipY)
        val leg2Bottom = hipY + legLength
        val leg2Paint = Paint(outfitPaint).apply { color = darkenColor(outfitPaint.color, 0.75f) }
        val leg2Rect = RectF(hipX - width * 0.08f, hipY, hipX + width * 0.08f, leg2Bottom)
        leg2Paint.shader = LinearGradient(leg2Rect.left, leg2Rect.top, leg2Rect.right, leg2Rect.bottom, leg2Paint.color, darkenColor(leg2Paint.color, 0.7f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(leg2Rect, 12f, 12f, leg2Paint)
        canvas.drawRoundRect(leg2Rect, 12f, 12f, darkOutlinePaint)

        // Back Shoe
        val backShoeRect = RectF(hipX - shoeWidth * 0.3f, leg2Bottom - 4f, hipX + shoeWidth * 0.7f, leg2Bottom + shoeHeight)
        canvas.drawRoundRect(backShoeRect, 10f, 10f, shoePaint)
        canvas.drawRoundRect(backShoeRect, 10f, 10f, darkOutlinePaint)
        canvas.restore()

        // Front Leg
        canvas.save()
        canvas.rotate(legAngle1, hipX, hipY)
        val leg1Bottom = hipY + legLength
        val leg1Rect = RectF(hipX - width * 0.08f, hipY, hipX + width * 0.08f, leg1Bottom)
        val leg1Paint = Paint(outfitPaint)
        leg1Paint.shader = LinearGradient(leg1Rect.left, leg1Rect.top, leg1Rect.right, leg1Rect.bottom, leg1Paint.color, darkenColor(leg1Paint.color, 0.8f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(leg1Rect, 12f, 12f, leg1Paint)
        canvas.drawRoundRect(leg1Rect, 12f, 12f, darkOutlinePaint)

        // Front Shoe
        val frontShoeRect = RectF(hipX - shoeWidth * 0.3f, leg1Bottom - 4f, hipX + shoeWidth * 0.7f, leg1Bottom + shoeHeight)
        canvas.drawRoundRect(frontShoeRect, 10f, 10f, shoePaint)
        canvas.drawRoundRect(frontShoeRect, 10f, 10f, darkOutlinePaint)
        canvas.drawRect(RectF(frontShoeRect.left, frontShoeRect.bottom - 4f, frontShoeRect.right, frontShoeRect.bottom), shoeSolePaint)
        canvas.restore()

        // BACK ARM
        val shoulderX = cx
        val shoulderY = topY + height * 0.40f
        val armLength = height * 0.22f
        canvas.save()
        canvas.rotate(armAngle2, shoulderX, shoulderY)
        val arm2Rect = RectF(shoulderX - width * 0.07f, shoulderY, shoulderX + width * 0.07f, shoulderY + armLength)
        canvas.drawRoundRect(arm2Rect, 10f, 10f, leg2Paint)
        canvas.drawRoundRect(arm2Rect, 10f, 10f, darkOutlinePaint)
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, skinPaint)
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, darkOutlinePaint)
        canvas.restore()

        // TORSO
        val torsoRect = RectF(cx - width * 0.22f, topY + height * 0.35f, cx + width * 0.22f, topY + height * 0.68f)
        val torsoGradient = LinearGradient(torsoRect.left, torsoRect.top, torsoRect.right, torsoRect.bottom, outfitPaint.color, darkenColor(outfitPaint.color, 0.7f), Shader.TileMode.CLAMP)
        outfitPaint.shader = torsoGradient
        canvas.drawRoundRect(torsoRect, 20f, 20f, outfitPaint)
        canvas.drawRoundRect(torsoRect, 20f, 20f, darkOutlinePaint)
        outfitPaint.shader = null

        when (characterId) {
            "DEFAULT" -> {
                val zipPaint = Paint(darkOutlinePaint).apply { strokeWidth = 3f }
                canvas.drawLine(cx, torsoRect.top + 10f, cx, torsoRect.bottom - 10f, zipPaint)
                canvas.drawRoundRect(RectF(cx - width * 0.12f, torsoRect.bottom - height * 0.12f, cx + width * 0.12f, torsoRect.bottom - 6f), 8f, 8f, accentPaint)
            }
            "NINJA" -> {
                canvas.drawRect(RectF(torsoRect.left, torsoRect.centerY() - 6f, torsoRect.right, torsoRect.centerY() + 6f), accentPaint)
            }
            "ROBOT" -> {
                canvas.drawCircle(cx, torsoRect.centerY(), width * 0.08f, accentPaint)
                canvas.drawCircle(cx, torsoRect.centerY(), width * 0.08f, darkOutlinePaint)
            }
            "GIRL" -> {
                val heartPaint = Paint(accentPaint)
                canvas.drawCircle(cx - 6f, torsoRect.centerY() - 4f, 8f, heartPaint)
                canvas.drawCircle(cx + 6f, torsoRect.centerY() - 4f, 8f, heartPaint)
            }
            "PIRATE" -> {
                canvas.drawCircle(cx - 10f, torsoRect.top + 20f, 5f, accentPaint)
                canvas.drawCircle(cx + 10f, torsoRect.top + 20f, 5f, accentPaint)
                canvas.drawCircle(cx - 10f, torsoRect.top + 40f, 5f, accentPaint)
                canvas.drawCircle(cx + 10f, torsoRect.top + 40f, 5f, accentPaint)
            }
            "COWBOY" -> {
                canvas.drawCircle(cx - 12f, torsoRect.top + 18f, 7f, accentPaint)
            }
        }

        // FRONT ARM
        canvas.save()
        canvas.rotate(armAngle1, shoulderX, shoulderY)
        val arm1Rect = RectF(shoulderX - width * 0.07f, shoulderY, shoulderX + width * 0.07f, shoulderY + armLength)
        val arm1Paint = Paint(outfitPaint)
        arm1Paint.shader = LinearGradient(arm1Rect.left, arm1Rect.top, arm1Rect.right, arm1Rect.bottom, arm1Paint.color, darkenColor(arm1Paint.color, 0.8f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(arm1Rect, 10f, 10f, arm1Paint)
        canvas.drawRoundRect(arm1Rect, 10f, 10f, darkOutlinePaint)
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, skinPaint)
        canvas.drawCircle(shoulderX, shoulderY + armLength + 4f, width * 0.06f, darkOutlinePaint)
        canvas.restore()

        // HEAD & FACE
        val headCX = cx
        val headCY = headY

        if (characterId == "ROBOT") {
            val headRect = RectF(headCX - headRadius, headCY - headRadius, headCX + headRadius, headCY + headRadius)
            hairHatPaint.shader = LinearGradient(headRect.left, headRect.top, headRect.right, headRect.bottom, hairHatPaint.color, darkenColor(hairHatPaint.color, 0.7f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(headRect, 18f, 18f, hairHatPaint)
            canvas.drawRoundRect(headRect, 18f, 18f, darkOutlinePaint)
            hairHatPaint.shader = null

            val visorRect = RectF(headCX - headRadius * 0.7f, headCY - headRadius * 0.3f, headCX + headRadius * 0.7f, headCY + headRadius * 0.3f)
            canvas.drawRoundRect(visorRect, 8f, 8f, accentPaint)
            canvas.drawRoundRect(visorRect, 8f, 8f, darkOutlinePaint)
        } else {
            skinPaint.shader = RadialGradient(headCX - headRadius * 0.2f, headCY - headRadius * 0.2f, headRadius * 1.5f, skinPaint.color, darkenColor(skinPaint.color, 0.8f), Shader.TileMode.CLAMP)
            canvas.drawCircle(headCX, headCY, headRadius, skinPaint)
            canvas.drawCircle(headCX, headCY, headRadius, darkOutlinePaint)
            skinPaint.shader = null

            val isBlinking = animState == AnimState.IDLE && (animTime % 3.5f > 3.3f)
            val eyeX1 = headCX + headRadius * 0.2f
            val eyeX2 = headCX + headRadius * 0.65f
            val eyeY = headCY - headRadius * 0.05f

            // Eyebrows
            val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3E2723"); strokeWidth = 4f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
            if (characterId != "NINJA") {
               canvas.drawLine(eyeX1 - 8f, eyeY - 14f, eyeX1 + 8f, eyeY - 10f, browPaint)
               canvas.drawLine(eyeX2 - 8f, eyeY - 10f, eyeX2 + 8f, eyeY - 14f, browPaint)
            }

            if (isBlinking) {
                canvas.drawLine(eyeX1 - 6f, eyeY, eyeX1 + 6f, eyeY, darkOutlinePaint)
                canvas.drawLine(eyeX2 - 6f, eyeY, eyeX2 + 6f, eyeY, darkOutlinePaint)
            } else {
                canvas.drawCircle(eyeX1, eyeY, width * 0.05f, eyePaint)
                canvas.drawCircle(eyeX2, eyeY, width * 0.05f, eyePaint)
                canvas.drawCircle(eyeX1 + 3f, eyeY - 3f, width * 0.02f, eyeHighlightPaint)
                canvas.drawCircle(eyeX2 + 3f, eyeY - 3f, width * 0.02f, eyeHighlightPaint)
            }

            val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#40FF8A80") }
            canvas.drawCircle(eyeX1, eyeY + 12f, 6f, cheekPaint)
            canvas.drawCircle(eyeX2, eyeY + 12f, 6f, cheekPaint)

            when (characterId) {
                "DEFAULT" -> {
                    val hoodPath = Path().apply {
                        moveTo(headCX - headRadius - 8f, headCY + headRadius + 4f)
                        quadTo(headCX - headRadius - 12f, headCY - headRadius - 12f, headCX, headCY - headRadius - 16f)
                        quadTo(headCX + headRadius + 12f, headCY - headRadius - 12f, headCX + headRadius + 8f, headCY + headRadius + 4f)
                        close()
                    }
                    val hoodGradient = LinearGradient(headCX, headCY - headRadius - 16f, headCX, headCY + headRadius, outfitPaint.color, darkenColor(outfitPaint.color, 0.7f), Shader.TileMode.CLAMP)
                    outfitPaint.shader = hoodGradient
                    canvas.drawPath(hoodPath, outfitPaint)
                    canvas.drawPath(hoodPath, darkOutlinePaint)
                    outfitPaint.shader = null

                    canvas.drawCircle(headCX + 6f, headCY + 2f, headRadius - 2f, skinPaint)

                    if (!isBlinking) {
                        canvas.drawCircle(eyeX1, eyeY, width * 0.05f, eyePaint)
                        canvas.drawCircle(eyeX2, eyeY, width * 0.05f, eyePaint)
                        canvas.drawCircle(eyeX1 + 3f, eyeY - 3f, width * 0.02f, eyeHighlightPaint)
                        canvas.drawCircle(eyeX2 + 3f, eyeY - 3f, width * 0.02f, eyeHighlightPaint)
                    }
                    canvas.drawLine(eyeX1 - 8f, eyeY - 10f, eyeX1 + 8f, eyeY - 14f, browPaint)
                    canvas.drawLine(eyeX2 - 8f, eyeY - 14f, eyeX2 + 8f, eyeY - 10f, browPaint)
                }
                "NINJA" -> {
                    val maskRect = RectF(headCX - headRadius, headCY, headCX + headRadius, headCY + headRadius)
                    outfitPaint.shader = LinearGradient(maskRect.left, maskRect.top, maskRect.right, maskRect.bottom, outfitPaint.color, darkenColor(outfitPaint.color, 0.7f), Shader.TileMode.CLAMP)
                    canvas.drawRoundRect(maskRect, 10f, 10f, outfitPaint)
                    canvas.drawRoundRect(maskRect, 10f, 10f, darkOutlinePaint)
                    outfitPaint.shader = null

                    val bandRect = RectF(headCX - headRadius - 2f, headCY - headRadius * 0.6f, headCX + headRadius + 2f, headCY - headRadius * 0.1f)
                    canvas.drawRect(bandRect, accentPaint)
                    canvas.drawRect(bandRect, darkOutlinePaint)
                }
                "GIRL" -> {
                    hairHatPaint.shader = RadialGradient(headCX - headRadius * 0.4f, headCY - headRadius * 0.5f, headRadius * 0.8f, hairHatPaint.color, darkenColor(hairHatPaint.color, 0.7f), Shader.TileMode.CLAMP)
                    canvas.drawCircle(headCX - headRadius * 0.4f, headCY - headRadius * 0.5f, headRadius * 0.8f, hairHatPaint)
                    canvas.drawCircle(headCX - headRadius * 0.4f, headCY - headRadius * 0.5f, headRadius * 0.8f, darkOutlinePaint)
                    hairHatPaint.shader = null
                    canvas.drawCircle(headCX - headRadius * 0.5f, headCY - headRadius * 0.8f, 10f, accentPaint)
                }
                "PIRATE" -> {
                    val hatPath = Path().apply {
                        moveTo(headCX - headRadius * 1.5f, headCY - headRadius * 0.2f)
                        quadTo(headCX, headCY - headRadius * 1.6f, headCX + headRadius * 1.5f, headCY - headRadius * 0.2f)
                        close()
                    }
                    hairHatPaint.shader = LinearGradient(headCX, headCY - headRadius * 1.6f, headCX, headCY, hairHatPaint.color, darkenColor(hairHatPaint.color, 0.7f), Shader.TileMode.CLAMP)
                    canvas.drawPath(hatPath, hairHatPaint)
                    canvas.drawPath(hatPath, darkOutlinePaint)
                    hairHatPaint.shader = null
                    canvas.drawLine(headCX - headRadius * 1.4f, headCY - headRadius * 0.3f, headCX + headRadius * 1.4f, headCY - headRadius * 0.3f, accentPaint)
                }
                "COWBOY" -> {
                    val brimRect = RectF(headCX - headRadius * 1.6f, headCY - headRadius * 0.5f, headCX + headRadius * 1.6f, headCY - headRadius * 0.2f)
                    canvas.drawRoundRect(brimRect, 14f, 14f, hairHatPaint)
                    canvas.drawRoundRect(brimRect, 14f, 14f, darkOutlinePaint)
                    val crownRect = RectF(headCX - headRadius * 0.8f, headCY - headRadius * 1.4f, headCX + headRadius * 0.8f, headCY - headRadius * 0.4f)
                    canvas.drawRoundRect(crownRect, 16f, 16f, hairHatPaint)
                    canvas.drawRoundRect(crownRect, 16f, 16f, darkOutlinePaint)
                }
                "ICE", "DESERT", "LAVA", "NEON", "FOREST", "GALAXY" -> {
                    val crownRect = RectF(headCX - headRadius * 1.1f, headCY - headRadius * 1.3f, headCX + headRadius * 1.1f, headCY - headRadius * 0.5f)
                    canvas.drawRoundRect(crownRect, 12f, 12f, accentPaint)
                    canvas.drawRoundRect(crownRect, 12f, 12f, darkOutlinePaint)
                }
            }
        }

        canvas.restore()
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= factor
        return Color.HSVToColor(hsv)
    }
}
