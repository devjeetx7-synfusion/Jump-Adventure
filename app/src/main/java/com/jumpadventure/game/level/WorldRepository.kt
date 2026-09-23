package com.jumpadventure.game.level

import com.jumpadventure.game.model.WorldInfo

object WorldRepository {
    val worlds = listOf(
        WorldInfo(
            id = 1,
            name = "Green Forest",
            iconEmoji = "🌳",
            startLevel = 1,
            endLevel = 10,
            skyColorHex = "#81D4FA", // Bright sky blue
            platformColorHex = "#4CAF50", // Vibrant grass green
            dangerColorHex = "#E53935",
            requiredStarsToUnlock = 0
        ),
        WorldInfo(
            id = 2,
            name = "Desert Land",
            iconEmoji = "🏜️",
            startLevel = 11,
            endLevel = 25,
            skyColorHex = "#FFE082", // Warm desert amber
            platformColorHex = "#FB8C00", // Sand orange
            dangerColorHex = "#D84315",
            requiredStarsToUnlock = 15
        ),
        WorldInfo(
            id = 3,
            name = "Snow Mountain",
            iconEmoji = "❄️",
            startLevel = 26,
            endLevel = 50,
            skyColorHex = "#B3E5FC", // Icy crisp cyan
            platformColorHex = "#E0F7FA", // Snow white/light cyan
            dangerColorHex = "#1E88E5",
            requiredStarsToUnlock = 45
        ),
        WorldInfo(
            id = 4,
            name = "Lava Cave",
            iconEmoji = "🌋",
            startLevel = 51,
            endLevel = 100,
            skyColorHex = "#3E2723", // Dark lava cavern
            platformColorHex = "#D84315", // Fiery red-orange
            dangerColorHex = "#FF3D00",
            requiredStarsToUnlock = 90
        ),
        WorldInfo(
            id = 5,
            name = "Sky City",
            iconEmoji = "☁️",
            startLevel = 101,
            endLevel = 250,
            skyColorHex = "#E1F5FE", // Soft cloud sky
            platformColorHex = "#90CAF9", // Pastel sky platform
            dangerColorHex = "#FFB300",
            requiredStarsToUnlock = 180
        ),
        WorldInfo(
            id = 6,
            name = "Ocean World",
            iconEmoji = "🌊",
            startLevel = 251,
            endLevel = 500,
            skyColorHex = "#0288D1", // Deep ocean blue
            platformColorHex = "#00ACC1", // Coral teal
            dangerColorHex = "#FF5722",
            requiredStarsToUnlock = 350
        ),
        WorldInfo(
            id = 7,
            name = "Moon World",
            iconEmoji = "🌙",
            startLevel = 501,
            endLevel = 750,
            skyColorHex = "#1A237E", // Deep space midnight blue
            platformColorHex = "#9E9E9E", // Lunar grey
            dangerColorHex = "#E91E63",
            requiredStarsToUnlock = 600
        ),
        WorldInfo(
            id = 8,
            name = "Cyber City",
            iconEmoji = "🤖",
            startLevel = 751,
            endLevel = 1000,
            skyColorHex = "#212121", // Dark neon cityscape
            platformColorHex = "#00E676", // Neon green
            dangerColorHex = "#FF0055",
            requiredStarsToUnlock = 900
        ),
        WorldInfo(
            id = 9,
            name = "Magic Kingdom",
            iconEmoji = "🏰",
            startLevel = 1001,
            endLevel = 1500,
            skyColorHex = "#4A148C", // Enchanted purple
            platformColorHex = "#AB47BC", // Royal violet
            dangerColorHex = "#FFD54F",
            requiredStarsToUnlock = 1200
        ),
        WorldInfo(
            id = 10,
            name = "Final Kingdom",
            iconEmoji = "👑",
            startLevel = 1501,
            endLevel = 999999,
            skyColorHex = "#1B1B2F", // Regal dark blue-gold
            platformColorHex = "#FFD700", // Golden platforms
            dangerColorHex = "#D50000",
            requiredStarsToUnlock = 1500
        )
    )

    fun getWorldForLevel(level: Int): WorldInfo {
        return worlds.lastOrNull { level >= it.startLevel } ?: worlds.first()
    }
}
