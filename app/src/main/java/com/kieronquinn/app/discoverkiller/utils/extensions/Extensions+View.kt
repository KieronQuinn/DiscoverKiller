package com.kieronquinn.app.discoverkiller.utils.extensions

import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val TAP_DEBOUNCE = 250L

suspend fun View.awaitPost() = suspendCancellableCoroutine<View> {
    post {
        if(isAttachedToWindow){
            it.resume(this)
        }else{
            it.cancel()
        }
    }
}

fun View.onClicked() = callbackFlow {
    setOnClickListener {
        trySend(it)
    }
    awaitClose {
        setOnClickListener(null)
    }
}.debounce(TAP_DEBOUNCE)

fun View.addRipple() = with(TypedValue()) {
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

fun View.removeRipple() {
    setBackgroundResource(0)
}

fun View.delayPreDrawUntilFlow(flow: Flow<Boolean>, lifecycle: Lifecycle) {
    val listener = ViewTreeObserver.OnPreDrawListener {
        false
    }
    val removeListener = {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
    lifecycle.runOnDestroy {
        removeListener()
    }
    viewTreeObserver.addOnPreDrawListener(listener)
    lifecycle.coroutineScope.launchWhenResumed {
        flow.collect {
            removeListener()
        }
    }
}


fun View.measureSize(windowManager: WindowManager): Pair<Int, Int> {
    val display = windowManager.defaultDisplay
    val height = display.height
    val width = display.width
    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST)
    measure(widthSpec, heightSpec)
    return Pair(measuredWidth, measuredHeight)
}

/**
 *  The standard [Window.setNavigationBarColor] and [Window.setStatusBarColor] don't work for
 *  this embedded window so we make them invisible manually
 */
fun View.removeStatusNavBackgroundOnPreDraw() = apply {
    doOnPreDraw {
        val statusBarBackground = it.findViewById<View>(android.R.id.statusBarBackground)
        statusBarBackground?.run {
            visibility = View.INVISIBLE
            alpha = 0f
        }
        val navigationBarBackground = it.findViewById<View>(android.R.id.navigationBarBackground)
        navigationBarBackground?.run {
            visibility = View.INVISIBLE
            alpha = 0f
        }
    }
}