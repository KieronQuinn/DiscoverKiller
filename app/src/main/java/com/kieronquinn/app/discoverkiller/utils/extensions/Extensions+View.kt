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
import com.kieronquinn.app.discoverkiller.utils.OverlayContext


fun View.onApplyInsets(doOnApply: (View, WindowInsetsCompat) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this){ view, insets ->
        doOnApply.invoke(view, insets)
        insets
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

val View.isInOverlay: Boolean
    get() {
        var context = context
        while (context is ContextWrapper) {
            Log.d("GAS", "looking for context, found ${context.javaClass.simpleName}")
            if (context is OverlayContext) {
                return true
            }
            context = context.baseContext
        }
        return false
    }