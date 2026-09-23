package com.jumpadventure.game.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object InsetsManager {

    private class OriginalPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)
    private class OriginalMargin(val left: Int, val top: Int, val right: Int, val bottom: Int)

    fun setupEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
        activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
        }

        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    fun applySystemWindowInsets(
        rootView: View,
        topViewsToPad: List<View> = emptyList(),
        bottomViewsToMargin: List<View> = emptyList(),
        bottomViewsToPad: List<View> = emptyList(),
        overlayViewsToPad: List<View> = emptyList()
    ) {
        // Store original paddings and margins before applying insets to prevent accumulating padding
        val topOriginalPaddings = topViewsToPad.associateWith {
            OriginalPadding(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom)
        }
        val bottomOriginalMargins = bottomViewsToMargin.associateWith { v ->
            val params = v.layoutParams as? ViewGroup.MarginLayoutParams
            OriginalMargin(
                params?.leftMargin ?: 0,
                params?.topMargin ?: 0,
                params?.rightMargin ?: 0,
                params?.bottomMargin ?: 0
            )
        }
        val bottomOriginalPaddings = bottomViewsToPad.associateWith {
            OriginalPadding(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom)
        }
        val overlayOriginalPaddings = overlayViewsToPad.associateWith {
            OriginalPadding(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom)
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            topViewsToPad.forEach { v ->
                val orig = topOriginalPaddings[v] ?: return@forEach
                v.setPadding(orig.left, orig.top + systemBars.top, orig.right, orig.bottom)
            }

            bottomViewsToMargin.forEach { v ->
                val orig = bottomOriginalMargins[v] ?: return@forEach
                val params = v.layoutParams as? ViewGroup.MarginLayoutParams ?: return@forEach
                params.bottomMargin = orig.bottom + systemBars.bottom
                v.layoutParams = params
            }

            bottomViewsToPad.forEach { v ->
                val orig = bottomOriginalPaddings[v] ?: return@forEach
                v.setPadding(orig.left, orig.top, orig.right, orig.bottom + systemBars.bottom)
            }

            overlayViewsToPad.forEach { v ->
                val orig = overlayOriginalPaddings[v] ?: return@forEach
                v.setPadding(orig.left, orig.top + systemBars.top, orig.right, orig.bottom + systemBars.bottom)
            }

            insets
        }
    }
}
