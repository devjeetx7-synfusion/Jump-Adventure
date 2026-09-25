package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jumpadventure.game.model.WorldInfo
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos

class LevelMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class NodeInfo(
        val levelNumber: Int,
        val isUnlocked: Boolean,
        val isCurrent: Boolean,
        val isBoss: Boolean,
        val stars: Int,
        val x: Float,
        val y: Float
    )

    var onNodeClicked: ((levelNumber: Int) -> Unit)? = null
    var selectedCharacterId: String = "DEFAULT"

    private val nodes = mutableListOf<NodeInfo>()
    private var mapTotalHeight = 1000f

    private var currentWorldInfo: WorldInfo? = null
    private var currentHighestLevel: Int = 1
    private var currentLevelNum: Int = 1
    private var effectiveCurrentLevel: Int = 1
    private var currentLevelStars: Map<Int, Int> = emptyMap()

    // Paints
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 16f
        pathEffect = DashPathEffect(floatArrayOf(24f, 16f), 0f)
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B2F")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val unlockedNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
    }
    private val currentNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB300")
    }
    private val lockedNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
    }
    private val bossNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1B2F")
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val starFacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B06A00") }
    private val starHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 255, 255, 255) }
    private val starStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#8A5400")
    }
    private val starPath = Path()
    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodeShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(110, 0, 0, 0) }

    private var animTime = 0f
    private var cachedBgBitmap: Bitmap? = null
    private val bgSrcRect = Rect()
    private val bgDstRect = RectF()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bgOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val nodeGlossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 255, 255, 255) }
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80FFD54F") }
    private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#455A64") }

    fun setupMap(
        worldInfo: WorldInfo,
        highestLevel: Int,
        currentLevel: Int,
        levelStars: Map<Int, Int>
    ) {
        currentWorldInfo = worldInfo
        currentHighestLevel = highestLevel
        currentLevelNum = currentLevel
        currentLevelStars = levelStars

        val bgResId = com.jumpadventure.game.level.WorldRepository.getWorldBackgroundRes(worldInfo.id)
        cachedBgBitmap = BitmapFactory.decodeResource(resources, bgResId)

        rebuildNodes()
        requestLayout()
        invalidate()
    }

    private fun rebuildNodes() {
        val worldInfo = currentWorldInfo ?: return
        nodes.clear()
        val totalLevels = worldInfo.endLevel - worldInfo.startLevel + 1
        val startLvl = worldInfo.startLevel

        val nodeSpacingY = 170f
        mapTotalHeight = totalLevels * nodeSpacingY + 300f

        val viewWidth = if (width > 0) width.toFloat() else 1080f
        val centerX = viewWidth / 2f
        val amplitude = viewWidth * 0.32f

        // The marker follows the latest playable/unlocked progression frontier.
        // currentLevel can temporarily point to a selected level, so highestLevel
        // is preferred as the persistent progression position on the map.
        effectiveCurrentLevel = when {
            currentHighestLevel in startLvl..worldInfo.endLevel -> currentHighestLevel
            currentHighestLevel > worldInfo.endLevel -> worldInfo.endLevel
            currentLevelNum in startLvl..worldInfo.endLevel -> currentLevelNum
            else -> startLvl
        }

        // Progression ascends upward: level 1 at bottom, higher levels toward top
        for (i in 0 until totalLevels) {
            val lvl = startLvl + i
            val isUnlocked = lvl <= currentHighestLevel
            val isCurr = lvl == effectiveCurrentLevel
            val isBoss = (lvl % 5 == 0) || (lvl == worldInfo.endLevel)
            val stars = currentLevelStars[lvl] ?: 0

            val angle = i * 0.7f
            val nx = centerX + sin(angle.toDouble()).toFloat() * amplitude
            val ny = mapTotalHeight - 150f - (i * nodeSpacingY)

            nodes.add(NodeInfo(lvl, isUnlocked, isCurr, isBoss, stars, nx, ny))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && w != oldw) {
            rebuildNodes()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        val targetHeight = maxOf(mapTotalHeight.toInt(), parentHeight)
        val hSpec = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        animTime += 0.05f

        val w = width.toFloat().coerceAtLeast(1f)
        val h = maxOf(mapTotalHeight, height.toFloat()).coerceAtLeast(1f)

        // 1. Full-viewport continuous world background artwork
        val bg = cachedBgBitmap
        if (bg != null && !bg.isRecycled) {
            val scale = maxOf(w / bg.width.toFloat(), h / bg.height.toFloat())
            val scaledW = bg.width.toFloat() * scale
            val scaledH = bg.height.toFloat() * scale
            val left = (w - scaledW) / 2f
            val top = (h - scaledH) / 2f

            bgSrcRect.set(0, 0, bg.width, bg.height)
            bgDstRect.set(left, top, left + scaledW, top + scaledH)
            canvas.drawBitmap(bg, bgSrcRect, bgDstRect, bgPaint)

            // Dark overlay for level chart readability
            bgOverlayPaint.color = Color.parseColor("#600B1426")
            canvas.drawRect(0f, 0f, w, h, bgOverlayPaint)
        } else {
            canvas.drawColor(Color.parseColor(currentWorldInfo?.skyColorHex ?: "#4CAF50"))
        }

        if (nodes.isEmpty()) return

        // 2. Path connecting nodes
        val path = Path()
        path.moveTo(nodes.first().x, nodes.first().y)
        for (i in 1 until nodes.size) {
            val prev = nodes[i - 1]
            val curr = nodes[i]
            val midY = (prev.y + curr.y) / 2f
            path.cubicTo(prev.x, midY, curr.x, midY, curr.x, curr.y)
        }
        canvas.drawPath(path, pathPaint)

        // 2. Draw Nodes
        nodes.forEach { node ->
            val radius = if (node.isBoss) 60f else 48f

            // Pulse effect for current level
            if (node.isCurrent) {
                val pulseRadius = radius + (sin(animTime * 4.0) * 8.0).toFloat()
                canvas.drawCircle(node.x, node.y, pulseRadius + 8f, pulsePaint)
            }

            val baseColor = when {
                node.isCurrent -> currentNodePaint.color
                node.isBoss -> bossNodePaint.color
                node.isUnlocked -> unlockedNodePaint.color
                else -> lockedNodePaint.color
            }
            val hsv = FloatArray(3)
            Color.colorToHSV(baseColor, hsv)
            hsv[2] *= 0.7f
            val darkColor = Color.HSVToColor(hsv)
            // 3D node: bottom shadow/extrusion, glossy gradient face, rim and highlight.
            canvas.drawCircle(node.x, node.y + 7f, radius + 1f, nodeShadowPaint)
            nodePaint.shader = RadialGradient(
                node.x - radius * 0.32f, node.y - radius * 0.34f,
                radius * 1.25f, baseColor, darkColor, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(node.x, node.y, radius, nodePaint)
            canvas.drawCircle(node.x, node.y, radius, outlinePaint)

            canvas.drawOval(
                RectF(
                    node.x - radius * 0.58f,
                    node.y - radius * 0.64f,
                    node.x + radius * 0.16f,
                    node.y - radius * 0.22f
                ),
                nodeGlossPaint
            )

            if (node.isUnlocked) {
                // Level Number with responsive text size for large numbers
                val numStr = "${node.levelNumber}"
                textPaint.textSize = when {
                    numStr.length >= 5 -> 18f
                    numStr.length == 4 -> 21f
                    numStr.length == 3 -> 25f
                    else -> if (node.isBoss) 32f else 28f
                }
                canvas.drawText(numStr, node.x, node.y + textPaint.textSize * 0.35f, textPaint)

                // Star Rating under node
                if (node.stars > 0) {
                    val starY = node.y + radius + 22f
                    for (s in 0 until node.stars) {
                        val starX = node.x + (s - (node.stars - 1) / 2f) * 24f
                        canvas.drawCircle(starX, starY, 8f, starFacePaint)
                        canvas.drawCircle(starX, starY, 8f, outlinePaint)
                    }
                }
            } else {
                val shacklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#90A4AE")
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                canvas.drawArc(RectF(node.x - 12f, node.y - 14f, node.x + 12f, node.y + 6f), 180f, 180f, false, shacklePaint)
                canvas.drawRoundRect(RectF(node.x - 16f, node.y - 4f, node.x + 16f, node.y + 16f), 4f, 4f, lockPaint)
                canvas.drawRoundRect(RectF(node.x - 16f, node.y - 4f, node.x + 16f, node.y + 16f), 4f, 4f, outlinePaint)
            }

            if (node.isUnlocked) {
                val starCount = node.stars.coerceIn(0, 3)
                draw3DStars(canvas, node.x, node.y + radius + 26f, starCount, radius)
            }

            // Draw exactly one character marker on the current playable level.
            if (node.levelNumber == effectiveCurrentLevel) {
                val charW = 76f
                val charH = 86f
                val anchorOffsetY = 5f
                val markerBottom = node.y - radius + anchorOffsetY
                val markerBounds = RectF(
                    node.x - charW / 2f,
                    markerBottom - charH,
                    node.x + charW / 2f,
                    markerBottom
                )
                CharacterRenderer.drawCharacter(
                    canvas = canvas,
                    bounds = markerBounds,
                    characterId = selectedCharacterId,
                    facingRight = true,
                    animState = CharacterRenderer.AnimState.IDLE,
                    animTime = animTime
                )
            }
        }
    }

    private fun draw3DStars(canvas: Canvas, centerX: Float, centerY: Float, count: Int, nodeRadius: Float) {
        val total = 3
        val size = (nodeRadius * 0.34f).coerceIn(13f, 21f)
        val spacing = size * 1.65f
        for (i in 0 until total) {
            val x = centerX + (i - 1) * spacing
            val earned = i < count

            starFacePaint.shader = if (earned) {
                LinearGradient(x - size, centerY - size, x + size, centerY + size,
                    Color.parseColor("#FFF59D"), Color.parseColor("#FFC107"), Shader.TileMode.CLAMP)
            } else {
                LinearGradient(x - size, centerY - size, x + size, centerY + size,
                    Color.parseColor("#D9E1EA"), Color.parseColor("#8C9BAE"), Shader.TileMode.CLAMP)
            }

            starPath.reset()
            for (p in 0 until 10) {
                val angle = Math.toRadians(-90.0 + p * 36.0)
                val radiusValue = if (p % 2 == 0) size else size * 0.45f
                val px = x + cos(angle).toFloat() * radiusValue
                val py = centerY + sin(angle).toFloat() * radiusValue
                if (p == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
            }
            starPath.close()

            // 3D bottom extrusion
            canvas.save()
            canvas.translate(0f, 3.5f)
            canvas.drawPath(starPath, starShadowPaint)
            canvas.restore()

            canvas.drawPath(starPath, starFacePaint)
            canvas.drawPath(starPath, starStrokePaint)

            if (earned) {
                val highlight = Path()
                highlight.moveTo(x - size * 0.42f, centerY - size * 0.38f)
                highlight.lineTo(x - size * 0.05f, centerY - size * 0.72f)
                highlight.lineTo(x - size * 0.16f, centerY - size * 0.18f)
                highlight.close()
                canvas.drawPath(highlight, starHighlightPaint)
            }
        }
        starFacePaint.shader = null
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val ex = event.x
            val ey = event.y

            nodes.forEach { node ->
                val radius = if (node.isBoss) 60f else 48f
                if (hypot((ex - node.x).toDouble(), (ey - node.y).toDouble()) <= radius * 1.3f) {
                    if (node.isUnlocked) {
                        onNodeClicked?.invoke(node.levelNumber)
                    }
                    return true
                }
            }
        }
        return true
    }
}
