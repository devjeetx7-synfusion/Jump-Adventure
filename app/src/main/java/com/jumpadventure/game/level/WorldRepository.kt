package com.jumpadventure.game.level

import com.jumpadventure.game.model.WorldInfo

object WorldRepository {

    private val themeTemplates = listOf(
        Triple("Green Forest", "🌳", Pair("#81D4FA", "#4CAF50")),
        Triple("Desert Land", "🏜️", Pair("#FFE082", "#FB8C00")),
        Triple("Snow Mountain", "❄️", Pair("#B3E5FC", "#E0F7FA")),
        Triple("Lava Cave", "🌋", Pair("#3E2723", "#D84315")),
        Triple("Sky City", "☁️", Pair("#E1F5FE", "#90CAF9")),
        Triple("Ocean World", "🌊", Pair("#0288D1", "#00ACC1")),
        Triple("Moon World", "🌙", Pair("#1A237E", "#9E9E9E")),
        Triple("Cyber City", "🤖", Pair("#212121", "#00E676")),
        Triple("Magic Kingdom", "🏰", Pair("#4A148C", "#AB47BC")),
        Triple("Galaxy Realm", "🌌", Pair("#1B1B2F", "#FFD700")),
        Triple("Cloud Kingdom", "☁️", Pair("#E1F5FE", "#FFB300")),
        Triple("Volcano Cavern", "🌋", Pair("#260F17", "#FF3D00")),
        Triple("Crystal Cavern", "💎", Pair("#0F172A", "#00E5FF")),
        Triple("Ancient Ruins", "🏛️", Pair("#3E2723", "#8D6E63")),
        Triple("Dark Forest", "🌲", Pair("#0B1426", "#2E7D32")),
        Triple("Neon Metropolis", "🏙️", Pair("#1A0F2E", "#E040FB"))
    )

    fun getWorldForLevel(level: Int): WorldInfo {
        val safeLevel = maxOf(1, level)
        val worldId = ((safeLevel - 1) / 25) + 1
        val startLvl = (worldId - 1) * 25 + 1
        val endLvl = worldId * 25

        val themeIdx = (worldId - 1) % themeTemplates.size
        val (themeName, emoji, colors) = themeTemplates[themeIdx]
        val (skyColor, platColor) = colors

        val cycle = (worldId - 1) / themeTemplates.size
        val displayName = if (cycle > 0) "$themeName ${toRoman(cycle + 1)}" else themeName

        val reqStars = (worldId - 1) * 15

        return WorldInfo(
            id = worldId,
            name = displayName,
            iconEmoji = emoji,
            startLevel = startLvl,
            endLevel = endLvl,
            skyColorHex = skyColor,
            platformColorHex = platColor,
            dangerColorHex = "#E53935",
            requiredStarsToUnlock = reqStars
        )
    }

    val worlds: List<WorldInfo>
        get() = (1..10).map { getWorldForLevel((it - 1) * 25 + 1) }

    fun getWorldBackgroundRes(worldId: Int): Int {
        val safeId = maxOf(1, worldId)
        return when ((safeId - 1) % 4) {
            0 -> com.jumpadventure.game.R.drawable.green_forest
            1 -> com.jumpadventure.game.R.drawable.desert_land
            2 -> com.jumpadventure.game.R.drawable.snow_mountain
            else -> com.jumpadventure.game.R.drawable.lava_cave
        }
    }

    private fun toRoman(number: Int): String {
        return when (number) {
            1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"
            6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"; 10 -> "X"
            else -> "$number"
        }
    }
}
