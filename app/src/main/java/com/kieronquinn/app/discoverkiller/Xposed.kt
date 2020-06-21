package com.kieronquinn.app.discoverkiller

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.kieronquinn.app.discoverkiller.settings.holders.DiscoverBehaviour
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage


/*
    Because the Google app is obfuscated, this class contains logic to find the classes we need without relying on obfuscated class names. Entry point is an unobfuscated class name.
 */

class Xposed : IXposedHookLoadPackage {

    companion object {
        const val TAG = "DiscoverKiller"
        const val INTENT_KEY_FROM_DISCOVER_KILLER = "from_discover_killer"
        const val VIEW_TAG_DISCOVER_KILLER = "discoverkiller"
        val COLOR_LIGHT = Color.parseColor("#F8F9FA")
        val COLOR_DARK = Color.parseColor("#1F1F20")
    }

    private var hasReplacedFeed = false

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if(lpparam.packageName == BuildConfig.APPLICATION_ID){
            //Self hooking for EdXposed check
            XposedHelpers.findAndHookMethod("com.kieronquinn.app.discoverkiller.utils.XposedUtils", lpparam.classLoader,"isEdXposedModuleActive", object: XC_MethodReplacement(){
                override fun replaceHookedMethod(param: MethodHookParam?): Any {
                    //Just return true
                    param?.result = true
                    return true
                }
            })
        }

