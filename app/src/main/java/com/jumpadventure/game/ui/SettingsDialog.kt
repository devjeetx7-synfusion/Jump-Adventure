package com.jumpadventure.game.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import com.jumpadventure.game.R
import com.jumpadventure.game.data.SaveManager

class SettingsDialog(
    context: Context,
    private val saveManager: SaveManager,
    private val onResetConfirmed: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_settings)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        val switchSound = findViewById<Switch>(R.id.switchSound)
        val switchMusic = findViewById<Switch>(R.id.switchMusic)
        val switchVibration = findViewById<Switch>(R.id.switchVibration)
        val btnLanguage = findViewById<Button>(R.id.btnLanguageToggle)
        val btnResetProgress = findViewById<Button>(R.id.btnResetProgress)
        val btnClose = findViewById<Button>(R.id.btnCloseSettings)

        switchSound.isChecked = saveManager.soundEnabled
        switchMusic.isChecked = saveManager.musicEnabled
        switchVibration.isChecked = saveManager.vibrationEnabled
        btnLanguage.text = saveManager.language

        switchSound.setOnCheckedChangeListener { _, isChecked -> saveManager.soundEnabled = isChecked }
        switchMusic.setOnCheckedChangeListener { _, isChecked -> saveManager.musicEnabled = isChecked }
        switchVibration.setOnCheckedChangeListener { _, isChecked -> saveManager.vibrationEnabled = isChecked }

        btnLanguage.setOnClickListener {
            saveManager.language = if (saveManager.language == "English") "Hindi" else "English"
            btnLanguage.text = saveManager.language
        }

        btnResetProgress.setOnClickListener {
            saveManager.resetAllProgress()
            onResetConfirmed()
            dismiss()
        }

        btnClose.setOnClickListener { dismiss() }
    }
}
