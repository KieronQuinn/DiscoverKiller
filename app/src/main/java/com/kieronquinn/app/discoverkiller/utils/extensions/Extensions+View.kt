package com.kieronquinn.app.discoverkiller.utils.extensions

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.discoverkiller.R
import android.content.ContextWrapper

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.Window
import androidx.core.view.doOnPreDraw
import com.kieronquinn.app.discoverkiller.components.xposed.apps.GoogleApp
import com.kieronquinn.app.discoverkiller.utils.OverlayContext

/**
 *  Provide a block to run when window insets change. [reApplyNow] = `true` will trigger the block
 *  immediately with root window insets, if they exist.
 */
fun View.onApplyInsets(reApplyNow: Boolean = false, doOnApply: (View, WindowInsetsCompat) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this){ view, insets ->
        doOnApply.invoke(view, insets)
        insets
    }
    if(reApplyNow){
        ViewCompat.getRootWindowInsets(this)?.let {
            doOnApply.invoke(this, it)
        }
    }
}

fun View.slideOut(callback: () -> Unit): Animation {
    AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom).apply {
        onEnd {
            isVisible = false
            callback.invoke()
        }
    }.also {
        startAnimation(it)
        return it
    }
}

fun View.slideIn(callback: () -> Unit): Animation {
    isVisible = true
    AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom).apply {
        onEnd {
            callback.invoke()
        }
    }.also {
        startAnimation(it)
        return it
    }
}

fun Animation.onEnd(callback: () -> Unit){
    setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            callback.invoke()
        }
    })
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

/**
 *  Runs a given [block] if the view ID is in [idStrings] and it is attached to the
 *  Discover Killer activity.
 */
fun View.runAfterPostIfIdMatches(flag: () -> Boolean, vararg idStrings: String, block: (View) -> Unit){
    val ids = idStrings.map {
        context.resources.getIdentifier(it, "id", context.packageName)
    }.filter { it != 0x0 }
    post {
        if(!flag()) return@post
        if(!isAttachedToWindow) return@post
        if(!ids.contains(id)) return@post
        block.invoke(this)
    }
}

val View.activity: Activity?
    get() {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }