package com.kieronquinn.app.discoverkiller.utils.extensions

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager

fun PackageManager.getDefaultLauncher(): String? {
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    return resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName
}

fun PackageManager.isAppInstalled(packageName: String): Boolean {
    return try {
        getPackageInfo(packageName, 0)
        true
    }catch (e: PackageManager.NameNotFoundException){
        false
    }
}

fun PackageItemInfo.toComponent(): ComponentName {
    return ComponentName(packageName, name)
}