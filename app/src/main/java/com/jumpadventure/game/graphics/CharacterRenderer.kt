package com.jumpadventure.game.graphics

import android.graphics.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural renderer for the Jump Adventure mascot **"Scout"** — an original
 * chibi explorer: orange beanie with aviator goggles, flowing red scarf,
 * teal hoodie with a glowing chest emblem, cargo pants and blue sneakers.
 *
 * The engine is a 2D [Canvas] surface (no GL), so the mascot is drawn as a
 * richly shaded, pseudo-3D character: radial/linear gradients for volume,
 * rim highlights, squash & stretch and a physics-driven scarf.
 *
 * Shop characters reuse the same model with their own palettes, and ROBOT
 * variants keep their visor head.
 */
object CharacterRenderer {

    enum class AnimState { IDLE, RUN, JUMP, DOUBLE_JUMP, FALL, LAND, HIT, GAME_OVER }

    // ------------------------------------------------------------------
    // Mascot palette (Scout's signature look)
    // ------------------------------------------------------------------
    private val INK = Color.parseColor("#232338")
    private val SKIN = Color.parseColor("#FFD9B3")
    private val SKIN_SHADE = Color.parseColor("#F0B98A")
    private val BLUSH = Color.parseColor("#40F2788F")
    private val BEANIE = Color.parseColor("#FF8A3D")
    private val BEANIE_SHADE = Color.parseColor("#E06A1F")
    private val GOGGLE = Color.parseColor("#35D6FF")
    private val GOGGLE_FRAME = Color.parseColor("#37474F")
    private val SCARF = Color.parseColor("#E5484D")
    private val SCARF_SHADE = Color.parseColor("#B9313B")
    private val HOODIE = Color.parseColor("#19B8A6")
    private val HOODIE_SHADE = Color.parseColor("#0E8A7C")
    private val EMBLEM = Color.parseColor("#8FF7FF")
    private val PANTS = Color.parseColor("#2E3A59")
    private val PANTS_SHADE = Color.parseColor("#232C46")
    private val SHOE = Color.parseColor("#3D7BFF")
    private val SHOE_SHADE = Color.parseColor("#2B5FD9")
    private val SOLE = Color.parseColor("#F5F7FA")
    private val PACK = Color.parseColor("#8D5A2B")
    private val PACK_SHADE = Color.parseColor("#6E4420")

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reusablePath = Path()

