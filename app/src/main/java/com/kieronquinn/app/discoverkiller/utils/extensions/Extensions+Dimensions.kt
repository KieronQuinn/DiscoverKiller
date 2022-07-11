package com.kieronquinn.app.discoverkiller.utils.extensions

import android.content.res.Resources

val Float.toPx get() = this * Resources.getSystem().displayMetrics.density

val Float.toDp get() = this / Resources.getSystem().displayMetrics.density