package com.jumpadventure.game.engine

enum class WorldTheme(
    val worldId: Int,
    val worldName: String,
    val skyTopColor: Int,
    val skyBottomColor: Int,
    val platformColor: Int,
    val platformTopColor: Int,
    val obstacleColor: Int,
    val enemyColor: Int,
    val bgDecorType: String,
    val unlockReqLevel: Int
) {
    GREEN_FOREST(1, "Green Forest", 0xFF4FC3F7.toInt(), 0xFF81C784.toInt(), 0xFF5D4037.toInt(), 0xFF4CAF50.toInt(), 0xFFD32F2F.toInt(), 0xFFE65100.toInt(), "TREES", 1),
    DESERT_LAND(2, "Desert Land", 0xFFFFD54F.toInt(), 0xFFFFB74D.toInt(), 0xFF8D6E63.toInt(), 0xFFF57C00.toInt(), 0xFFC62828.toInt(), 0xFFBF360C.toInt(), "PYRAMIDS", 10),
    SNOW_MOUNTAIN(3, "Snow Mountain", 0xFFB2EBF2.toInt(), 0xFFE0F7FA.toInt(), 0xFF546E7A.toInt(), 0xFFFFFFFF.toInt(), 0xFFB0BEC5.toInt(), 0xFF1565C0.toInt(), "SNOW_TREES", 25),
    LAVA_CAVE(4, "Lava Cave", 0xFF3E2723.toInt(), 0xFFBF360C.toInt(), 0xFF212121.toInt(), 0xFFFF3D00.toInt(), 0xFFFF1744.toInt(), 0xFFDD2C00.toInt(), "LAVA_FALLS", 50),
    SKY_CITY(5, "Sky City", 0xFF80D8FF.toInt(), 0xFFEA80FC.toInt(), 0xFF78909C.toInt(), 0xFFE0E0E0.toInt(), 0xFFFF5252.toInt(), 0xFF6A1B9A.toInt(), "CLOUDS", 100),
    OCEAN_WORLD(6, "Ocean World", 0xFF0288D1.toInt(), 0xFF0097A7.toInt(), 0xFF004D40.toInt(), 0xFF26A69A.toInt(), 0xFFD81B60.toInt(), 0xFF00838F.toInt(), "CORAL", 200),
    MOON_WORLD(7, "Moon World", 0xFF1A237E.toInt(), 0xFF311B92.toInt(), 0xFF37474F.toInt(), 0xFF78909C.toInt(), 0xFFFFD600.toInt(), 0xFF4A148C.toInt(), "CRATERS", 350),
    CYBER_CITY(8, "Cyber City", 0xFF12005E.toInt(), 0xFF000000.toInt(), 0xFF212121.toInt(), 0xFF00E676.toInt(), 0xFFFF0055.toInt(), 0xFF651FFF.toInt(), "NEON_GRID", 500),
    MAGIC_KINGDOM(9, "Magic Kingdom", 0xFF4A148C.toInt(), 0xFF880E4F.toInt(), 0xFF4E342E.toInt(), 0xFFFF4081.toInt(), 0xFFFFAB00.toInt(), 0xFFAD1457.toInt(), "CASTLE", 750),
    FINAL_KINGDOM(10, "Final Kingdom", 0xFF212121.toInt(), 0xFF880E4F.toInt(), 0xFF000000.toInt(), 0xFFFFD700.toInt(), 0xFFFF1744.toInt(), 0xFFD50000.toInt(), "DRAGON_TOWER", 1000);

    companion object {
        fun getForLevel(level: Int): WorldTheme {
            return when {
                level <= 10 -> GREEN_FOREST
                level <= 25 -> DESERT_LAND
                level <= 50 -> SNOW_MOUNTAIN
                level <= 100 -> LAVA_CAVE
                level <= 200 -> SKY_CITY
                level <= 350 -> OCEAN_WORLD
                level <= 500 -> MOON_WORLD
                level <= 750 -> CYBER_CITY
                level <= 999 -> MAGIC_KINGDOM
                else -> FINAL_KINGDOM
            }
        }

        fun getByWorldId(id: Int): WorldTheme {
            return values().firstOrNull { it.worldId == id } ?: GREEN_FOREST
        }
    }
}
