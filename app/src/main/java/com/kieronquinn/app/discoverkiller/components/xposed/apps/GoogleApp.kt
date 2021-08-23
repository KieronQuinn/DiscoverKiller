package com.kieronquinn.app.discoverkiller.components.xposed.apps

import android.app.Activity
import android.app.Service
import android.content.*
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Bundle
import android.os.IBinder
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.kieronquinn.app.discoverkiller.components.intentforwarder.IntentForwarder
import com.kieronquinn.app.discoverkiller.components.settings.RemoteSettings
import com.kieronquinn.app.discoverkiller.model.RemoteSettingsHolder
import com.kieronquinn.app.discoverkiller.ui.controllers.DiscoverKillerOverlayController
import com.kieronquinn.app.discoverkiller.ui.screens.overlay.snapshot.SnapshotOverlay
import com.kieronquinn.app.discoverkiller.utils.extensions.*
import com.kieronquinn.monetcompat.core.MonetCompat
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.FileDescriptor
import java.io.PrintWriter
import java.lang.reflect.Field
import kotlin.system.exitProcess


/**
 *  Hooks for the Google App / googlequicksearchbox
 *
 *  Now with no obfuscated names!
 */
class GoogleApp: XposedApp(PACKAGE_NAME) {

    companion object {
        const val PACKAGE_NAME = "com.google.android.googlequicksearchbox"

        //Indicates an activity has been started from DiscoverKiller
        const val INTENT_KEY_FROM_DISCOVER_KILLER = "from_discover_killer"

        const val ACTION_RELOAD_SNAPSHOT = "$PACKAGE_NAME.RELOAD_SNAPSHOT"

        //ZeroState = Snapshot, EnterOpa is required to launch it properly
        private const val ZERO_STATE_CLASS_NAME = "com.google.android.apps.gsa.staticplugins.opa.ZeroStateActivity"
        private const val ENTER_OPA_CLASS_NAME = "com.google.android.apps.gsa.staticplugins.opa.EnterOpaActivity"

        // Google app dark text colors (primary/secondary)
        private val QUANTUM_GREY_900 = Color.parseColor("#ff212121")
        private val GOOGLE_GREY_900 = Color.parseColor("#ff202124")

        //Google app light secondary color (primary = white)
        private val GOOGLE_GREY_LIGHT = Color.parseColor("#f1f3f4")
    }

    private var discoverKillerOverlayController: DiscoverKillerOverlayController? = null
    private var isDiscoverActivityShowing = false
    private var remoteSettings: RemoteSettingsHolder? = null

    private val monet by lazy {
        MonetCompat.getInstance()
    }

    override fun onAppLoaded(lpparam: XC_LoadPackage.LoadPackageParam) {
        setupDrawerOverlayServiceHooks(lpparam)
        setupZeroStateHooks(lpparam)
    }

