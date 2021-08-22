package com.kieronquinn.app.discoverkiller.ui.screens.overlay.snapshot

import android.app.LocalActivityManagerCompat
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.gsa.overlay.overlay.BaseOverlay
import com.kieronquinn.app.discoverkiller.components.intentforwarder.IntentForwarder
import com.kieronquinn.app.discoverkiller.components.intentforwarder.IntentForwarderCallback
import com.kieronquinn.app.discoverkiller.components.xposed.apps.GoogleApp
import com.kieronquinn.app.discoverkiller.components.xposed.apps.GoogleApp.Companion.INTENT_KEY_FROM_DISCOVER_KILLER
import com.kieronquinn.app.discoverkiller.databinding.OverlaySnapshotBinding
import com.kieronquinn.app.discoverkiller.model.RemoteSettingsHolder
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.sendSecureBroadcast
import com.kieronquinn.monetcompat.core.MonetCompat
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class SnapshotOverlay(context: Context, private val settings: RemoteSettingsHolder): BaseOverlay<OverlaySnapshotBinding>(context, OverlaySnapshotBinding::inflate), IntentForwarderCallback {

    companion object {
        private const val KEY_ACTIVITY_STATE = "activity_state"
        private const val KEY_BACKGROUND_BLUR_PROGRESS = "background_blur_progress"
        private var sWindowManager: WeakReference<WindowManager?>? = null
        val BACKGROUND_COLOR_DISCOVER_LIGHT = Color.parseColor("#F8F9FA")
        val BACKGROUND_COLOR_DISCOVER_DARK = Color.parseColor("#1F1F20")
        private val BACKGROUND_COLOR_DISCOVER_WHITE = Color.parseColor("#ffffffff")
        private val BACKGROUND_COLOR_DISCOVER_CREAM = Color.parseColor("#ffe0e0e0")
        private val TEXT_COLOR_DISCOVER_DARK = Color.parseColor("#ff212121")

        private val DISCOVER_BACKGROUND_COLORS = arrayOf(BACKGROUND_COLOR_DISCOVER_LIGHT, BACKGROUND_COLOR_DISCOVER_WHITE, BACKGROUND_COLOR_DISCOVER_CREAM)

        fun getWindowManager(): WindowManager? {
            return sWindowManager?.get()
        }
    }

    private val localActivityManager by lazy {
        LocalActivityManagerCompat(this, false)
    }

    private val screenWidth by lazy {
        Resources.getSystem().displayMetrics.widthPixels.toFloat()
    }

    private val zeroStateContentViewId by lazy {
        context.resources.getIdentifier("zero_state_content_view", "id", context.packageName)
    }

    private val zeroStateInputPlateUpdatesIconId by lazy {
        context.resources.getIdentifier("zero_state_input_plate_updates_icon", "id", context.packageName)
    }

    private val contextualGreetingUpdatesCenterEntrypointViewId by lazy {
        context.resources.getIdentifier("contextual_greeting_updates_center_entrypoint", "id", context.packageName).run {
            if(this == 0) -1
            else this
        }
    }

    private val genericStackedCardsSectionCustomizationIconViewId by lazy {
        context.resources.getIdentifier("generic_stacked_cards_section_customization_icon", "id", context.packageName).run {
            if(this == 0) -1
            else this
        }
    }

    private val discoverRethemeImageViews by lazy {
        arrayOf(contextualGreetingUpdatesCenterEntrypointViewId, genericStackedCardsSectionCustomizationIconViewId)
    }

    private val isDarkMode by lazy {
        context.isDarkMode
    }

    private val discoverDefaultBackgroundColor by lazy {
        if(isDarkMode) BACKGROUND_COLOR_DISCOVER_DARK
        else BACKGROUND_COLOR_DISCOVER_LIGHT
    }

    private val monet by lazy {
        setupMonet()
        MonetCompat.setup(context).apply {
            defaultBackgroundColor = discoverDefaultBackgroundColor
            updateMonetColors()
        }
    }

    private val enterOpaIntent by lazy {
        Intent().apply {
            component = ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.apps.gsa.staticplugins.opa.EnterOpaActivity" //"com.google.android.apps.gsa.staticplugins.opa.ZeroStateActivity"
            )
            putExtra(INTENT_KEY_FROM_DISCOVER_KILLER, true)
            putExtra("opa_start_zero_state", true)
        }
    }

    private var backgroundBlurProgress = 0f

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        IntentForwarder.getOrCreateInstance().setIntentForwarderCallback(this)
        sWindowManager = WeakReference(windowManager)
        backgroundBlurProgress = bundle?.getFloat(KEY_BACKGROUND_BLUR_PROGRESS, 0f) ?: 0f
        binding.background.setBackgroundColor(BACKGROUND_COLOR_DISCOVER_LIGHT)
        localActivityManager.dispatchCreate(bundle?.getBundle(KEY_ACTIVITY_STATE) ?: Bundle())
        window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        localActivityManager.startActivity("enteropa", enterOpaIntent)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root){ view, insets ->
            lifecycleScope.launchWhenResumed {
                val containerInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                binding.overlayContainer.updatePadding(top = containerInsets.top, left = containerInsets.left, right = containerInsets.right, bottom = containerInsets.bottom)
            }
            insets
        }
    }

    override fun onPause() {
        super.onPause()
        localActivityManager.dispatchPause(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        localActivityManager.dispatchDestroy(true)
        sWindowManager = null
        IntentForwarder.clearInstance()
    }

    override fun onResume() {
        super.onResume()
        updateProgressViews(backgroundBlurProgress, true)
        localActivityManager.dispatchResume()
        window?.decorView?.post {
            lifecycleScope.launchWhenResumed {
                //Fix case where *two* status bar backgrounds sometimes appear
                window.decorView.removeStatusNavBackgroundOnPreDraw()
                binding.overlayContainer.removeStatusNavBackgroundOnPreDraw()
            }
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putBundle(KEY_ACTIVITY_STATE, localActivityManager.saveInstanceState())
        bundle.putFloat(KEY_BACKGROUND_BLUR_PROGRESS, backgroundBlurProgress)
    }

    override fun onDragProgress(progress: Float) {
        super.onDragProgress(progress)
        updateProgressViews(progress)
    }

    private fun updateProgressViews(progress: Float, force: Boolean = false){
        if(backgroundBlurProgress == progress && !force) return
        applyBlurToBackground(progress)
        backgroundBlurProgress = progress
        //Below half width the overlay container is full width, above that we need to adjust for progress
        val progressWidth = if(progress < 0.5f){
            screenWidth
        }else{
            progress * 2f * screenWidth
        }.roundToInt()
        binding.root.alpha = progress
        binding.background.updateLayoutParams<FrameLayout.LayoutParams> {
            width = progressWidth
        }
    }

    override fun onIntentForwarded(intent: Intent) {
        IntentForwarder.clearInstance()
        setupWithZeroStateActivity(intent)
    }

    private fun setupWithZeroStateActivity(intent: Intent) = lifecycleScope.launchWhenResumed {
        val window = localActivityManager.startActivity("overlay", intent).apply {
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }
        window.decorView.post {
            lifecycleScope.launchWhenResumed {
                rethemeViews(window.decorView)
                window.decorView.findViewById<View>(zeroStateInputPlateUpdatesIconId)?.visibility = View.GONE
            }
        }
        binding.overlayContainer.addView(window.decorView.removeStatusNavBackgroundOnPreDraw())
    }

    private suspend fun rethemeViews(root: View){
        val views = ArrayList<View>()
        val textViews = ArrayList<TextView>()
        val imageViews = ArrayList<ImageView>()
        findRethemeViewsRecursive(root, views, textViews, imageViews)
        val backgroundColor = getBackgroundColor(root.context)
        binding.background.setBackgroundColor(backgroundColor)
        views.forEach {
            it.background = ColorDrawable(backgroundColor)
        }
        textViews.forEach {
            it.setTextColor(Color.WHITE)
        }
        imageViews.forEach {
            it.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }
    }

    private suspend fun getBackgroundColor(context: Context): Int {
        return if(settings.useMonet){
            monet.awaitMonetReady()
            monet.getBackgroundColor(context, isDarkMode)
        }else{
            discoverDefaultBackgroundColor
        }
    }

    override fun onHomeOrBackPressed(i: Int) {
        super.onHomeOrBackPressed(i)
        if(i == 0){
            sendReloadBroadcast()
            window?.decorView?.removeStatusNavBackgroundOnPreDraw()
            optBinding?.overlayContainer?.removeStatusNavBackgroundOnPreDraw()
        }
    }

    private fun sendReloadBroadcast(){
        if(settings.autoReloadSnapshot) {
            sendSecureBroadcast(Intent(GoogleApp.ACTION_RELOAD_SNAPSHOT).apply {
                `package` = packageName
            })
        }
    }

    private fun setupMonet(){
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

    private fun findRethemeViewsRecursive(view: View, views: ArrayList<View>, textViews: ArrayList<TextView>, imageViews: ArrayList<ImageView>){
        if((view.background as? ColorDrawable)?.let { DISCOVER_BACKGROUND_COLORS.contains(it.color) } == true){
            views.add(view)
        }
        if(view.id == zeroStateContentViewId){
            views.add(view)
        }
        if(discoverRethemeImageViews.contains(view.id)){
            imageViews.add(view as ImageView)
        }
        if(view is TextView && view.currentTextColor == TEXT_COLOR_DISCOVER_DARK){
            textViews.add(view)
        }
        if(view is ViewGroup){
            view.children.forEach {
                findRethemeViewsRecursive(it, views, textViews, imageViews)
            }
        }
    }

}