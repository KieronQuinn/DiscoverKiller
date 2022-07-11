package com.kieronquinn.app.discoverkiller.utils.picasso

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import com.kieronquinn.app.discoverkiller.ui.activities.MainActivity
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

/**
 *  Picasso handler that can load a given component's icon. Can handle Activities or Services,
 *  but tries Activity first.
 *
 *  Use [getUriFor] to create a compatible URI from a ComponentName.
 */
class PackageItemInfoRequestHandler(context: Context): RequestHandler() {

    companion object {
        private const val PACKAGE_ITEM_INFO_URI_SCHEME = "package"

        fun getUriFor(componentName: ComponentName): Uri {
            return Uri.Builder().apply {
                scheme(PACKAGE_ITEM_INFO_URI_SCHEME)
                authority(componentName.flattenToString())
            }.build()
        }
    }

    private val packageManager = context.packageManager
    private val defaultComponent = ComponentName(context, MainActivity::class.java)

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri?.scheme == PACKAGE_ITEM_INFO_URI_SCHEME
    }

    override fun load(request: Request, networkPolicy: Int): Result? {
        val uri = request.uri
        val component = uri.authority ?: return null
        val icon = loadIcon(component)
            ?: return null
        return Result(icon, Picasso.LoadedFrom.DISK)
    }

    private fun loadIcon(component: String): Bitmap? {
        val componentName = ComponentName.unflattenFromString(component)!!
        val packageItemInfo = componentName.getActivityInfo()
            ?: componentName.getServiceInfo()
            ?: defaultComponent.getActivityInfo()
            ?: return null
        return packageItemInfo.loadIcon(packageManager).toBitmap()
    }

    private fun ComponentName.getActivityInfo(): PackageItemInfo? {
        return try {
            packageManager.getActivityInfo(this, 0)
        }catch (e: PackageManager.NameNotFoundException){
            null
        }
    }

    private fun ComponentName.getServiceInfo(): PackageItemInfo? {
        return try {
            packageManager.getServiceInfo(this, 0)
        }catch (e: PackageManager.NameNotFoundException){
            null
        }
    }

}