    fun drawCharacter(
        canvas: Canvas,
        bounds: RectF,
        characterId: String,
        skinId: String = "DEFAULT",
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

        // Palette per shop character / equipped skin (game economy untouched).
        val (outfit, hair, accent, shoe) = paletteFor(characterId, skinId)
        val isRobot = characterId.contains("ROBOT")

        // --------------------------------------------------------------
        // Pose evaluation: squash & stretch + limb angles per state.
        // --------------------------------------------------------------
        var bounceY = 0f
        var scaleY = 1f
        var scaleX = 1f
        var tilt = 0f
        var legA1 = 0f; var legA2 = 0f
        var armA1 = 0f; var armA2 = 0f
        var scarfSway = 0f
        var eyesClosed = false
        var dizzy = false

        when (animState) {
            AnimState.IDLE -> {
                bounceY = sin(animTime * 3.2f) * height * 0.008f
                legA1 = sin(animTime * 3.2f) * 6f
                legA2 = -legA1
                armA1 = -sin(animTime * 3.2f) * 8f
                armA2 = sin(animTime * 3.2f) * 8f
                scarfSway = sin(animTime * 2.2f) * 4f
            }
            AnimState.RUN -> {
                val t = animTime * 12f
                bounceY = abs(sin(t)) * height * 0.045f
                legA1 = sin(t) * 34f
                legA2 = -legA1
                armA1 = -sin(t) * 40f
                armA2 = sin(t) * 40f
                scarfSway = 10f + sin(animTime * 9f) * 3f
            }
            AnimState.JUMP -> {
                scaleY = 1.14f; scaleX = 0.9f
                legA1 = -24f; legA2 = 22f
                armA1 = -55f; armA2 = -35f
                scarfSway = 14f
            }
            AnimState.DOUBLE_JUMP -> {
                scaleY = 1.22f; scaleX = 0.84f
                legA1 = -48f; legA2 = 40f
                armA1 = -140f; armA2 = 160f // brief windmill
                scarfSway = 18f
            }
            AnimState.FALL -> {
                scaleY = 0.96f; scaleX = 1.04f
                legA1 = 14f; legA2 = -18f
                armA1 = -70f; armA2 = -95f
                scarfSway = 16f
            }
            AnimState.LAND -> {
                scaleY = 0.74f; scaleX = 1.22f
                legA1 = 18f; legA2 = -14f
                armA1 = 30f; armA2 = 38f
                scarfSway = 6f
            }
            AnimState.HIT -> {
                tilt = -8f
                scaleY = 0.94f; scaleX = 1.06f
                legA1 = 22f; legA2 = -26f
                armA1 = -120f; armA2 = 130f
                dizzy = true
                scarfSway = 8f
            }
            AnimState.GAME_OVER -> {
                tilt = 10f
                scaleY = 0.9f; scaleX = 1.05f
                bounceY = height * 0.02f
                legA1 = 8f; legA2 = -8f
                armA1 = 55f; armA2 = 65f
                eyesClosed = true
                scarfSway = 2f
            }
        }

        canvas.translate(cx, bounds.bottom)
        canvas.rotate(tilt, 0f, 0f)
        canvas.scale(scaleX, scaleY)
        canvas.translate(-cx, -bounds.bottom)

        val topY = bounds.top + bounceY

        // Proportions
        val headR = height * 0.185f
        val headCY = topY + headR + height * 0.035f
        val shoulderY = topY + height * 0.40f
        val hipY = topY + height * 0.66f
        val legLen = height * 0.26f
        val armLen = height * 0.21f

        // --------------------------------------------------------------
        // BACK ARM (behind torso)
        // --------------------------------------------------------------
        drawLimb(canvas, cx, shoulderY, armA2, armLen, width * 0.075f, shade(outfit, 0.72f), shade(skin, 0.9f), width)

        // --------------------------------------------------------------
        // LEGS + SHOES
        // --------------------------------------------------------------
        val shoeW = width * 0.24f
        val shoeH = height * 0.085f
        // back leg
        drawLeg(canvas, cx, hipY, legA2, legLen, width * 0.085f, shade(PANTS, 0.8f), shoeW, shoeH, shoe, false, width)
        // front leg
        drawLeg(canvas, cx, hipY, legA1, legLen, width * 0.085f, PANTS, shoeW, shoeH, shoe, true, width)

        // --------------------------------------------------------------
        // BACKPACK (behind torso, peeks out on the left)
        // --------------------------------------------------------------
        run {
            val packRect = RectF(cx - width * 0.34f, topY + height * 0.40f, cx - width * 0.12f, topY + height * 0.62f)
            fillPaint.shader = LinearGradient(packRect.left, packRect.top, packRect.right, packRect.bottom, PACK, PACK_SHADE, Shader.TileMode.CLAMP)
            outlinePaint.strokeWidth = width * 0.035f
            canvas.drawRoundRect(packRect, 10f, 10f, fillPaint)
            canvas.drawRoundRect(packRect, 10f, 10f, outlinePaint)
            fillPaint.shader = null
            // pack strap
            fillPaint.color = PACK_SHADE
            canvas.drawRect(RectF(cx - width * 0.13f, topY + height * 0.38f, cx - width * 0.05f, topY + height * 0.60f), fillPaint)
        }

        // --------------------------------------------------------------
        // TORSO (hoodie) with gradient volume + chest emblem
        // --------------------------------------------------------------
        val torso = RectF(cx - width * 0.23f, topY + height * 0.345f, cx + width * 0.23f, topY + height * 0.68f)
        fillPaint.shader = LinearGradient(torso.left, torso.top, torso.right * 0.7f, torso.bottom, outfit, shade(outfit, 0.66f), Shader.TileMode.CLAMP)
        outlinePaint.strokeWidth = width * 0.04f
        canvas.drawRoundRect(torso, 22f, 22f, fillPaint)
        canvas.drawRoundRect(torso, 22f, 22f, outlinePaint)
        fillPaint.shader = null
        // zipper
        val zipPaint = Paint(outlinePaint).apply { strokeWidth = width * 0.018f }
        canvas.drawLine(cx, torso.top + 8f, cx, torso.bottom - 6f, zipPaint)
        // glowing chest emblem (triangle)
        reusablePath.reset()
        val emCx = cx; val emCy = torso.centerY() + height * 0.01f; val emR = width * 0.085f
        reusablePath.moveTo(emCx, emCy - emR)
        reusablePath.lineTo(emCx + emR * 0.95f, emCy + emR * 0.75f)
        reusablePath.lineTo(emCx - emR * 0.95f, emCy + emR * 0.75f)
        reusablePath.close()
        fillPaint.color = accent
        canvas.drawPath(reusablePath, fillPaint)
        canvas.drawPath(reusablePath, outlinePaint)
        // kangaroo pocket
        fillPaint.color = shade(outfit, 0.85f)
        canvas.drawRoundRect(RectF(cx - width * 0.14f, torso.bottom - height * 0.10f, cx + width * 0.14f, torso.bottom - 4f), 8f, 8f, fillPaint)

        // --------------------------------------------------------------
        // SCARF (flows behind the neck, wave driven by pose)
        // --------------------------------------------------------------
        run {
            val neckY = topY + height * 0.345f
            reusablePath.reset()
            val w = width * 0.30f
            val wave = scarfSway
            reusablePath.moveTo(cx + width * 0.05f, neckY)
            reusablePath.quadTo(cx - w * 0.6f, neckY - 4f + wave * 0.3f, cx - w * 1.15f, neckY + wave)
            reusablePath.quadTo(cx - w * 0.7f, neckY + height * 0.09f + wave * 0.5f, cx + width * 0.02f, neckY + height * 0.075f)
            reusablePath.close()
            fillPaint.shader = LinearGradient(cx, neckY, cx - w * 1.2f, neckY + height * 0.08f, SCARF, SCARF_SHADE, Shader.TileMode.CLAMP)
            outlinePaint.strokeWidth = width * 0.03f
            canvas.drawPath(reusablePath, fillPaint)
            canvas.drawPath(reusablePath, outlinePaint)
            fillPaint.shader = null
            // scarf knot
            fillPaint.color = SCARF_SHADE
            canvas.drawCircle(cx + width * 0.04f, neckY + height * 0.02f, width * 0.055f, fillPaint)
            canvas.drawCircle(cx + width * 0.04f, neckY + height * 0.02f, width * 0.055f, outlinePaint)
        }

        // --------------------------------------------------------------
        // FRONT ARM
        // --------------------------------------------------------------
        drawLimb(canvas, cx, shoulderY, armA1, armLen, width * 0.075f, outfit, skin, width)

        // --------------------------------------------------------------
        // HEAD
        // --------------------------------------------------------------
        if (isRobot) {
            val headRect = RectF(cx - headR, headCY - headR, cx + headR, headCY + headR)
            fillPaint.shader = LinearGradient(headRect.left, headRect.top, headRect.right, headRect.bottom, hair, shade(hair, 0.65f), Shader.TileMode.CLAMP)
            outlinePaint.strokeWidth = width * 0.04f
            canvas.drawRoundRect(headRect, 18f, 18f, fillPaint)
            canvas.drawRoundRect(headRect, 18f, 18f, outlinePaint)
            fillPaint.shader = null
            val visor = RectF(cx - headR * 0.72f, headCY - headR * 0.28f, cx + headR * 0.72f, headCY + headR * 0.32f)
            fillPaint.color = accent
            canvas.drawRoundRect(visor, 8f, 8f, fillPaint)
            canvas.drawRoundRect(visor, 8f, 8f, outlinePaint)
            // antenna
            canvas.drawLine(cx, headCY - headR, cx, headCY - headR - height * 0.05f, outlinePaint)
            fillPaint.color = accent
            canvas.drawCircle(cx, headCY - headR - height * 0.05f, width * 0.035f, fillPaint)
        } else {
            // face base with soft volume gradient
            fillPaint.shader = RadialGradient(cx - headR * 0.25f, headCY - headR * 0.3f, headR * 1.6f, SKIN, SKIN_SHADE, Shader.TileMode.CLAMP)
            outlinePaint.strokeWidth = width * 0.04f
            canvas.drawCircle(cx, headCY, headR, fillPaint)
            canvas.drawCircle(cx, headCY, headR, outlinePaint)
            fillPaint.shader = null

            // ---- beanie ----
            reusablePath.reset()
            reusablePath.moveTo(cx - headR * 1.06f, headCY - headR * 0.15f)
            reusablePath.quadTo(cx - headR * 1.1f, headCY - headR * 1.25f, cx, headCY - headR * 1.3f)
            reusablePath.quadTo(cx + headR * 1.1f, headCY - headR * 1.25f, cx + headR * 1.06f, headCY - headR * 0.15f)
            reusablePath.close()
            fillPaint.shader = LinearGradient(cx, headCY - headR * 1.35f, cx, headCY, BEANIE, BEANIE_SHADE, Shader.TileMode.CLAMP)
            canvas.drawPath(reusablePath, fillPaint)
            canvas.drawPath(reusablePath, outlinePaint)
            fillPaint.shader = null
            // beanie ribbed band
            fillPaint.color = BEANIE_SHADE
            canvas.drawRoundRect(RectF(cx - headR * 1.07f, headCY - headR * 0.42f, cx + headR * 1.07f, headCY - headR * 0.08f), 10f, 10f, fillPaint)
            canvas.drawRoundRect(RectF(cx - headR * 1.07f, headCY - headR * 0.42f, cx + headR * 1.07f, headCY - headR * 0.08f), 10f, 10f, outlinePaint)
            // pom-pom
            fillPaint.color = Color.parseColor("#FFF3E0")
            canvas.drawCircle(cx, headCY - headR * 1.32f, width * 0.06f, fillPaint)
            canvas.drawCircle(cx, headCY - headR * 1.32f, width * 0.06f, outlinePaint)

            // ---- aviator goggles pushed up on the beanie ----
            val gY = headCY - headR * 0.52f
            fillPaint.color = GOGGLE_FRAME
            canvas.drawRoundRect(RectF(cx - headR * 0.62f, gY - headR * 0.16f, cx - headR * 0.06f, gY + headR * 0.16f), 6f, 6f, fillPaint)
            canvas.drawRoundRect(RectF(cx + headR * 0.06f, gY - headR * 0.16f, cx + headR * 0.62f, gY + headR * 0.16f), 6f, 6f, fillPaint)
            fillPaint.color = GOGGLE
            canvas.drawRoundRect(RectF(cx - headR * 0.56f, gY - headR * 0.11f, cx - headR * 0.12f, gY + headR * 0.11f), 5f, 5f, fillPaint)
            canvas.drawRoundRect(RectF(cx + headR * 0.12f, gY - headR * 0.11f, cx + headR * 0.56f, gY + headR * 0.11f), 5f, 5f, fillPaint)
            // lens glint
            fillPaint.color = Color.WHITE
            canvas.drawRect(RectF(cx - headR * 0.52f, gY - headR * 0.09f, cx - headR * 0.40f, gY - headR * 0.02f), fillPaint)
            canvas.drawRect(RectF(cx + headR * 0.16f, gY - headR * 0.09f, cx + headR * 0.28f, gY - headR * 0.02f), fillPaint)
            // strap
            fillPaint.color = GOGGLE_FRAME
            canvas.drawRect(RectF(cx - headR * 0.06f, gY - headR * 0.05f, cx + headR * 0.06f, gY + headR * 0.05f), fillPaint)

            // ---- face ----
            val eyeY = headCY + headR * 0.12f
            val eyeX1 = cx + headR * 0.30f
            val eyeX2 = cx + headR * 0.68f
            val blink = animState == AnimState.IDLE && (animTime % 3.5f > 3.28f)

            // brows
            val browPaint = Paint(outlinePaint).apply { strokeWidth = width * 0.022f }
            if (!dizzy) {
                canvas.drawLine(eyeX1 - 7f, eyeY - 15f, eyeX1 + 7f, eyeY - 11f, browPaint)
                canvas.drawLine(eyeX2 - 7f, eyeY - 11f, eyeX2 + 7f, eyeY - 15f, browPaint)
            }

            if (eyesClosed) {
                canvas.drawLine(eyeX1 - 7f, eyeY, eyeX1 + 7f, eyeY, outlinePaint)
                canvas.drawLine(eyeX2 - 7f, eyeY, eyeX2 + 7f, eyeY, outlinePaint)
            } else if (dizzy) {
                // spiral-ish dizzy eyes: draw small crosses
                canvas.drawLine(eyeX1 - 5f, eyeY - 5f, eyeX1 + 5f, eyeY + 5f, outlinePaint)
                canvas.drawLine(eyeX1 - 5f, eyeY + 5f, eyeX1 + 5f, eyeY - 5f, outlinePaint)
                canvas.drawLine(eyeX2 - 5f, eyeY - 5f, eyeX2 + 5f, eyeY + 5f, outlinePaint)
                canvas.drawLine(eyeX2 - 5f, eyeY + 5f, eyeX2 + 5f, eyeY - 5f, outlinePaint)
            } else if (blink) {
                canvas.drawLine(eyeX1 - 6f, eyeY, eyeX1 + 6f, eyeY, outlinePaint)
                canvas.drawLine(eyeX2 - 6f, eyeY, eyeX2 + 6f, eyeY, outlinePaint)
            } else {
                val eyeR = width * 0.052f
                fillPaint.color = INK
                canvas.drawCircle(eyeX1, eyeY, eyeR, fillPaint)
                canvas.drawCircle(eyeX2, eyeY, eyeR, fillPaint)
                fillPaint.color = Color.WHITE
                canvas.drawCircle(eyeX1 + eyeR * 0.35f, eyeY - eyeR * 0.35f, eyeR * 0.38f, fillPaint)
                canvas.drawCircle(eyeX2 + eyeR * 0.35f, eyeY - eyeR * 0.35f, eyeR * 0.38f, fillPaint)
            }

            // blush + mouth
            fillPaint.color = BLUSH
            canvas.drawCircle(eyeX1 - 4f, eyeY + 13f, 6f, fillPaint)
            canvas.drawCircle(eyeX2 + 4f, eyeY + 13f, 6f, fillPaint)
            val mouthPaint = Paint(outlinePaint).apply { strokeWidth = width * 0.02f }
            reusablePath.reset()
            if (dizzy || eyesClosed) {
                canvas.drawLine(cx + headR * 0.34f, eyeY + 20f, cx + headR * 0.62f, eyeY + 20f, mouthPaint)
            } else {
                reusablePath.moveTo(cx + headR * 0.34f, eyeY + 18f)
                reusablePath.quadTo(cx + headR * 0.5f, eyeY + 26f, cx + headR * 0.64f, eyeY + 18f)
                canvas.drawPath(reusablePath, mouthPaint)
            }
        }

        // Hit flash overlay
        if (animState == AnimState.HIT) {
            fillPaint.color = Color.WHITE
            fillPaint.alpha = 90
            canvas.drawCircle(cx, headCY, headR * 1.25f, fillPaint)
            fillPaint.alpha = 255
        }

        canvas.restore()
    }

