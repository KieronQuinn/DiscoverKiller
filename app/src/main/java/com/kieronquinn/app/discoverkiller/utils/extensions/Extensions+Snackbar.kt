package com.kieronquinn.app.discoverkiller.utils.extensions

import android.graphics.Typeface
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

fun Snackbar.setTypeface(typeface: Typeface?): Snackbar {
    val tv = view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
    val actionTv = view.findViewById(com.google.android.material.R.id.snackbar_action) as TextView
    tv.typeface = typeface
    actionTv.typeface = typeface
    return this
}