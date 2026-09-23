package com.jumpadventure.game.graphics

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jumpadventure.game.model.WorldInfo
import kotlin.math.hypot
import kotlin.math.sin

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
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
    }

    private var animTime = 0f

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

        // Progression ascends upward: level 1 at bottom, higher levels toward top
        for (i in 0 until totalLevels) {
            val lvl = startLvl + i
            val isUnlocked = lvl <= currentHighestLevel
            val isCurr = lvl == currentLevelNum
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
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val hSpec = MeasureSpec.makeMeasureSpec(mapTotalHeight.toInt(), MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        animTime += 0.05f

        if (nodes.isEmpty()) return

        // 1. Draw Environment and Path connecting nodes
        val envPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(currentWorldInfo?.skyColorHex ?: "#4CAF50") }
        nodes.forEachIndexed { index, node ->
            if (index % 2 == 0) {
                canvas.drawCircle(node.x - 120f, node.y + 40f, 30f, envPaint)
            } else {
                canvas.drawCircle(node.x + 120f, node.y + 40f, 25f, envPaint)
            }
        }
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
                val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#80FFD54F")
                }
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
            val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(node.x - radius * 0.3f, node.y - radius * 0.3f, radius * 1.2f, baseColor, darkColor, Shader.TileMode.CLAMP)
            }

            // Node Circle Face
            canvas.drawCircle(node.x, node.y, radius, nodePaint)
            canvas.drawCircle(node.x, node.y, radius, outlinePaint)

            if (node.isUnlocked) {
                // Level Number
                textPaint.textSize = if (node.isBoss) 34f else 28f
                canvas.drawText("${node.levelNumber}", node.x, node.y + 10f, textPaint)

                // Star Rating under node
                if (node.stars > 0) {
                    val starY = node.y + radius + 22f
                    for (s in 0 until node.stars) {
                        val starX = node.x + (s - (node.stars - 1) / 2f) * 24f
                        canvas.drawCircle(starX, starY, 8f, starPaint)
                        canvas.drawCircle(starX, starY, 8f, outlinePaint)
                    }
                }
            } else {
                val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#455A64") }
                val shacklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#90A4AE")
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                canvas.drawArc(RectF(node.x - 12f, node.y - 14f, node.x + 12f, node.y + 6f), 180f, 180f, false, shacklePaint)
                canvas.drawRoundRect(RectF(node.x - 16f, node.y - 4f, node.x + 16f, node.y + 16f), 4f, 4f, lockPaint)
                canvas.drawRoundRect(RectF(node.x - 16f, node.y - 4f, node.x + 16f, node.y + 16f), 4f, 4f, outlinePaint)
            }

            // Draw Character Marker on Current Level Node
            if (node.isCurrent) {
                val markerBounds = RectF(
                    node.x - 45f,
                    node.y - radius - 80f,
                    node.x + 45f,
                    node.y - radius - 10f
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
