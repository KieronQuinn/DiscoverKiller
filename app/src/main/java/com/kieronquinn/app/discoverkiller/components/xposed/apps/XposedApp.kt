package com.kieronquinn.app.discoverkiller.components.xposed.apps

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class XposedApp(internal val packageName: String): IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        onAppLoaded(lpparam!!)
    }

    abstract fun onAppLoaded(lpparam: XC_LoadPackage.LoadPackageParam)

}