package com.kieronquinn.app.discoverkiller.xposed

import android.app.Application
import android.content.Context
import android.util.Log
import com.kieronquinn.app.discoverkiller.xposed.hooks.*
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class Xposed: IXposedHookLoadPackage {

    companion object {
        const val GOOGLE_APP_PACKAGE_NAME = "com.google.android.googlequicksearchbox"
        const val ENTERTAINMENT_SPACE_PACKAGE_NAME = "com.google.android.apps.mediahome.launcher"

        private val PACKAGES = mapOf(
            GOOGLE_APP_PACKAGE_NAME to Xposed::gsaHooks,
            ENTERTAINMENT_SPACE_PACKAGE_NAME to Xposed::entertainmentSpaceHooks
        )
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val methods = PACKAGES[lpparam.packageName] ?: return
        hookMethods(lpparam, methods)
    }

    private fun hookMethods(
        lpparam: LoadPackageParam,
        methods: (Xposed, ClassLoader, Context) -> Array<BaseXposedHooks>
    ) {
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "onCreate",
            object: XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    Log.d("DOS", "onCreate ${lpparam.packageName}")
                    methods(this@Xposed, lpparam.classLoader, param.thisObject as Context).forEach {
                        it.init()
                    }
                }
            }
        )
    }

    private fun gsaHooks(classLoader: ClassLoader, context: Context): Array<BaseXposedHooks> {
        return arrayOf(
            GoogleAppDrawerOverlayServiceHooks(classLoader, context),
            GoogleAppPublicSearchHooks(classLoader)
        )
    }

    private fun entertainmentSpaceHooks(
        classLoader: ClassLoader, context: Context
    ): Array<BaseXposedHooks> {
        return arrayOf(
            EntertainmentSpaceLayoutHooks(classLoader, context),
            EntertainmentSpaceDetailsActivityHooks(classLoader)
        )
    }

}