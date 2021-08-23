package com.google.android.libraries.gsa.overlay.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.viewbinding.ViewBinding
import com.google.android.libraries.gsa.overlay.controllers.OverlayController
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.components.blur.BlurProvider
import com.kieronquinn.app.discoverkiller.components.settings.RemoteSettings
import org.koin.core.component.KoinComponent

abstract class BaseOverlay<T: ViewBinding>(context: Context, private val viewBindingInflate: (LayoutInflater, ViewGroup?, Boolean) -> ViewBinding): OverlayController(context, getOpaTheme(context), android.R.style.Theme_Translucent_NoTitleBar), LifecycleOwner {

    companion object {
        private fun getOpaTheme(context: Context): Int {
            return context.resources.getIdentifier("Opa_Activity_ThemeMaterial", "style", context.packageName)
        }
    }

    private val lifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    private var _binding: T? = null
    internal val binding
        get() = _binding ?: throw Exception("Cannot use binding before onCreate or after onDestroy")
    internal val optBinding: T?
        get() = _binding

    internal val remoteContext by lazy {
        context.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY)
    }

    private val remoteLayoutInflater by lazy {
        LayoutInflater.from(remoteContext).cloneInContext(remoteContext)
    }

    private val blurProvider by lazy {
        BlurProvider.getBlurProvider(remoteContext.resources)
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        container?.fitsSystemWindows = false
        _binding = viewBindingInflate.invoke(remoteLayoutInflater, container, true) as T
        window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.addFlags(Window.FEATURE_NO_TITLE)
            it.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            it.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            it.statusBarColor = Color.TRANSPARENT
            it.navigationBarColor = Color.TRANSPARENT
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onStop() {
        super.onStop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    internal fun applyBlurToBackground(progress: Float){
        //Intentionally using _rootView here in case it's called before onCreate or after onDestroy
        _binding?.root?.let {
            blurProvider.applyBlurToWindow(window!!, progress / 2f)
        }
    }

    final override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

}