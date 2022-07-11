package com.kieronquinn.app.discoverkiller.xposed.hooks

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.view.IWindowManager
import android.view.WindowManager
import com.google.android.libraries.launcherclient.ILauncherOverlay
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.overlayclient.BaseOverlayClient
import com.kieronquinn.app.discoverkiller.overlayclient.MediaOverlayClient
import com.kieronquinn.app.discoverkiller.overlayclient.OverlayClient
import com.kieronquinn.app.discoverkiller.providers.OverlaySettingsProvider
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayType
import com.kieronquinn.app.discoverkiller.service.DiscoverKillerService
import com.kieronquinn.app.discoverkiller.service.IDiscoverKiller
import com.kieronquinn.app.discoverkiller.service.OverlayUnsetService
import com.kieronquinn.app.discoverkiller.utils.extensions.*
import com.kieronquinn.app.discoverkiller.xposed.BaseXposedHooks
import com.kieronquinn.app.discoverkiller.xposed.Xposed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import rikka.shizuku.SystemServiceHelper
import java.util.concurrent.Executors
import com.google.android.mediahome.launcheroverlay.aidl.ILauncherOverlay as IMediaLauncherOverlay

/**
 *  Hooks DrawerOverlayService, replacing the regular overlay with one that proxies another overlay
 *  service. Can handle multiple clients for different apps, without needing multiple instances
 *  of the proxied overlay.
 */
