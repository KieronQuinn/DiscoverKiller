package com.kieronquinn.app.discoverkiller.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kieronquinn.app.discoverkiller.xposed.Xposed.Companion.GOOGLE_APP_PACKAGE_NAME
import com.topjohnwu.superuser.Shell

class DiscoverKillerService: Service() {

    companion object {
        const val ACTION = "com.kieronquinn.app.discoverkiller.SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder {
        return DiscoverServiceImpl()
    }

    private inner class DiscoverServiceImpl: IDiscoverKiller.Stub() {

        /**
         *  Background activity launches are not allowed by default on recent Android versions,
         *  and as the overlay is *technically* a service, it is in the background. We work around
         *  this by temporarily allowing background starts while the overlay is showing.
         */
        override fun setBypassBackgroundStarts(enabled: Boolean) {
            assertGoogleQuickSearchBox()
            Shell.cmd("cmd device_config put activity_manager default_background_activity_starts_enabled $enabled").submit()
        }

        override fun killOverlayPackage(packageName: String?) {
            assertGoogleQuickSearchBox()
            Shell.cmd("am force-stop $packageName").submit()
        }

        override fun ping() {
            //Pong!
        }
    }

    /**
     *  Asserts that the package that is calling this service is the Google App (overlay host),
     *  otherwise throwing a security exception to prevent unauthorised use.
     */
    private fun assertGoogleQuickSearchBox() {
        if(!getCallingPackageNames().contains(GOOGLE_APP_PACKAGE_NAME)){
            throw SecurityException("This service can only be used by the Discover Killer Xposed module.")
        }
    }

    private fun getCallingPackageNames(): Array<String> {
        return packageManager.getPackagesForUid(Binder.getCallingUid()) ?: emptyArray()
    }

}