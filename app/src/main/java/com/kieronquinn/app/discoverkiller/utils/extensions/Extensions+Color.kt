package com.kieronquinn.app.discoverkiller.utils.extensions

fun Int.toHexString(): String {
    return "#" + Integer.toHexString(this)
}