package com.kieronquinn.app.discoverkiller.utils.extensions

import android.widget.AbsListView
import android.widget.ListView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun ListView.isScrolled(): Flow<Boolean> = callbackFlow {
    setOnScrollChangeListener { _, _, _, _, _ ->
        trySend(computeVerticalScrollOffset() > 0)
    }
    trySend(computeVerticalScrollOffset() > 0)
    awaitClose {
        setOnScrollChangeListener(null)
    }
}.distinctUntilChanged()

private val ListView.computeVerticalScrollOffset by lazy {
    AbsListView::class.java.getDeclaredMethod("computeVerticalScrollOffset").apply {
        isAccessible = true
    }
}

fun ListView.computeVerticalScrollOffset(): Int {
    return computeVerticalScrollOffset.invoke(this) as Int
}