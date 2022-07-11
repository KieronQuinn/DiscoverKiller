package com.kieronquinn.app.discoverkiller.xposed.hooks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.kieronquinn.app.discoverkiller.utils.extensions.toPx
import com.kieronquinn.app.discoverkiller.xposed.BaseXposedHooks

/**
 *  Hooks [LayoutInflater.inflate] in Entertainment Space, neutering the No Connection view which
 *  is bugged, and adding padding to the overlay tabs
 */
@Suppress("unused", "UNUSED_PARAMETER")
class EntertainmentSpaceLayoutHooks(
    classLoader: ClassLoader,
    private val context: Context
): BaseXposedHooks(classLoader) {

    override val clazz = LayoutInflater::class.java.name

    private val tabLayoutId = context.resources.getIdentifier(
        "stream_tab_layout", "id", context.packageName
    )

    fun inflate(resource: Int, parent: ViewGroup?, attach: Boolean) = MethodHook(afterHookedMethod = {
        val resourceName = context.resources.getResourceEntryName(resource)
        if(resourceName == "no_connection_view"){
            val view = result as View
            view.updateLayoutParams<ViewGroup.LayoutParams> { height = 0 }
            view.visibility = View.GONE
        }
        if(resourceName == "stream_view") {
            val view = result as View
            view.findViewById<HorizontalScrollView>(tabLayoutId).run {
                val padding = 16f.toPx.toInt()
                overScrollMode = View.OVER_SCROLL_NEVER
                updatePadding(left = padding, right = padding)
                isHorizontalFadingEdgeEnabled = true
                setFadingEdgeLength(padding)
            }
        }
        MethodResult.Skip<View>()
    })

}