    /**
     *  Hooks calls in DrawerOverlayService, with the following logic:
     *  * onCreate: Keep original call, create [DiscoverKillerOverlayController] after call
     *  * onBind: Replace original call, call [DiscoverKillerOverlayController.onBind] and return the [IBinder]
     *  * onUnbind: Replace original call, call [DiscoverKillerOverlayController.onUnbind] and return `true`
     *  * onDestroy: Keep original call, call [SnapshotOverlay.onDestroy] after call
     *  * dump: Replace original call, call [DiscoverKillerOverlayController.dump] and return the response
     *
     *  If [RemoteSettingsHolder.overlayEnabled] is set to `false`, no controller calls will be made and the default
     *  service will be used instead.
     *
     *  We can't simply replace all the calls as there are super calls involved which break the service if not called.
     */
    private fun setupDrawerOverlayServiceHooks(lpparam: XC_LoadPackage.LoadPackageParam){
        val overlayClass = XposedHelpers.findClass("com.google.android.apps.gsa.nowoverlayservice.DrawerOverlayService", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(overlayClass, "onCreate", object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val service = param.thisObject as Service
                val remoteSettings = RemoteSettings.getInstance().getRemoteSettings(service).also {
                    this@GoogleApp.remoteSettings = it
                }
                if(remoteSettings.overlayEnabled) {
                    discoverKillerOverlayController = DiscoverKillerOverlayController(service, remoteSettings)
                }
            }
        })
        XposedHelpers.findAndHookMethod(overlayClass, "onBind", Intent::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                if(remoteSettings?.overlayEnabled == true) {
                    val result = discoverKillerOverlayController?.onBind(param.args[0] as Intent)
                    param.result = result as IBinder
                }
            }
        })
        XposedHelpers.findAndHookMethod(overlayClass, "onUnbind", Intent::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                if(remoteSettings?.overlayEnabled == true) {
                    discoverKillerOverlayController?.onUnbind(param.args[0] as Intent)
                    param.result = true
                }
            }
        })
        XposedHelpers.findAndHookMethod(overlayClass, "onDestroy", object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                if(remoteSettings?.overlayEnabled == true) {
                    discoverKillerOverlayController?.onDestroy()
                }
                remoteSettings = null
            }
        })
        XposedHelpers.findAndHookMethod(overlayClass, "dump", FileDescriptor::class.java, PrintWriter::class.java, Array<String>::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                if(remoteSettings?.overlayEnabled == true) {
                    discoverKillerOverlayController?.dump(param.args[1] as PrintWriter)
                    param.result = true
                }
            }
        })
    }

    private fun setupZeroStateHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        setupZeroStateWindowHooks()
        setupZeroStateStateHooks(lpparam)
        setupZeroStateColorHooks()
        setupZeroStateActivityStartHooks(lpparam)
        setupZeroStateRefreshListenerHooks(lpparam)
        setupZeroStateRecyclerHooks(lpparam)
        setupZeroStateImageViewHooks()
        setupZeroStateBackgroundHooks(lpparam)
    }

    /**
     *  Replaces the call to [Context.getSystemService] passing [Context.WINDOW_SERVICE] with
     *  the overlay WindowManager, when the activity is ZeroState or EnterOpa.
     *  This is required as the overlay is **special** and has its own WindowManager,
     *  using the normal one will crash.
     */
    private fun setupZeroStateWindowHooks() {
        XposedHelpers.findAndHookMethod(Activity::class.java, "getSystemService", String::class.java, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val clazz = param.thisObject::class.java.name
                if(clazz != ZERO_STATE_CLASS_NAME && clazz != ENTER_OPA_CLASS_NAME) {
                    return
                }
                val activity = param.thisObject as Activity
                val serviceName = param.args[0]
                if(serviceName == Context.WINDOW_SERVICE && activity.isFromDiscoverKiller){
                    val windowManager = SnapshotOverlay.getWindowManager()
                    param.result = windowManager
                }
            }
        })
    }

    /**
     *  Stores the resume state of ZeroState for use in theming
     */
    private fun setupZeroStateStateHooks(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookMethod(ZERO_STATE_CLASS_NAME, lpparam.classLoader, "onResume", object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val activity = param.thisObject as Activity
                if(activity.isFromDiscoverKiller) {
                    activity.window.decorView.removeStatusNavBackgroundOnPreDraw()
                    isDiscoverActivityShowing = true
                }
            }
        })
        XposedHelpers.findAndHookMethod(ZERO_STATE_CLASS_NAME, lpparam.classLoader, "onPause", object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                isDiscoverActivityShowing = false
            }
        })
    }

    /**
     *  Hooks [TextView.setTextColor], replacing calls where the [QUANTUM_GREY_900] and [GOOGLE_GREY_900] colors are being looked up
     *  to return [Color.WHITE] and [GOOGLE_GREY_LIGHT] respectively, when the dark theme is in use.
     *
     *  This is no-op when [isDiscoverActivityShowing] is false (ie. the overlay is not open) or if not in dark theme.
     */
    private fun setupZeroStateColorHooks() {
        XposedHelpers.findAndHookMethod(TextView::class.java, "setTextColor", ColorStateList::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val textView = param.thisObject as TextView
                if(!isDiscoverActivityShowing) return
                if(!textView.context.isDarkMode) return
                val textColors = param.args[0] as ColorStateList
                val textColor = textColors.defaultColor
                if(textColor == QUANTUM_GREY_900){
                    textView.post {
                        if(textView.isAttachedToWindow) {
                            textView.setTextColor(Color.WHITE)
                        }
                    }
                }
                if(textColor == GOOGLE_GREY_900){
                    textView.post {
                        if(textView.isAttachedToWindow) {
                            textView.setTextColor(GOOGLE_GREY_LIGHT)
                        }
                    }
                }
            }
        })
    }

    /**
     *  1.) Hooks [Context.startActivity] (via ContextImpl), adding [INTENT_KEY_FROM_DISCOVER_KILLER] = `true` when starting ZeroState,
     *  and firing it on to the IntentForwarder. This is then handled by the overlay as a local startActivity, rather than a proper one.
     *  It's required as EnterOpa starts some services & sets some fields that when un-set, can cause ZeroState to not reload properly.
     *  This is a no-op when there's no IntentForwarder to fire on to.
     *
     *  2.) Hooks [Activity.startActivityForResult], when [isDiscoverActivityShowing] = `true` (ie. the overlay is showing), to add
     *  [INTENT_KEY_FROM_DISCOVER_KILLER] = `true`. This is then used in 3.)
     *
     *  3.) Hooks [Activity.getCallingActivity], when the [Activity.getIntent] bundle has [isDiscoverActivityShowing] = `true`,
     *  and returns [ZERO_STATE_CLASS_NAME] as the class. This fixes launching activities from the overlay, as LocalActivityManager
     *  does not support [Activity.startActivityForResult]
     */
    private fun setupZeroStateActivityStartHooks(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "startActivity", Intent::class.java, Bundle::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                IntentForwarder.getInstance()?.let {
                    val intent = param.args[0] as Intent
                    if(intent.toUri(0).startsWith("googleassistant://zerostate")){
                        //Zero state intent!
                        intent.putExtra(INTENT_KEY_FROM_DISCOVER_KILLER, true)
                        it.postIntent(intent)
                        //Prevent actual startActivity call
                        param.result = true
                    }
                }
            }
        })
        XposedHelpers.findAndHookMethod(Activity::class.java, "startActivityForResult", Intent::class.java, Integer.TYPE, Bundle::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val intent = param.args[0] as Intent
                if(isDiscoverActivityShowing){
                    intent.putExtra(INTENT_KEY_FROM_DISCOVER_KILLER, true)
                }
            }
        })
        XposedHelpers.findAndHookMethod(Activity::class.java, "getCallingActivity", object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val activity = param.thisObject as Activity
                if(activity.isFromDiscoverKiller){
                    param.result = ComponentName(activity, ZERO_STATE_CLASS_NAME)
                }
            }
        })
    }

    /**
     *  Applies top padding to Zero State's main RecyclerView, to allow it to draw behind the
     *  status bar without clipping
     */
    private fun setupZeroStateRecyclerHooks(lpparam: XC_LoadPackage.LoadPackageParam){
        XposedHelpers.findAndHookConstructor("android.support.v7.widget.RecyclerView", lpparam.classLoader, Context::class.java, AttributeSet::class.java, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val view = param.thisObject as View
                view.runAfterPostIfIdMatches({ isDiscoverActivityShowing },"zero_state_content_view") {
                    view.onApplyInsets(true) { view, insets ->
                        view.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
                    }
                }
            }
        })
    }

    /**
     *  Tints the lightbulb and settings icons, sets a long click of the profile icon to restart the overlay
     */
    private fun setupZeroStateImageViewHooks() {
        XposedHelpers.findAndHookConstructor(ImageView::class.java, Context::class.java, AttributeSet::class.java, Integer.TYPE, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val view = param.thisObject as View
                view.runAfterPostIfIdMatches({ isDiscoverActivityShowing },"contextual_greeting_profile_icon") { profileIcon ->
                    profileIcon.setOnLongClickListener {
                        exitProcess(0)
                    }
                }
                view.runAfterPostIfIdMatches({ isDiscoverActivityShowing && remoteSettings?.useMonet == true },"contextual_greeting_updates_center_entrypoint", "generic_stacked_cards_section_customization_icon", "zero_state_input_plate_lens_icon", "zero_state_input_plate_keyboard_icon"){ imageview ->
                    imageview as ImageView
                    imageview.imageTintList = ColorStateList.valueOf(monet.getAccentColor(view.context))
                }
            }
        })
    }

    /**
     *  Tints the backgrounds of the FAB and bottom pill to monet background secondary if enabled,
     *  and disables elevation on the cards if monet is enabled.
     */
    private fun setupZeroStateBackgroundHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookConstructor(FrameLayout::class.java, Context::class.java, AttributeSet::class.java, Integer.TYPE, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val view = param.thisObject as View
                view.runAfterPostIfIdMatches({ isDiscoverActivityShowing && remoteSettings?.useMonet == true },"zero_state_input_plate_inner_container") { container ->
                    val monet = MonetCompat.getInstance()
                    container.background.run {
                        val paint = this::class.java.declaredFields.firstOrNull { it.type == Paint::class.java }!!.apply {
                            isAccessible = true
                        }.get(this) as Paint
                        paint.color = monet.getBackgroundColorSecondary(view.context) ?: monet.getBackgroundColor(view.context)
                        container.invalidate()
                    }
                }
            }
        })
        XposedHelpers.findAndHookConstructor(ImageButton::class.java, Context::class.java, AttributeSet::class.java, Integer.TYPE, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val view = param.thisObject as View
                view.runAfterPostIfIdMatches({ isDiscoverActivityShowing && remoteSettings?.useMonet == true },"zero_state_fab"){ fab ->
                    fab.backgroundTintList = ColorStateList.valueOf(monet.getBackgroundColorSecondary(view.context) ?: monet.getBackgroundColor(view.context))
                }
            }
        })

        XposedHelpers.findAndHookConstructor("com.facebook.litho.LithoView", lpparam.classLoader, Context::class.java, AttributeSet::class.java, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                val view = param.thisObject as View
                view.runAfterPostIfIdMatches({ isDiscoverActivityShowing && remoteSettings?.useMonet == true },"zero_state_eml_card"){ card ->
                    card.elevation = 0f
                }
            }
        })
    }

    /**
     *  1.) Hooks ZeroState [Activity.onCreate] to register a secure broadcast receiver for the [ACTION_RELOAD_SNAPSHOT] action, and stores that the activity has started
     *
     *  2.) Hooks SwipeRefreshLayout creation to store the view, when the previous stored value is set to `true` (ie. the overlay is being created)
     *      This is a no-op when the value is `false`, and resets it to `false` if required.
     *
     *  3.) Hooks ZeroState [Activity.onDestroy] to unregister the broadcast receiver and clear the stored SwipeRefreshLayout
     *
     *  The [SecureBroadcastReceiver] finds the SwipeRefreshLayout's "onRefresh" interface and invokes it, causing a refresh to trigger. If there's no
     *  stored SwipeRefreshLayout, this is a no-op.
     */
    private fun setupZeroStateRefreshListenerHooks(lpparam: XC_LoadPackage.LoadPackageParam){
        var swipeRefreshLayout: View? = null
        var isWaitingForSwipeRefreshInit = false
        val zeroStateRefreshReceiver = SecureBroadcastReceiver { _, _ ->
            swipeRefreshLayout?.let {
                val refreshInterface = swipeRefreshLayout?.getRefreshInterface()?.apply {
                    isAccessible = true
                } ?: return@let
                val refresh = refreshInterface.get(it)
                refreshInterface.type.declaredMethods.first().invoke(refresh)
            }
        }
        XposedHelpers.findAndHookMethod(ZERO_STATE_CLASS_NAME, lpparam.classLoader, "onCreate", Bundle::class.java, object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val activity = param.thisObject as Activity
                isDiscoverActivityShowing = activity.isFromDiscoverKiller
                if(!activity.isFromDiscoverKiller) return
                isWaitingForSwipeRefreshInit = true
                activity.registerReceiver(zeroStateRefreshReceiver, IntentFilter(
                    ACTION_RELOAD_SNAPSHOT
                ))
            }
        })
        XposedHelpers.findAndHookConstructor("androidx.swiperefreshlayout.widget.SwipeRefreshLayout", lpparam.classLoader, Context::class.java, AttributeSet::class.java, object: XC_MethodHook(){
            override fun afterHookedMethod(param: MethodHookParam) {
                super.afterHookedMethod(param)
                if(isWaitingForSwipeRefreshInit){
                    isWaitingForSwipeRefreshInit = false
                    swipeRefreshLayout = param.thisObject as View
                }
            }
        })
        XposedHelpers.findAndHookMethod(ZERO_STATE_CLASS_NAME, lpparam.classLoader, "onDestroy", object: XC_MethodHook(){
            override fun beforeHookedMethod(param: MethodHookParam) {
                super.beforeHookedMethod(param)
                val activity = param.thisObject as Activity
                if(!activity.isFromDiscoverKiller) return
                activity.unregisterReceiver(zeroStateRefreshReceiver)
                swipeRefreshLayout = null
            }
        })
    }

    /**
     *  Finds the "onRefresh" interface for a SwipeRefreshLayout by searching for a field with the following requirements:
     *  - It's an Interface
     *  - It has exactly one method
     *  - That one method has exactly 0 parameters
     *
     *  This is sufficient to find onRefresh and is quick as there are few fields in SwipeRefreshLayout.
     */
    private fun Any.getRefreshInterface(): Field? {
        return this::class.java.declaredFields.firstOrNull { it.type.isInterface && it.type.declaredMethods.size == 1 && it.type.declaredMethods[0].parameterCount == 0 }
    }

    /**
     *  Returns if the [Activity] has the [INTENT_KEY_FROM_DISCOVER_KILLER] = `true` extra
     */
    private val Activity.isFromDiscoverKiller
        get() = intent?.getBooleanExtra(INTENT_KEY_FROM_DISCOVER_KILLER, false) ?: false

}