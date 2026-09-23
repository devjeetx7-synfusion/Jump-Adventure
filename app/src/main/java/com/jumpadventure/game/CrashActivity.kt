package com.jumpadventure.game

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jumpadventure.game.graphics.GamePrimaryButton

class CrashActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EXCEPTION = "exception"
        const val EXTRA_STACKTRACE = "stacktrace"
        const val EXTRA_THREAD = "thread"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.jumpadventure.game.util.InsetsManager.setupEdgeToEdge(this)

        val exception = intent.getStringExtra(EXTRA_EXCEPTION).orEmpty()
        val stack = intent.getStringExtra(EXTRA_STACKTRACE).orEmpty()
        val thread = intent.getStringExtra(EXTRA_THREAD).orEmpty()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#EAF7FF"))
        }

        val title = TextView(this).apply {
            text = "GAME CRASH"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#9B1C1C"))
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val message = TextView(this).apply {
            text = "Something went wrong. Copy the error details and share them for debugging."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1F3045"))
            setPadding(0, 12, 0, 12)
        }
        root.addView(message, LinearLayout.LayoutParams(-1, -2))

        val details = TextView(this).apply {
            text = "Exception: $exception\nThread: $thread\n\n$stack"
            textSize = 12f
            setTextColor(Color.parseColor("#243447"))
            setBackgroundColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
        }
        val scroll = ScrollView(this).apply { addView(details) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, 8, 0, 12) })

        val density = resources.displayMetrics.density
        val btnHeight = (56 * density).toInt()

        val copyButton = GamePrimaryButton(this).apply {
            mainText = "COPY ERROR"
            subText = ""
            variant = GamePrimaryButton.Variant.BLUE
        }
        copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Jump Adventure Crash", details.text.toString()))
            copyButton.mainText = "COPIED"
            copyButton.postDelayed({ copyButton.mainText = "COPY ERROR" }, 1200L)
        }
        root.addView(copyButton, LinearLayout.LayoutParams(-1, btnHeight).apply { setMargins(0, 6, 0, 8) })

        val restart = GamePrimaryButton(this).apply {
            mainText = "RESTART"
            subText = ""
            variant = GamePrimaryButton.Variant.GREEN
        }
        restart.setOnClickListener {
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            finish()
        }
        root.addView(restart, LinearLayout.LayoutParams(-1, btnHeight).apply { setMargins(0, 4, 0, 8) })

        setContentView(root)
    }
}