@Suppress("unused")
class GoogleAppDrawerOverlayServiceHooks(
    classLoader: ClassLoader,
    private val applicationContext: Context
): BaseXposedHooks(classLoader) {

    companion object {
        private const val SERVICE_CONNECT_TIMEOUT = 2500L

        private const val ACTION_WINDOW_OVERLAY = "com.android.launcher3.WINDOW_OVERLAY"
        private const val ACTION_MEDIA_OVERLAY = "com.google.android.apps.mediahome.SHOW_OVERLAY"
    }

    private val slideAnimationDuration =
        applicationContext.resources.getInteger(android.R.integer.config_mediumAnimTime)

    private val windowManager by lazy {
        val proxy = SystemServiceHelper.getSystemService("window")
        IWindowManager.Stub.asInterface(proxy)
    }

    private var shouldReconnect = false

    private val proxyServiceExecutor = Executors.newSingleThreadExecutor()
    private var serviceContext: Context? = null
    private var lastOverlay: ILauncherOverlay? = null

    private var proxyServiceConnection: ServiceConnection? = null
    private var proxyService: ILauncherOverlay? = null
    private var proxyIntent: Intent? = null

    private var proxyMediaServiceConnection: ServiceConnection? = null
    private var proxyMediaService: IMediaLauncherOverlay? = null
    private var proxyMediaIntent: Intent? = null

    private var discoverKillerServiceConnection: ServiceConnection? = null
    private var discoverKillerService: IDiscoverKiller? = null
    private var discoverKillerIntent = Intent(DiscoverKillerService.ACTION).apply {
        `package` = BuildConfig.APPLICATION_ID
    }

    private val overlayClients = HashMap<Int, BaseOverlayClient>()
    private var currentClientId = -1
    private val mainOverlay = MainLauncherOverlay()

    private var scope = MainScope()

    private val settings = OverlaySettingsProvider.getOverlaySettings(applicationContext)
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            OverlaySettingsProvider.getCurrentOverlaySettings(applicationContext)
        )

    override val clazz = "com.google.android.apps.gsa.nowoverlayservice.DrawerOverlayService"

    private fun Intent.getVersions(): Pair<String, String> {
        val cv = data?.getQueryParameter("cv") ?: "1"
        val v = data?.getQueryParameter("v") ?: "1"
        return Pair(cv, v)
    }

    private fun createIntent(
        originalIntent: Intent, overrideComponent: ComponentName? = null
    ): Intent {
        val overlayComponent = overrideComponent ?:
            ComponentName.unflattenFromString(settings.value.overlayComponent)!!
        val versions = originalIntent.getVersions()
        val params = "cv=${versions.first}&v=${versions.second}"
        return Intent(ACTION_WINDOW_OVERLAY).apply {
            data = Uri.parse(
                "app://${applicationContext.packageName}:${Process.myUid()}/?$params"
            )
            component = overlayComponent
            `package` = overlayComponent.packageName
        }
    }

    private fun createMediaIntent(originalIntent: Intent): Intent {
        val overlayComponent = ComponentName.unflattenFromString(settings.value.overlayComponent)!!
        val versions = originalIntent.getVersions()
        val params = "cv=${versions.first}&v=${versions.second}"
        return Intent(ACTION_MEDIA_OVERLAY).apply {
            data = Uri.parse(
                "app://${applicationContext.packageName}:${Process.myUid()}/?$params"
            )
            addCategory(Intent.CATEGORY_DEFAULT)
            component = overlayComponent
            `package` = overlayComponent.packageName
        }
    }

    fun onCreate() = MethodHook {
        if(!settings.value.enabled){
            return@MethodHook MethodResult.Skip()
        }
        serviceContext = thisObject as Context
        shouldReconnect = true
        MethodResult.Skip<Unit>()
    }

    fun onDestroy() = MethodHook {
        if(!settings.value.enabled){
            return@MethodHook MethodResult.Skip()
        }
        serviceContext = null
        shouldReconnect = false
        MethodResult.Skip<Unit>()
    }

    fun onBind(intent: Intent) = MethodHook(afterHookedMethod = {
        if(!settings.value.enabled){
            return@MethodHook MethodResult.Skip()
        }
        Log.d("DOS", "onBind")
        val callingUid = intent.data?.port ?: return@MethodHook MethodResult.Skip()
        proxyIntent = createIntent(intent)
        proxyMediaIntent = createMediaIntent(intent)
        val original = result as IBinder
        val originalOverlay = ILauncherOverlay.Stub.asInterface(original)
        lastOverlay = originalOverlay
        Log.d("DOS", "onBind $callingUid")
        createTypedClient(callingUid, originalOverlay)
        MethodResult.Replace(mainOverlay.asBinder())
    })

    fun onUnbind(intent: Intent) = MethodHook {
        if(!settings.value.enabled){
            return@MethodHook MethodResult.Skip()
        }
        val callingUid = intent.data!!.port
        tearDownClient(callingUid)
        if(overlayClients.isEmpty()){
            unbindProxy()
            val service = thisObject as Service
            service.stopSelf()
        }
        MethodResult.Skip<Boolean>()
    }

    private fun createTypedClient(
        uid: Int, original: ILauncherOverlay? = lastOverlay
    ): BaseOverlayClient {
        return when(settings.value.overlayType){
            OverlayType.NOW -> {
                //Fall back to unset if service isn't available
                if(!proxyIntent.isAvailable()){
                    Log.d("GSA", "Intent not available, falling back")
                    proxyIntent = createIntent(proxyIntent!!, OverlayUnsetService.COMPONENT)
                }
                createClient(uid, original)
            }
            OverlayType.MEDIA -> {
                //Fall back to unset if service isn't available
                if(!proxyMediaIntent.isAvailable()){
                    proxyIntent = createIntent(proxyIntent!!, OverlayUnsetService.COMPONENT)
                    return createClient(uid, original)
                }
                createMediaClient(uid, original)
            }
        }
    }

    private fun Intent?.isAvailable(): Boolean {
        if(this == null) throw RuntimeException("Intent not available when required")
        return applicationContext.packageManager.queryIntentServices(this, 0).isNotEmpty()
    }

    private fun createClient(uid: Int, original: ILauncherOverlay? = lastOverlay): OverlayClient {
        val originalOverlay = original
            ?: throw RuntimeException("Client for UID $uid not registered")
        val useOriginal = settings.value.originalHandlesSearch
        Log.d("DOS", "Creating client for $uid, useOriginal = $useOriginal")
        return OverlayClient(originalOverlay, useOriginal, ::runWithService).also {
            overlayClients[uid] = it
        }
    }

    private fun createMediaClient(uid: Int, original: ILauncherOverlay? = lastOverlay): MediaOverlayClient {
        val originalOverlay = original
            ?: throw RuntimeException("Client for UID $uid not registered")
        Log.d("DOS", "Creating media client for $uid")
        return MediaOverlayClient(originalOverlay, slideAnimationDuration, ::runWithMediaService).also {
            overlayClients[uid] = it
        }
    }

    private fun tearDownClient(uid: Int) {
        Log.d("DOS", "Tearing down client for $uid")
        overlayClients.remove(uid)
    }

    @Synchronized
    private fun <T> runWithService(
        overlayClient: BaseOverlayClient,
        block: (ILauncherOverlay) -> T
    ): T {
        proxyService?.let {
            if(!it.ping()) return@let
            return block(it)
        }
        val intent = proxyIntent ?:
            throw RuntimeException("Attempting to bind to proxy service without an intent")
        val serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                proxyService = ILauncherOverlay.Stub.asInterface(binder)
                proxyServiceConnection = this
                overlayClient.reattach()
            }

            override fun onServiceDisconnected(component: ComponentName) {
                proxyServiceConnection = null
                proxyService = null
            }
        }
        applicationContext.bindService(
            intent,
            Context.BIND_AUTO_CREATE,
            proxyServiceExecutor,
            serviceConnection
        )
        val startTime = System.currentTimeMillis()
        while(proxyService == null){
            if(System.currentTimeMillis() - startTime > SERVICE_CONNECT_TIMEOUT){
                Log.e(TAG, "Failed to connect to service, stopping self")
                (serviceContext as? Service)?.stopSelf()
            }
            Thread.sleep(10)
        }
        return block(proxyService!!)
    }

    @Synchronized
    private fun <T> runWithMediaService(
        overlayClient: BaseOverlayClient, block: (IMediaLauncherOverlay) -> T
    ): T {
        proxyMediaService?.let {
            if(!it.ping()) return@let
            return block(it)
        }
        val intent = proxyMediaIntent ?:
            throw RuntimeException("Attempting to bind to proxy service without an intent")
        val serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                proxyMediaService = IMediaLauncherOverlay.Stub.asInterface(binder)
                proxyMediaServiceConnection = this
                overlayClient.reattach()
            }

            override fun onServiceDisconnected(component: ComponentName) {
                proxyMediaServiceConnection = null
                proxyMediaService = null
            }
        }
        applicationContext.bindService(
            intent,
            Context.BIND_AUTO_CREATE,
            proxyServiceExecutor,
            serviceConnection
        )
        val startTime = System.currentTimeMillis()
        while(proxyMediaService == null){
            if(System.currentTimeMillis() - startTime > SERVICE_CONNECT_TIMEOUT){
                Log.e(TAG, "Failed to connect to service, stopping self")
                (serviceContext as? Service)?.stopSelf()
            }
            Thread.sleep(10)
        }
        return block(proxyMediaService!!)
    }

    @Synchronized
    private fun <T> runWithDiscoverKillerService(block: (IDiscoverKiller) -> T): T {
        discoverKillerService?.let {
            if(!it.safePing()) return@let
            return block(it)
        }
        val serviceConnection = object: ServiceConnection {
            override fun onServiceConnected(component: ComponentName, binder: IBinder) {
                discoverKillerService = IDiscoverKiller.Stub.asInterface(binder)
                discoverKillerServiceConnection = this
            }

            override fun onServiceDisconnected(component: ComponentName) {
                discoverKillerServiceConnection = null
                discoverKillerService = null
            }
        }
        applicationContext.bindService(
            discoverKillerIntent,
            Context.BIND_AUTO_CREATE,
            proxyServiceExecutor,
            serviceConnection
        )
        val startTime = System.currentTimeMillis()
        while(discoverKillerService == null){
            if(System.currentTimeMillis() - startTime > SERVICE_CONNECT_TIMEOUT){
                throw RuntimeException("Timeout: Failed to connect to service")
            }
            Thread.sleep(10)
        }
        return block(discoverKillerService!!)
    }

    private fun unbindProxy(unbindDiscoverKiller: Boolean = true) {
        proxyServiceConnection?.let {
            applicationContext.unbindSafely(it)
        }
        proxyServiceConnection = null
        proxyService = null
        proxyMediaServiceConnection?.let {
            applicationContext.unbindSafely(it)
        }
        proxyMediaServiceConnection = null
        proxyMediaService = null
        if(!unbindDiscoverKiller) return
        discoverKillerServiceConnection?.let {
            applicationContext.unbindSafely(it)
        }
    }

    private fun teardownClientsIfNeeded() {
        val shouldTerminate = settings.value.entertainmentSpaceRestart
        if(!shouldTerminate) return
        val overlayPackage = settings.value.getOverlayComponent()
        if(overlayPackage.packageName != Xposed.ENTERTAINMENT_SPACE_PACKAGE_NAME) return
        runWithDiscoverKillerService {
            it.killOverlayPackage(overlayPackage.packageName)
        }
        Log.d("DOS", "Overlay $overlayPackage terminated")
    }

    private fun recreateClients() {
        val uids = overlayClients.keys
        uids.forEach {
            overlayClients[it] = createTypedClient(it, lastOverlay).also { newClient ->
                newClient.reattach()
            }
        }
    }

    private fun setupRecreate() = scope.launch {
        settings.debounce(250L).collect {
            Log.d("DOS", "Settings changed: $it")
            proxyIntent = createIntent(proxyIntent ?: return@collect)
            proxyMediaIntent = createMediaIntent(proxyMediaIntent ?: return@collect)
            unbindProxy(false)
            recreateClients()
        }
    }

    init {
        setupRecreate()
    }

    private inner class MainLauncherOverlay: ILauncherOverlay.Stub() {

        private fun <T> runWithClient(block: (ILauncherOverlay) -> T): T {
            val clientId = Binder.getCallingUid()
            if(currentClientId != clientId){
                //Detach the current client and attach the new one
                overlayClients[currentClientId]?.detach()
                currentClientId = clientId
                overlayClients[currentClientId]?.reattach()
            }
            setupActivityStarts(1)
            return runWithClient(clientId, block)
        }

        private fun <T> runWithClient(uid: Int, block: (ILauncherOverlay) -> T): T {
            val client = overlayClients[uid] ?: run {
                createTypedClient(uid)
            }
            return block(client)
        }

        private var serviceTerminateJob: Job? = null

        private fun setOverlayVisible(visible: Boolean) {
            serviceTerminateJob?.cancel()
            serviceTerminateJob = if(!visible) scope?.launch {
                delay(5000L)
                teardownClientsIfNeeded()
            }else null
        }

        override fun startScroll() {
            runWithClient { it.startScroll() }
        }

        override fun onScroll(progress: Float) {
            runWithClient { it.onScroll(progress) }
        }

        override fun endScroll() {
            runWithClient { it.endScroll() }
        }

        override fun windowAttached(
            lp: WindowManager.LayoutParams?,
            cb: ILauncherOverlayCallback?,
            flags: Int
        ) {
            runWithClient { it.windowAttached(lp, cb, flags) }
        }

        override fun windowDetached(isChangingConfigurations: Boolean) {
            runWithClient { it.windowDetached(isChangingConfigurations) }
        }

        override fun closeOverlay(flags: Int) {
            runWithClient { it.closeOverlay(flags) }
        }

        override fun onPause() {
            runWithClient { it.onPause() }
        }

        override fun onResume() {
            runWithClient { it.onResume() }
        }

        override fun openOverlay(flags: Int) {
            runWithClient { it.openOverlay(flags) }
        }

        override fun requestVoiceDetection(start: Boolean) {
            runWithClient { it.requestVoiceDetection(start) }
        }

        override fun getVoiceSearchLanguage(): String {
            return runWithClient { it.voiceSearchLanguage }
        }

        override fun isVoiceDetectionRunning(): Boolean {
            return runWithClient { it.isVoiceDetectionRunning }
        }

        override fun hasOverlayContent(): Boolean {
            return runWithClient { it.hasOverlayContent() }
        }

        override fun windowAttached2(bundle: Bundle?, cb: ILauncherOverlayCallback?) {
            runWithClient { it.windowAttached2(bundle, cb) }
        }

        override fun unusedMethod() {
            runWithClient { it.unusedMethod() }
        }

        override fun setActivityState(flags: Int) {
            setupActivityStarts(flags)
            //Activity State can be set after a different client is bound, so access directly
            runWithClient(Binder.getCallingUid()) { it.setActivityState(flags) }
        }

        private fun setupActivityStarts(flags: Int){
            //Reject stale calls from non-current clients
            if(Binder.getCallingUid() != currentClientId) return
            when(flags){
                1, 3 -> {
                    //Overlay has opened, enable background starts for overlays that need it
                    runWithDiscoverKillerService { it.setBypassBackgroundStarts(true) }
                    setOverlayVisible(true)
                }
                0 -> {
                    //Launcher has closed (as well as overlay), disable background starts again
                    runWithDiscoverKillerService { it.setBypassBackgroundStarts(false) }
                    setOverlayVisible(false)
                }
            }
        }

        override fun startSearch(data: ByteArray?, bundle: Bundle?): Boolean {
            Log.d("DOS2", "startSearch")
            return runWithClient { it.startSearch(data, bundle) }
        }

    }

}