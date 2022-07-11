package com.kieronquinn.app.discoverkiller.utils.extensions

import androidx.core.graphics.ColorUtils

fun Int.toHexString(): String {
    return "#" + Integer.toHexString(this)
}

fun Int.isColorDark(): Boolean {
    return ColorUtils.calculateLuminance(this) < 0.5
}