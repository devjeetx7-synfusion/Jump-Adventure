package com.jumpadventure.game.data

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SaveManagerTest {

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = TODO()
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private inner class EditorImpl : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private var clearFlag = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { temp[key!!] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = TODO()
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { temp[key!!] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { temp[key!!] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { temp[key!!] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { temp[key!!] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { temp.remove(key) }
            override fun clear(): SharedPreferences.Editor = apply { clearFlag = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearFlag) map.clear()
                temp.forEach { (k, v) ->
                    if (v == null) map.remove(k) else map[k] = v
                }
            }
        }
    }

    private lateinit var saveManager: SaveManager

    @Before
    fun setUp() {
        saveManager = SaveManager(FakeSharedPreferences())
    }

    @Test
    fun testDefaultValues() {
        assertEquals(1, saveManager.currentLevel)
        assertEquals(1250, saveManager.coins)
        assertEquals(35, saveManager.gems)
        assertEquals("DEFAULT", saveManager.selectedCharacter)
        assertTrue(saveManager.isCharacterUnlocked("DEFAULT"))
        assertFalse(saveManager.isCharacterUnlocked("NINJA"))
    }

    @Test
    fun testUnlockCharacterAndLevelStars() {
        saveManager.coins += 1000
        assertEquals(2250, saveManager.coins)

        saveManager.unlockCharacter("NINJA")
        assertTrue(saveManager.isCharacterUnlocked("NINJA"))

        saveManager.setLevelStars(1, 3)
        assertEquals(3, saveManager.getLevelStars(1))

        saveManager.setLevelStars(1, 2)
        assertEquals(3, saveManager.getLevelStars(1))
    }
}
