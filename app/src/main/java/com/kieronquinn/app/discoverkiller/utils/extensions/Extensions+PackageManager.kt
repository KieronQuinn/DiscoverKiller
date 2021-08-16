package com.kieronquinn.app.discoverkiller.utils.extensions

import android.content.pm.PackageManager

fun PackageManager.isAppInstalled(packageName: String): Boolean {
    return try {
        getPackageInfo(packageName, 0)
        true
    }catch (e: PackageManager.NameNotFoundException){
        false
    }
}