package com.kieronquinn.app.discoverkiller.components.xposed.apps

import com.kieronquinn.app.discoverkiller.BuildConfig
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class DiscoverKillerApp: XposedApp(BuildConfig.APPLICATION_ID) {

    override fun onAppLoaded(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod("com.kieronquinn.app.discoverkiller.components.xposed.XposedSelfHook", lpparam.classLoader, "isXposedHooked", object: XC_MethodReplacement(){
            override fun replaceHookedMethod(param: MethodHookParam): Any {
                param.result = true
                return true
            }
        })
    }

}