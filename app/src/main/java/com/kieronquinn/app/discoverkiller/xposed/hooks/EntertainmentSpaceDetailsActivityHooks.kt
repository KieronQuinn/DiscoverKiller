package com.kieronquinn.app.discoverkiller.xposed.hooks

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import com.kieronquinn.app.discoverkiller.xposed.BaseXposedHooks

/**
 *  Hooks Entertainment Space's `DetailsActivity`, making it full width and height to fit better
 *  on phones.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class EntertainmentSpaceDetailsActivityHooks(
    classLoader: ClassLoader
): BaseXposedHooks(classLoader) {

    override val clazz = "com.google.android.apps.mediahome.launcher.details.ui.DetailsActivity"

    fun onCreate(savedInstanceState: Bundle?) = MethodHook(afterHookedMethod = {
        val activity = thisObject as Activity
        val containerId = activity.resources.getIdentifier(
            "details_activity_container", "id", activity.packageName
        )
        val margin = activity.resources.getIdentifier(
            "details_activity_margin_side", "dimen", activity.packageName
        )
        val displayWidth = activity.resources.displayMetrics.widthPixels
        activity.findViewById<FrameLayout>(containerId).run {
            translationX = -(resources.getDimension(margin))
            updateLayoutParams<FrameLayout.LayoutParams> {
                updateMargins(0, 0, 0, 0)
                width = displayWidth
            }
        }
        activity.window.decorView.updatePadding(0, 0, 0, 0)
        MethodResult.Skip<Unit>()
    })

}