        if (lpparam.packageName == "com.google.android.googlequicksearchbox") {
            Log.d(TAG, "HOOKED ${lpparam.packageName}")
            attachSlidrListener(lpparam, "com.google.android.apps.gsa.staticplugins.opa.OpaActivity")
            /*
                Method to find class:
                DrawerOverlayService -> only field -> method with signature's return class:
                    Configuration arg9, int arg10, int arg11, boolean arg12, Bundle arg13, String arg14
             */
            val drawerOverlayService = XposedHelpers.findClass(
                "com.google.android.apps.gsa.nowoverlayservice.DrawerOverlayService",
                lpparam.classLoader
            )
            //val onlyField = drawerOverlayService.declaredFields.first().type
            //TODO find a way of finding this when obfuscated again ^
            val onlyField = XposedHelpers.findClass(
                "com.google.android.apps.gsa.nowoverlayservice.s",
                lpparam.classLoader
            )

            val returnClassMethod = onlyField.declaredMethods.first { it.parameterCount == 6 }

            val returnClass = returnClassMethod.returnType

            val fields = returnClass.declaredFields

            var isBehaviourDisabled = false
            //This method shouldn't change name (in theory)
            XposedHelpers.findAndHookMethod(
                returnClass,
                "a",
                Int::class.java,
                Int::class.java,
                Long::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        /*
                            Class contains two views as fields, a sliding panel and a framelayout (container) - in that order. They can both be cast to FrameLayout, which is how we'll search for them
                            Also contains a field which can be cast to an Activity, but is not a full activity (so can't run startActivity or host a child activity)
                         */
                        val state = param.args[0] as Int
                        val position = param.args[1] as Int

                        if (state == 1 && position > 250 && isBehaviourDisabled) {
                            //Reset behaviour to check again next time
                            isBehaviourDisabled = false
                        }
                        val frameLayouts = fields.filter {
                            it.isAccessible = true
                            it.get(param.thisObject) is FrameLayout
                        }
                        val activities = fields.filter {
                            it.isAccessible = true
                            it.get(param.thisObject) is Activity
                        }
                        if (frameLayouts.isNotEmpty()) {
                            val frameLayout = frameLayouts[1].get(param.thisObject) as FrameLayout
                            val slidingPanel = frameLayouts[0].get(param.thisObject) as FrameLayout
                            val discoverBehaviour = DiscoverBehaviour.valueOf(getSharedStringPref(frameLayout.context, KEY_DISCOVER_BEHAVIOUR, DiscoverBehaviour.UPDATES.name) ?: DiscoverBehaviour.UPDATES.name)
                            if(discoverBehaviour == DiscoverBehaviour.NONE){
                                //Do nothing
                                isBehaviourDisabled = true
                                return
                            }
                            val activity = activities.first().get(param.thisObject) as Activity
                            val isNightMode =
                                frameLayout.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                            val color = if (isNightMode) COLOR_DARK else COLOR_LIGHT
                            if (!hasReplacedFeed) {
                                hasReplacedFeed = true
                                if (frameLayout.findViewWithTag<View>(VIEW_TAG_DISCOVER_KILLER) == null) {
                                    val childFrameLayout = FrameLayout(activity)
                                    childFrameLayout.layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    childFrameLayout.tag = VIEW_TAG_DISCOVER_KILLER
                                    childFrameLayout.setBackgroundColor(color)
                                    frameLayout.addView(childFrameLayout)
                                }
                            }

                            if (state == 1 && position > 250) {
                                //Allow the slide to complete before we start the activity
                                runAfter(250) {
                                    performLaunch(activity, frameLayout.context)
                                    //Allow the activity to start before we start sliding the panel away
                                    runAfter(500) {
                                        //This method shouldn't change name (in theory)
                                        XposedHelpers.callMethod(slidingPanel, "b", 100)
                                        val blockingView = frameLayout.findViewWithTag<View>(
                                            VIEW_TAG_DISCOVER_KILLER
                                        )
                                        frameLayout.removeView(blockingView)
                                        hasReplacedFeed = false
                                    }
                                }
                            }
                        }
                    }
                })
        }
    }

    private fun performLaunch(activity: Activity, context: Context) {
        val behaviour = getSharedStringPref(context, KEY_DISCOVER_BEHAVIOUR, DiscoverBehaviour.UPDATES.name)
        val discoverBehaviour = DiscoverBehaviour.valueOf(behaviour ?: DiscoverBehaviour.UPDATES.name)
        val customApp = getSharedStringPref(context, KEY_CUSTOM_APP, "null")
        val intent = when (discoverBehaviour) {
            DiscoverBehaviour.UPDATES -> {
                Intent().setComponent(
                    ComponentName(
                        "com.google.android.googlequicksearchbox",
                        "com.google.android.apps.gsa.staticplugins.opa.EnterOpaActivity" //"com.google.android.apps.gsa.staticplugins.opa.ZeroStateActivity"
                    )
                )
            }
            DiscoverBehaviour.CUSTOM_APP -> {
                try {
                    context.packageManager?.getLaunchIntentForPackage(customApp ?: "")
                }catch (e: PackageManager.NameNotFoundException){
                    null
                }
            }
            DiscoverBehaviour.NONE -> {
                //Shouldn't ever get here
                null
            }
        }

        intent?.let {
            intent.putExtra(INTENT_KEY_FROM_DISCOVER_KILLER, true)
            intent.putExtra("opa_start_zero_state", true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            context.startActivity(intent)
            activity.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } ?: run {
            Toast.makeText(context, context.getAppString("no_app_selected"), Toast.LENGTH_LONG).show()
        }

    }

    private fun Context.getAppString(id: String): String {
        val appContext = createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY)
        return appContext.getString(getString(appContext, id))
    }

    @StringRes
    private fun getString(context: Context, id: String): Int {
        return context.resources.getIdentifier(id, "string", context.packageName)
    }

    private fun attachSlidrListener(lpparam: XC_LoadPackage.LoadPackageParam, launchComponent: String){
        Log.d(TAG, "Hooked ${lpparam.packageName}")
        XposedHelpers.findAndHookMethod(
                launchComponent,
                lpparam.classLoader,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        val activity = param.thisObject as Activity
                        if (activity.intent.getBooleanExtra(
                                        INTENT_KEY_FROM_DISCOVER_KILLER,
                                        false
                                )
                        ) {
                            activity.overridePendingTransition(
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out
                            )
                            val shouldAttachSlidr = getSharedBoolPref(activity, KEY_SWIPE_CLOSE, false)
                            if(shouldAttachSlidr) {
                                val isNightMode =
                                        activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                                val color =
                                        if (isNightMode) COLOR_DARK else COLOR_LIGHT
                                val config = SlidrConfig.Builder()
                                        .position(SlidrPosition.HORIZONTAL)
                                        .sensitivity(0.5f)
                                        .scrimColor(color)
                                        .scrimStartAlpha(0.8f)
                                        .scrimEndAlpha(1f)
                                        .build()
                                activity.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                                Slidr.attach(activity, config)
                                Log.d(TAG, "Slidr attached")
                            }
                        }
                    }
                })
    }


}