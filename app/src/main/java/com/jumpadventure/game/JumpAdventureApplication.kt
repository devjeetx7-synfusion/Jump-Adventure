package com.jumpadventure.game

import android.app.Application
import android.content.Intent

class JumpAdventureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(CrashActivity.EXTRA_THREAD, thread.name)
                    putExtra(CrashActivity.EXTRA_STACKTRACE, throwable.stackTraceToString())
                    putExtra(CrashActivity.EXTRA_EXCEPTION, (throwable::class.java.name + ": " + (throwable.message ?: "")).trim())
                }
                startActivity(intent)
            } catch (_: Throwable) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}