    /** Draws one arm (sleeve + hand) rotated by [angleDeg] around the shoulder. */
    private fun drawLimb(
        canvas: Canvas,
        sx: Float, sy: Float,
        angleDeg: Float, length: Float,
        thickness: Float,
        sleeveColor: Int, handColor: Int,
        refWidth: Float
    ) {
        canvas.save()
        canvas.rotate(angleDeg, sx, sy)
        fillPaint.color = sleeveColor
        outlinePaint.strokeWidth = refWidth * 0.035f
        canvas.drawRoundRect(RectF(sx - thickness, sy, sx + thickness, sy + length), 10f, 10f, fillPaint)
        canvas.drawRoundRect(RectF(sx - thickness, sy, sx + thickness, sy + length), 10f, 10f, outlinePaint)
        fillPaint.color = handColor
        canvas.drawCircle(sx, sy + length + thickness * 0.4f, thickness * 0.95f, fillPaint)
        canvas.drawCircle(sx, sy + length + thickness * 0.4f, thickness * 0.95f, outlinePaint)
        canvas.restore()
    }

    /** Draws one leg (pants + shoe) rotated by [angleDeg] around the hip. */
    private fun drawLeg(
        canvas: Canvas,
        hx: Float, hy: Float,
        angleDeg: Float, length: Float,
        thickness: Float,
        pantsColor: Int,
        shoeW: Float, shoeH: Float,
        shoeColor: Int,
        front: Boolean,
        refWidth: Float
    ) {
        canvas.save()
        canvas.rotate(angleDeg, hx, hy)
        fillPaint.shader = LinearGradient(hx, hy, hx, hy + length, pantsColor, shade(pantsColor, 0.75f), Shader.TileMode.CLAMP)
        outlinePaint.strokeWidth = refWidth * 0.035f
        canvas.drawRoundRect(RectF(hx - thickness, hy, hx + thickness, hy + length), 12f, 12f, fillPaint)
        canvas.drawRoundRect(RectF(hx - thickness, hy, hx + thickness, hy + length), 12f, 12f, outlinePaint)
        fillPaint.shader = null
        val shoeRect = RectF(hx - shoeW * 0.3f, hy + length - 4f, hx + shoeW * 0.75f, hy + length + shoeH)
        fillPaint.color = shoeColor
        canvas.drawRoundRect(shoeRect, 10f, 10f, fillPaint)
        canvas.drawRoundRect(shoeRect, 10f, 10f, outlinePaint)
        if (front) {
            fillPaint.color = SOLE
            canvas.drawRect(RectF(shoeRect.left, shoeRect.bottom - 4f, shoeRect.right, shoeRect.bottom), fillPaint)
            // lace dot
            fillPaint.color = shade(shoeColor, 1.25f)
            canvas.drawCircle(hx + shoeW * 0.28f, hy + length + shoeH * 0.3f, refWidth * 0.02f, fillPaint)
        }
        canvas.restore()
    }

