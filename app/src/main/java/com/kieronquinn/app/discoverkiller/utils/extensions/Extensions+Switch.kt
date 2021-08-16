package com.kieronquinn.app.discoverkiller.utils.extensions

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SwitchCompat
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.toArgb
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor

/**
 *  Copied from MonetSwitch, this uses lighter colors than the regular applyMonet()
 */
fun SwitchCompat.applyMonetLight() = with(MonetCompat.getInstance()) {
    val uncheckedTrackColor = getMonetColors().accent1[600]?.toArgb() ?: getAccentColor(context, false)
    val checkedTrackColor = getMonetColors().accent1[300]?.toArgb() ?: uncheckedTrackColor
    val checkedThumbColor = getPrimaryColor(context, false)
    val uncheckedThumbColor = getSecondaryColor(context, false)
    setTint(checkedTrackColor, uncheckedTrackColor, uncheckedThumbColor, checkedThumbColor)
}

private fun SwitchCompat.setTint(@ColorInt checkedTrackColor: Int, @ColorInt unCheckedTrackColor: Int, @ColorInt uncheckedThumbColor: Int, @ColorInt checkedThumbColor: Int){
    trackTintList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(checkedTrackColor, unCheckedTrackColor)
    )
    val bgTintList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(checkedThumbColor, uncheckedThumbColor)
    )
    thumbTintList = bgTintList
    backgroundTintList = bgTintList
    backgroundTintMode = PorterDuff.Mode.SRC_ATOP
    overrideRippleColor(colorStateList = bgTintList)
}