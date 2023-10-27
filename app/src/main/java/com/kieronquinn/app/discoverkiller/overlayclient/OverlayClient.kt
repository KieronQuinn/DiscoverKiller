package com.kieronquinn.app.discoverkiller.overlayclient

import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.core.os.bundleOf
import com.google.android.libraries.launcherclient.ILauncherOverlay
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback

class OverlayClient(
    private var original: ILauncherOverlay,
    private val preferOriginal: Boolean,
    private var _runWithOverlay: (BaseOverlayClient, (ILauncherOverlay) -> Any) -> Any?
): BaseOverlayClient() {

    private val overlayState = OverlayState()

    private fun <T> runWithOverlay(block: (ILauncherOverlay) -> T): T? {
        return _runWithOverlay(this) {
            block(it) as Any
        } as? T
    }

    override fun startScroll() {
        runWithOverlay { it.startScroll() }
    }

    override fun onScroll(progress: Float) {
        runWithOverlay { it.onScroll(progress) }
    }

    override fun endScroll() {
        runWithOverlay { it.endScroll() }
    }

    override fun windowAttached(
        lp: WindowManager.LayoutParams?,
        cb: ILauncherOverlayCallback?,
        flags: Int
    ) {
        overlayState.params = lp
        overlayState.callback = null
        overlayState.flags = flags
        Log.d("DOS", "Attached, setting params to $lp")
        runWithOverlay { it.windowAttached(lp, cb, flags) }
    }

    override fun windowAttached2(bundle: Bundle?, cb: ILauncherOverlayCallback?) {
        overlayState.attachBundle = bundle
        overlayState.callback = cb
        Log.d("DOS", "Attached, keys: ${bundle?.keySet()?.joinToString(", ")}")
        bundle?.getParcelable<WindowManager.LayoutParams>("layout_params")?.let {
            Log.d("DOS", "Setting layout params to $it")
            overlayState.params = it
        }
        runWithOverlay { it.windowAttached2(bundle, cb) }
    }

    override fun windowDetached(isChangingConfigurations: Boolean) {
        overlayState.params = null
        overlayState.callback = null
        overlayState.flags = null
        overlayState.attachBundle = null
        runWithOverlay { it.windowDetached(isChangingConfigurations) }
    }

    override fun closeOverlay(flags: Int) {
        runWithOverlay { it.closeOverlay(flags) }
    }

    override fun onPause() {
        original.onPause()
        runWithOverlay { it.onPause() }
    }

    override fun onResume() {
        original.onResume()
        runWithOverlay { it.onResume() }
    }

    override fun openOverlay(flags: Int) {
        runWithOverlay { it.openOverlay(flags) }
    }

    override fun requestVoiceDetection(start: Boolean) {
        if(preferOriginal) {
            original.requestVoiceDetection(start)
            return
        }
        runWithOverlay { it.requestVoiceDetection(start) }
    }

    override fun getVoiceSearchLanguage(): String {
        return if(preferOriginal){
            original.voiceSearchLanguage
        }else{
            runWithOverlay { it.voiceSearchLanguage } ?: ""
        }
    }

    override fun isVoiceDetectionRunning(): Boolean {
        return if(preferOriginal){
            isVoiceDetectionRunning
        }else{
            runWithOverlay { it.isVoiceDetectionRunning } ?: false
        }
    }

    override fun hasOverlayContent(): Boolean {
        return runWithOverlay { it.hasOverlayContent() } ?: true
    }

    override fun unusedMethod() {
        runWithOverlay { it.unusedMethod() }
    }

    override fun setActivityState(flags: Int) {
        original.setActivityState(flags)
        runWithOverlay { it.setActivityState(flags) }
    }

    override fun startSearch(data: ByteArray?, bundle: Bundle?): Boolean {
        return if(preferOriginal){
            original.startSearch(data, bundle)
        }else{
            runWithOverlay { it.startSearch(data, bundle) } ?: false
        }
    }

    private data class OverlayState(
        var params: WindowManager.LayoutParams? = null,
        var callback: ILauncherOverlayCallback? = null,
        var flags: Int? = null,
        var attachBundle: Bundle? = null,
        var activityState: Int? = null
    )

    override fun reattach() {
        reattachWindowAttached()
        rettachWindowAttached2()
        reattachActivityState()
    }

    private fun reattachWindowAttached() {
        val params = overlayState.params ?: return
        val callback = overlayState.callback ?: return
        val flags = overlayState.flags ?: return
        runWithOverlay { it.windowAttached(params, callback, flags) }
        runWithOverlay { it.onScroll(0f) }
    }

    private fun rettachWindowAttached2() {
        val bundle = overlayState.attachBundle ?: return
        val callback = overlayState.callback ?: return
        runWithOverlay { it.windowAttached2(bundle, callback) }
        runWithOverlay { it.onScroll(0f) }
    }

    private fun reattachActivityState() {
        val activityState = overlayState.activityState ?: return
        runWithOverlay { it.setActivityState(activityState) }
    }

    override fun detach() {
        runWithOverlay { it.setActivityState(0) } //Stop overlay
        runWithOverlay { it.windowDetached(false) } //Detach overlay
    }

}