    /** Shop-character palettes (unchanged economy, new mascot model). */
    private fun paletteFor(characterId: String, skinId: String): List<Int> {
        val base = when (characterId) {
            "NINJA", "CYBER_NINJA", "SHADOW_HUNTER" -> listOf("#263238", "#111111", "#E53935", "#111111")
            "ROBOT", "ROBOT_X" -> listOf("#78909C", "#455A64", "#00E676", "#37474F")
            "GIRL" -> listOf("#EC407A", "#F48FB1", "#FFD54F", "#FFFFFF")
            "PIRATE" -> listOf("#D84315", "#3E2723", "#FFD700", "#3E2723")
            "COWBOY" -> listOf("#8D6E63", "#5D4037", "#FFB300", "#5D4037")
            "ICE", "ICE_WARRIOR", "ARCTIC_RANGER" -> listOf("#00ACC1", "#B2EBF2", "#00E5FF", "#FFFFFF")
            "DESERT", "DESERT_RUNNER" -> listOf("#FB8C00", "#FFE082", "#FF6D00", "#5D4037")
            "LAVA", "LAVA_KNIGHT" -> listOf("#D84315", "#212121", "#FF3D00", "#212121")
            "NEON", "NEON_RUNNER" -> listOf("#00E676", "#212121", "#00E5FF", "#00E676")
            "FOREST", "FOREST_GUARDIAN", "JUNGLE_FIGHTER" -> listOf("#4CAF50", "#1B5E20", "#81C784", "#33691E")
            "GALAXY", "GALAXY_HERO", "SPACE_RUNNER" -> listOf("#7B1FA2", "#1A237E", "#E040FB", "#4A148C")
            "STORM_RIDER", "THUNDER_HERO" -> listOf("#0284C7", "#0369A1", "#FFD700", "#0284C7")
            "SAMURAI" -> listOf("#B91C1C", "#1E1B4B", "#F59E0B", "#1E1B4B")
            "CRYSTAL_MAGE", "VOID_WALKER" -> listOf("#6D28D9", "#4C1D95", "#06B6D4", "#4C1D95")
            "GOLDEN_WARRIOR" -> listOf("#D97706", "#78350F", "#F59E0B", "#78350F")
            "EXPLORER" -> listOf("#059669", "#064E3B", "#F59E0B", "#064E3B")
            else -> listOf("#19B8A6", "#FF8A3D", "#8FF7FF", "#3D7BFF") // Scout default
        }
        fun c(s: String) = Color.parseColor(s)
        val outfit = when (skinId) {
            "GOLDEN", "GOLDEN_HOODIE" -> c("#FFD700")
            "BLACK", "SHADOW_ARMOR" -> c("#212121")
            "NEON", "NEON_NINJA" -> c("#00E676")
            "WINTER", "ICE" -> c("#E0F7FA")
            "LAVA", "DRAGON_FLAME" -> c("#FF3D00")
            "CYBER_MECH" -> c("#00E5FF")
            "GALAXY", "COSMIC_GALAXY" -> c("#9C27B0")
            else -> c(base[0])
        }
        val hair = when (skinId) {
            "GOLDEN", "GOLDEN_HOODIE" -> c("#B45309")
            "BLACK", "SHADOW_ARMOR" -> c("#0F172A")
            "NEON", "NEON_NINJA" -> c("#15803D")
            "WINTER", "ICE" -> c("#818CF8")
            "LAVA", "DRAGON_FLAME" -> c("#7C2D12")
            "CYBER_MECH" -> c("#1E293B")
            "GALAXY", "COSMIC_GALAXY" -> c("#4C1D95")
            else -> c(base[1])
        }
        val accent = when (skinId) {
            "GOLDEN", "GOLDEN_HOODIE" -> c("#FFFBEB")
            "BLACK", "SHADOW_ARMOR" -> c("#A855F7")
            "NEON", "NEON_NINJA" -> c("#22D3EE")
            "WINTER", "ICE" -> c("#38BDF8")
            "LAVA", "DRAGON_FLAME" -> c("#FACC15")
            "CYBER_MECH" -> c("#F43F5E")
            "GALAXY", "COSMIC_GALAXY" -> c("#F472B6")
            else -> c(base[2])
        }
        val shoe = c(base[3])
        return listOf(outfit, hair, accent, shoe)
    }

    private fun shade(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }
}
