package com.kieronquinn.app.discoverkiller.utils.extensions

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri

fun ContentResolver.safeRegisterContentObserver(
    uri: Uri, notifyForDescendants: Boolean, observer: ContentObserver
) {
    return try {
        registerContentObserver(uri, notifyForDescendants, observer)
    }catch (e: SecurityException){
        //Provider not found
    }
}