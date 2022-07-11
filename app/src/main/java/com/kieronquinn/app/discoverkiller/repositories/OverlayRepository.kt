package com.kieronquinn.app.discoverkiller.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.annotation.StringRes
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository.Overlay
import com.kieronquinn.app.discoverkiller.repositories.OverlayRepository.OverlayType
import com.kieronquinn.app.discoverkiller.service.OverlayRssService
import com.kieronquinn.app.discoverkiller.service.OverlayUnsetService
import com.kieronquinn.app.discoverkiller.utils.extensions.getDefaultLauncher
import com.kieronquinn.app.discoverkiller.utils.extensions.toComponent
import com.kieronquinn.app.discoverkiller.xposed.Xposed
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

interface OverlayRepository {

    suspend fun terminateLauncher()
    suspend fun getOverlays(): List<Overlay>

    fun getOverlayName(componentName: ComponentName): CharSequence

    fun doesOverlaySupportRestart(componentName: ComponentName): Boolean
    fun doesOverlaySupportMonet(componentName: ComponentName): Boolean
    fun shouldAlwaysUseOriginalHandler(component: String): Boolean

    data class Overlay(
        val label: CharSequence,
        val overlayType: OverlayType,
        val overlayComponent: ComponentName
    )

    enum class OverlayType(@StringRes val nameRes: Int) {
        NOW(R.string.overlay_type_now),
        MEDIA(R.string.overlay_type_media),
        RSS(R.string.overlay_type_rss)
    }

}

class OverlayRepositoryImpl(context: Context): OverlayRepository {

    companion object {
        private val DENYLISTED_PACKAGES = arrayOf(
            Xposed.GOOGLE_APP_PACKAGE_NAME, BuildConfig.APPLICATION_ID
        )

        private val INTENT_NOW_OVERLAY = Intent("com.android.launcher3.WINDOW_OVERLAY").apply {
            data = Uri.parse("app://")
        }

        private val INTENT_MEDIA_HOME_OVERLAY = Intent("com.google.android.apps.mediahome.SHOW_OVERLAY").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = Uri.parse("app://")
        }
    }

    private val packageManager = context.packageManager
    private val resources = context.resources

    override suspend fun terminateLauncher() {
        withContext(Dispatchers.IO) {
            val defaultLauncher = packageManager.getDefaultLauncher()
            Shell.cmd("am force-stop $defaultLauncher").submit()
        }
    }

    override suspend fun getOverlays() = withContext(Dispatchers.IO) {
        val nowOverlays = packageManager.queryIntentServices(INTENT_NOW_OVERLAY, 0).filterNot {
            DENYLISTED_PACKAGES.contains(it.serviceInfo.packageName)
        }.mapToOverlays(OverlayType.NOW)
        val mediaHomeOverlays = packageManager.queryIntentServices(
            INTENT_MEDIA_HOME_OVERLAY, 0
        ).filterNot {
            DENYLISTED_PACKAGES.contains(it.serviceInfo.packageName)
        }.mapToOverlays(OverlayType.MEDIA)
        val customOverlays = arrayOf(
            Overlay(
                resources.getString(R.string.overlay_title_rss),
                OverlayType.RSS,
                OverlayRssService.COMPONENT
            )
        )
        (nowOverlays + mediaHomeOverlays + customOverlays).sortedBy { it.label.toString().lowercase() }
    }

    private fun List<ResolveInfo>.mapToOverlays(type: OverlayType) = map {
        val label = it.loadLabel(packageManager)
        val component = ComponentName(it.serviceInfo.packageName, it.serviceInfo.name)
        Overlay(label, type, component)
    }

    override fun getOverlayName(componentName: ComponentName): CharSequence {
        val overlayInfo = try {
            packageManager.getServiceInfo(componentName, 0)
        }catch (e: PackageManager.NameNotFoundException){
            null
        }
        return when (overlayInfo) {
            null -> resources.getString(R.string.overlay_unset)
            else -> overlayInfo.loadLabel(packageManager)
        }
    }

    override fun doesOverlaySupportMonet(componentName: ComponentName): Boolean {
        return componentName == OverlayRssService.COMPONENT
    }

    override fun doesOverlaySupportRestart(componentName: ComponentName): Boolean {
        return componentName.packageName == Xposed.ENTERTAINMENT_SPACE_PACKAGE_NAME
    }

    override fun shouldAlwaysUseOriginalHandler(component: String): Boolean {
        val componentName = try {
            ComponentName.unflattenFromString(component)
        }catch (e: Exception){
            null
        } ?: return false
        return arrayOf(
            OverlayRssService.COMPONENT,
            OverlayUnsetService.COMPONENT
        ).contains(componentName)
    }

}