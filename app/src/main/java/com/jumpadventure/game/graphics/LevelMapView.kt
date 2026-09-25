package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jumpadventure.game.level.WorldRepository
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
        val worldId: Int,
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
    private var maxRenderLevel = 50
    private var mapTotalHeight = 1000f
    private val nodeSpacingY = 170f

    private var currentHighestLevel: Int = 1
    private var currentLevelNum: Int = 1
    private var effectiveCurrentLevel: Int = 1
    private var currentLevelStars: Map<Int, Int> = emptyMap()

    // Paints
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90FFFFFF")
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
    private val nodeGlossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 255, 255, 255) }
    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80FFD54F") }
    private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#455A64") }

    // World Banner Paints
    private val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D90F172A")
    }
    private val bannerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD43B")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setupMap(
        highestLevel: Int,
        currentLevel: Int,
        levelStars: Map<Int, Int>
    ) {
        currentHighestLevel = maxOf(1, highestLevel)
        currentLevelNum = maxOf(1, currentLevel)
        currentLevelStars = levelStars

        // Render nodes well past the current highest level to support infinite upward scrolling
        maxRenderLevel = maxOf(currentHighestLevel + 35, 50)

        rebuildNodes()
        requestLayout()
        invalidate()
    }

    fun getScrollYForLevel(levelNumber: Int, viewportHeight: Int): Int {
        val safeLevel = levelNumber.coerceIn(1, maxRenderLevel)
        val nodeIndex = safeLevel - 1
        val nodeY = mapTotalHeight - 200f - (nodeIndex * nodeSpacingY)
        val targetScrollY = (nodeY - viewportHeight / 2f).toInt()
        val maxScroll = (mapTotalHeight - viewportHeight).toInt().coerceAtLeast(0)
        return targetScrollY.coerceIn(0, maxScroll)
    }

    private fun rebuildNodes() {
        nodes.clear()
        mapTotalHeight = maxRenderLevel * nodeSpacingY + 400f

        val viewWidth = if (width > 0) width.toFloat() else 1080f
        val centerX = viewWidth / 2f
        val amplitude = viewWidth * 0.32f

        effectiveCurrentLevel = currentHighestLevel

        for (i in 0 until maxRenderLevel) {
            val lvl = i + 1
            val world = WorldRepository.getWorldForLevel(lvl)
            val isUnlocked = lvl <= currentHighestLevel
            val isCurr = lvl == effectiveCurrentLevel
            val isBoss = (lvl % 5 == 0) || (lvl % 25 == 0)
            val stars = currentLevelStars[lvl] ?: 0

            val angle = i * 0.7f
            val nx = centerX + sin(angle.toDouble()).toFloat() * amplitude
            val ny = mapTotalHeight - 200f - (i * nodeSpacingY)

            nodes.add(NodeInfo(lvl, world.id, isUnlocked, isCurr, isBoss, stars, nx, ny))
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
        val totalH = height.toFloat().coerceAtLeast(mapTotalHeight)
        val clipBounds = canvas.clipBounds
        val visibleTop = clipBounds.top.toFloat() - 200f
        val visibleBottom = clipBounds.bottom.toFloat() + 200f

        // Base continuous environment fill over entire view height to eliminate any black unpainted region
        val bottomWorld = WorldRepository.getWorldForLevel(1)
        val baseSkyColor = Color.parseColor(bottomWorld.skyColorHex)
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, totalH,
            Color.parseColor("#0B132B"), baseSkyColor, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, totalH, bgPaint)
        bgPaint.shader = null

        // 1. Draw World Background Sections with Seamless Color Transitions
        val minVisibleWorldId = maxOf(1, WorldRepository.getWorldForLevel(nodes.firstOrNull { it.y <= visibleBottom }?.levelNumber ?: 1).id - 1)
        val maxVisibleWorldId = WorldRepository.getWorldForLevel(nodes.lastOrNull { it.y >= visibleTop }?.levelNumber ?: maxRenderLevel).id + 1

        for (worldId in minVisibleWorldId..maxVisibleWorldId) {
            val worldStartLvl = (worldId - 1) * 25 + 1
            val worldEndLvl = worldId * 25

            var worldStartY = mapTotalHeight - 200f - ((worldStartLvl - 1) * nodeSpacingY) + nodeSpacingY * 0.5f
            var worldEndY = mapTotalHeight - 200f - ((worldEndLvl - 1) * nodeSpacingY) - nodeSpacingY * 0.5f

            if (worldId == 1) {
                worldStartY = maxOf(worldStartY, totalH)
            }
            if (worldId == maxVisibleWorldId) {
                worldEndY = minOf(worldEndY, 0f)
            }

            if (worldEndY > visibleBottom || worldStartY < visibleTop) continue

            val currentWorld = WorldRepository.getWorldForLevel(worldStartLvl)
            val nextWorld = WorldRepository.getWorldForLevel(worldEndLvl + 1)

            val sky1 = Color.parseColor(currentWorld.skyColorHex)
            val sky2 = Color.parseColor(nextWorld.skyColorHex)

            bgPaint.shader = LinearGradient(
                0f, worldStartY, 0f, worldEndY,
                sky1, sky2, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, worldEndY, w, worldStartY, bgPaint)
            bgPaint.shader = null

            // World Title Ribbon at transition between worlds
            if (worldStartY in visibleTop..visibleBottom) {
                val bannerW = (w * 0.75f).coerceAtMost(550f)
                val bannerH = 54f
                val bannerRect = RectF((w - bannerW) / 2f, worldStartY - bannerH / 2f, (w + bannerW) / 2f, worldStartY + bannerH / 2f)

                canvas.drawRoundRect(bannerRect, 16f, 16f, bannerBgPaint)
                canvas.drawRoundRect(bannerRect, 16f, 16f, bannerBorderPaint)

                val worldTitleText = "WORLD ${currentWorld.id}: ${currentWorld.name.uppercase()}"
                bannerTextPaint.textSize = when {
                    worldTitleText.length > 30 -> 18f
                    worldTitleText.length > 24 -> 20f
                    else -> 24f
                }
                canvas.drawText(worldTitleText, w / 2f, bannerRect.centerY() + bannerTextPaint.textSize * 0.35f, bannerTextPaint)
            }
        }

        if (nodes.isEmpty()) return

        // 2. Visible Path Connecting Nodes
        val path = Path()
        val visibleNodes = nodes.filter { it.y in visibleTop..visibleBottom }
        if (visibleNodes.isNotEmpty()) {
            val firstIdx = nodes.indexOf(visibleNodes.first()).coerceAtLeast(0)
            val lastIdx = nodes.indexOf(visibleNodes.last()).coerceAtMost(nodes.size - 1)
            val startDrawIdx = maxOf(0, firstIdx - 1)
            val endDrawIdx = minOf(nodes.size - 1, lastIdx + 1)

            path.moveTo(nodes[startDrawIdx].x, nodes[startDrawIdx].y)
            for (i in (startDrawIdx + 1)..endDrawIdx) {
                val prev = nodes[i - 1]
                val curr = nodes[i]
                val midY = (prev.y + curr.y) / 2f
                path.cubicTo(prev.x, midY, curr.x, midY, curr.x, curr.y)
            }
            canvas.drawPath(path, pathPaint)
        }

        // 3. Draw Visible Level Nodes
        visibleNodes.forEach { node ->
            val radius = if (node.isBoss) 60f else 48f

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
                val numStr = "${node.levelNumber}"
                textPaint.textSize = when {
                    numStr.length >= 5 -> 18f
                    numStr.length == 4 -> 21f
                    numStr.length == 3 -> 25f
                    else -> if (node.isBoss) 32f else 28f
                }
                canvas.drawText(numStr, node.x, node.y + textPaint.textSize * 0.35f, textPaint)

                if (node.stars > 0) {
                    val starCount = node.stars.coerceIn(0, 3)
                    draw3DStars(canvas, node.x, node.y + radius + 26f, starCount, radius)
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

        }

        // 4. Character Marker Overlay - Drawn strictly AFTER all paths, nodes, and stars
        val currentNode = nodes.firstOrNull { it.levelNumber == effectiveCurrentLevel }
        if (currentNode != null && currentNode.y in (visibleTop - 100f)..(visibleBottom + 100f)) {
            val radius = if (currentNode.isBoss) 60f else 48f
            val charW = 76f
            val charH = 86f
            val anchorOffsetY = 5f
            val markerBottom = currentNode.y - radius + anchorOffsetY
            val markerBounds = RectF(
                currentNode.x - charW / 2f,
                markerBottom - charH,
                currentNode.x + charW / 2f,
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
