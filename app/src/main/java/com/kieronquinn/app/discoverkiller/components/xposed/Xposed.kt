package com.kieronquinn.app.discoverkiller.components.xposed

import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.components.xposed.apps.DiscoverKillerApp
import com.kieronquinn.app.discoverkiller.components.xposed.apps.GoogleApp
import com.kieronquinn.app.discoverkiller.components.xposed.apps.XposedApp
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.reflect.KClass

class Xposed : IXposedHookLoadPackage {

    companion object {
        private val APPS = mapOf(
            GoogleApp.PACKAGE_NAME to GoogleApp::class,
            BuildConfig.APPLICATION_ID to DiscoverKillerApp::class
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        for(app in APPS){
            if(app.key == lpparam.packageName){
                app.value.java.newInstance().onAppLoaded(lpparam)
            }
        }
    }

}