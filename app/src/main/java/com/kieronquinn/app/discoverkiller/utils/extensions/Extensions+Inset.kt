package com.kieronquinn.app.discoverkiller.utils.extensions

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import com.kieronquinn.app.discoverkiller.R

fun View.onApplyInsets(block: (view: View, insets: WindowInsetsCompat) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        block(view, insets)
        insets
    }
}

fun View.applyBottomPadding(extraPadding: Float = 0f) {
    updatePadding(bottom = extraPadding.toInt())
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.ime()).bottom + context.getLegacyWorkaroundNavBarHeight()
        updatePadding(bottom = bottomInsets + extraPadding.toInt())
    }
}

fun View.applyBottomMargins(extraPadding: Float = 0f) {
    updatePadding(bottom = extraPadding.toInt())
    onApplyInsets { _, insets ->
        val bottomInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                or WindowInsetsCompat.Type.ime()).bottom + context.getLegacyWorkaroundNavBarHeight()
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(bottom = bottomInsets + extraPadding.toInt())
        }
    }
}

fun Context.getLegacyWorkaroundNavBarHeight(): Int {
    val resourceId: Int = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        resources.getDimensionPixelSize(resourceId)
    } else 0
}