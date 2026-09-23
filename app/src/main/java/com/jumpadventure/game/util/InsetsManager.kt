package com.jumpadventure.game.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object InsetsManager {
    private data class OriginalMargin(val left: Int, val top: Int, val right: Int, val bottom: Int)
    private data class OriginalPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

    fun setupEdgeToEdge(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = Color.TRANSPARENT
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
        val originalTopMargins = topViewsToPad.associateWith { view ->
            val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            OriginalMargin(lp?.leftMargin ?: 0, lp?.topMargin ?: 0, lp?.rightMargin ?: 0, lp?.bottomMargin ?: 0)
        }
        val originalBottomMargins = bottomViewsToMargin.associateWith { view ->
            val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
            OriginalMargin(lp?.leftMargin ?: 0, lp?.topMargin ?: 0, lp?.rightMargin ?: 0, lp?.bottomMargin ?: 0)
        }
        val originalBottomPaddings = bottomViewsToPad.associateWith {
            OriginalPadding(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom)
        }
        val originalOverlayPaddings = overlayViewsToPad.associateWith {
            OriginalPadding(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom)
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val topInset = maxOf(bars.top, cutout.top)
            val bottomInset = maxOf(bars.bottom, cutout.bottom)

            // Move top bars below the status/cutout area instead of injecting
            // the inset into their internal padding. This keeps the whole HUD visible.
            topViewsToPad.forEach { view ->
                val original = originalTopMargins[view] ?: return@forEach
                val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return@forEach
                lp.leftMargin = original.left
                lp.topMargin = original.top + topInset
                lp.rightMargin = original.right
                lp.bottomMargin = original.bottom
                view.layoutParams = lp
            }

            bottomViewsToMargin.forEach { view ->
                val original = originalBottomMargins[view] ?: return@forEach
                val lp = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return@forEach
                lp.leftMargin = original.left
                lp.topMargin = original.top
                lp.rightMargin = original.right
                lp.bottomMargin = original.bottom + bottomInset
                view.layoutParams = lp
            }

            bottomViewsToPad.forEach { view ->
                val original = originalBottomPaddings[view] ?: return@forEach
                view.setPadding(original.left, original.top, original.right, original.bottom + bottomInset)
            }

            overlayViewsToPad.forEach { view ->
                val original = originalOverlayPaddings[view] ?: return@forEach
                view.setPadding(original.left, original.top + topInset, original.right, original.bottom + bottomInset)
            }

            insets
        }

        ViewCompat.requestApplyInsets(rootView)
    }
}
