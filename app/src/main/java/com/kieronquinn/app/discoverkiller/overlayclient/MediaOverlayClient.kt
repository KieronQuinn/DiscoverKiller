package com.kieronquinn.app.discoverkiller.overlayclient

import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.core.os.bundleOf
import com.google.android.libraries.launcherclient.ILauncherOverlay
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback
import com.google.android.mediahome.launcheroverlay.common.AnimationType
import com.kieronquinn.app.discoverkiller.utils.extensions.ping
import com.google.android.mediahome.launcheroverlay.aidl.ILauncherOverlay as IMediaLauncherOverlay
import com.google.android.mediahome.launcheroverlay.aidl.ILauncherOverlayCallback as IMediaLauncherOverlayCallback

class MediaOverlayClient(
    private var original: ILauncherOverlay,
    private val slideAnimationDuration: Int,
    private var _runWithOverlay: (BaseOverlayClient, (IMediaLauncherOverlay) -> Any) -> Any
): BaseOverlayClient() {

    private val overlayState = OverlayState()

    companion object {
        private const val BUNDLE_KEY_ANIMATION_TYPE = "overlay_animation_type"
        private const val BUNDLE_KEY_ANIMATION_DURATION = "overlay_animation_duration"
    }

    private fun <T> runWithOverlay(block: (IMediaLauncherOverlay) -> T): T {
        return _runWithOverlay(this) {
            block(it) as Any
        } as T
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
        runWithOverlay { it.ping() }
    }

    override fun windowDetached(isChangingConfigurations: Boolean) {
        overlayState.callback = null
        overlayState.attachBundle = null
        runWithOverlay { it.windowDetached(isChangingConfigurations) }
    }

    override fun closeOverlay(flags: Int) {
        runWithOverlay {
            it.hideOverlay(
                bundleOf(
                    BUNDLE_KEY_ANIMATION_TYPE to AnimationType.SLIDE.ordinal,
                    BUNDLE_KEY_ANIMATION_DURATION to slideAnimationDuration
                )
            )
        }
    }

    override fun onPause() {
        original.onPause()
    }

    override fun onResume() {
        original.onResume()
    }

    override fun openOverlay(flags: Int) {
        runWithOverlay {
            it.showOverlay(
                bundleOf(
                    BUNDLE_KEY_ANIMATION_TYPE to AnimationType.SLIDE.ordinal,
                    BUNDLE_KEY_ANIMATION_DURATION to slideAnimationDuration
                )
            )
        }
    }

    override fun requestVoiceDetection(start: Boolean) {
        original.requestVoiceDetection(start)
    }

    override fun getVoiceSearchLanguage(): String {
        return original.voiceSearchLanguage
    }

    override fun isVoiceDetectionRunning(): Boolean {
        return original.isVoiceDetectionRunning
    }

    override fun hasOverlayContent(): Boolean {
        return runWithOverlay { it.hasOverlayContent() }
    }

    override fun windowAttached2(bundle: Bundle?, cb: ILauncherOverlayCallback) {
        overlayState.attachBundle = bundle
        val callback = BridgeCallback(cb)
        overlayState.callback = callback
        runWithOverlay { it.windowAttached(bundle, callback) }
    }

    override fun unusedMethod() {
        runWithOverlay { it.ping() }
    }

    override fun setActivityState(flags: Int) {
        original.setActivityState(flags)
        //The flags don't match up between the two clients
        //onStop = 0, onStart = 1
        when (flags) {
            3 -> {
                runWithOverlay { it.setActivityState(1) }
                overlayState.activityState = 1
            }
            0 -> {
                runWithOverlay { it.setActivityState(1) }
                overlayState.activityState = 1
            }
            1 -> {
                runWithOverlay { it.setActivityState(0) }
                overlayState.activityState = 0
            }
        }
    }

    override fun startSearch(data: ByteArray?, bundle: Bundle?): Boolean {
        return original.startSearch(data, bundle)
    }

    private data class OverlayState(
        var callback: IMediaLauncherOverlayCallback? = null,
        var attachBundle: Bundle? = null,
        var activityState: Int? = null
    )

    override fun reattach() {
        rettachWindowAttached2()
        reattachActivityState()
    }

    private fun rettachWindowAttached2() {
        val bundle = overlayState.attachBundle ?: return
        val callback = overlayState.callback ?: return
        runWithOverlay { it.windowAttached(bundle, callback) }
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

    private inner class BridgeCallback(val original: ILauncherOverlayCallback): IMediaLauncherOverlayCallback.Stub() {

        override fun overlayScrollChanged(progress: Float) {
            original.overlayScrollChanged(progress)
        }

        override fun overlayWindowAttached(options: Bundle) {
            val serviceStatus = options.getInt("service_status")
            original.overlayStatusChanged(serviceStatus)
        }

    }

}