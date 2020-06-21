package com.kieronquinn.app.discoverkiller.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object XposedUtils {

    private const val EDXPOSED_PACKAGE_NAME = "org.meowcat.edxposed.manager"

    //This will get hooked by the module itself when using EdXposed to return true
    fun isEdXposedModuleActive(): Boolean {
        return false
    }

    fun isEdXposedInstalled(context: Context?): Boolean {
        return try {
            context?.packageManager?.getApplicationInfo(EDXPOSED_PACKAGE_NAME, 0)?.enabled ?: false
        }catch (e: PackageManager.NameNotFoundException){
            false
        }
    }

    //Launches EdXposed to the modules fragment
    fun getEdXposedLaunchIntent(context: Context): Intent {
        return context.packageManager.getLaunchIntentForPackage(EDXPOSED_PACKAGE_NAME)!!.putExtra("fragment", 3)
    }

}