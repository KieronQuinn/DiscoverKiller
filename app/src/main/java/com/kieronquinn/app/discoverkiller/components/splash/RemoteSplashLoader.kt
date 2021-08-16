package com.kieronquinn.app.discoverkiller.components.splash

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.SplashBinding
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.toHexString
import com.kieronquinn.monetcompat.core.MonetCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen") //Shush.
abstract class RemoteSplashLoader {

    abstract suspend fun getRemoteSplashScreenOptions(context: Context, packageName: String): List<SplashScreen>

    abstract fun getDefaultSplashForPackage(context: Context, packageName: String): SplashScreen
    abstract fun getNativeSplashScreenForPackage(context: Context, packageName: String): SplashScreen.Native?
    abstract fun getCoreSplashScreenForPackage(context: Context, packageName: String): SplashScreen.Core?
    abstract fun getWindowBackgroundSplashScreenForPackage(context: Context, packageName: String): SplashScreen.WindowBackground?
    abstract fun getIconBackgroundForPackage(context: Context, packageName: String): SplashScreen.IconBackground?
    abstract fun getMonetBackgroundForPackage(context: Context, packageName: String): SplashScreen.MonetBackground?

    abstract suspend fun inflateSplashScreen(context: Context, splashScreenType: SplashScreenType, packageName: String, parent: ViewGroup? = null, isPreview: Boolean = false): View
    abstract suspend fun inflateSplashScreenIntoBitmap(context: Context, splashScreenType: SplashScreenType, packageName: String): Bitmap

    sealed class SplashScreen(open val type: SplashScreenType) {

        /**
         *  Native (Android 12) splash screen. Most of the time this will only be used on Android 12
         *
         *  Supports background color, icon (inc. animated), branding image, background for icon.
         *  Will use the windowBackground if the background is not specified but the icon is.
         *
         *  Ignores animation duration as we only show the first frame of the icon if animated
         */
        data class Native(@ColorInt val backgroundColor: Int, val iconDrawable: Drawable, val splashBrandingImage: Drawable?, @ColorInt val splashIconBackgroundColor: Int?): SplashScreen(SplashScreenType.NATIVE) {
            override fun toString(): String {
                return "SplashScreen.Native backgroundColor ${backgroundColor.toHexString()} icon $iconDrawable splashBrandingImage $splashBrandingImage splashIconBackgroundColor ${splashIconBackgroundColor?.toHexString()}"
            }
        }

        /**
         *  androidx.core.SplashScreen splash. Supports down to SDK 23.
         *
         *  Supports background color and icon (inc. animated). Will use the windowBackground if the background is not specified but the icon is.
         *
         *  Ignores animation duration as we only show the first frame of the icon if animated, and the post show theme as we don't use that.
         *
         *  Apparently eventually it will support icon background (no word on branding image). TODO add support when that happens
         */
        data class Core(@ColorInt val backgroundColor: Int, val iconDrawable: Drawable): SplashScreen(SplashScreenType.CORE) {
            override fun toString(): String {
                return "SplashScreen.Core backgroundColor ${backgroundColor.toHexString()} icon $iconDrawable"
            }
        }

        /**
         *  WindowBackground splash (old style), supports any SDK.
         *
         *  Supports a single drawable or color, can't be animated (not that we care)
         */
        data class WindowBackground(val backgroundDrawable: Drawable): SplashScreen(SplashScreenType.WINDOW_BACKGROUND) {
            override fun toString(): String {
                return "SplashScreen.WindowBackground backgroundDrawable $backgroundDrawable"
            }
        }

        /**
         *  Generated splash using the icon.
         *
         *  If the icon is adaptive, it uses the background to generate the dominant color w/ Picasso for [backgroundColor], otherwise it will use the whole icon.
         *  If the icon is adaptive, the foreground will be returned as [iconDrawable], otherwise the whole icon will be.
         */
        data class IconBackground(@ColorInt val backgroundColor: Int, val iconDrawable: Drawable): SplashScreen(SplashScreenType.ICON_BACKGROUND) {
            override fun toString(): String {
                return "SplashScreen.IconBackground backgroundColor ${backgroundColor.toHexString()} icon $iconDrawable"
            }
        }

        /**
         *  Monet background with just the app icon (foreground if Adaptive Icon) and a color. Icon is tinted to accent color if adaptive.
         */
        data class MonetBackground(@ColorInt val backgroundColor: Int, val iconDrawable: Drawable): SplashScreen(SplashScreenType.MONET_BACKGROUND) {
            override fun toString(): String {
                return "SplashScreen.MonetBackground"
            }
        }

        /**
         *  No background, transparent (shows wallpaper underneath, blurred if supported)
         */
        object Transparent: SplashScreen(SplashScreenType.TRANSPARENT) {
            override fun toString(): String {
                return "SplashScreen.Transparent"
            }
        }
    }

    enum class SplashScreenType(@StringRes val titleRes: Int, @StringRes val descRes: Int) {
        DEFAULT(R.string.splash_type_default_title, R.string.splash_type_default_desc),
        NATIVE(R.string.splash_type_native_title, R.string.splash_type_native_desc),
        CORE(R.string.splash_type_core_title, R.string.splash_type_core_desc),
        ICON_BACKGROUND(R.string.splash_type_icon_background_title, R.string.splash_type_icon_background_desc),
        WINDOW_BACKGROUND(R.string.splash_type_window_background_title, R.string.splash_type_window_background_desc),
        MONET_BACKGROUND(R.string.splash_type_monet_background_title, R.string.splash_type_monet_background_desc),
        TRANSPARENT(R.string.splash_type_transparent_title, R.string.splash_type_transparent_desc)
    }

}

@SuppressLint("CustomSplashScreen") //Shush.
class RemoteSplashLoaderImpl: RemoteSplashLoader() {

    /**
     *  Returns the first viable Splash for a package, in the order of [RemoteSplashLoader.SplashScreenType.values]
     */
    override fun getDefaultSplashForPackage(context: Context, packageName: String): SplashScreen {
        val defaultOrder = SplashScreenType.values()
        return defaultOrder.firstNotNullOfOrNull {
            it.toSplashScreen(context, packageName)
        } ?: SplashScreen.Transparent
    }

    private fun SplashScreenType.toSplashScreen(context: Context, packageName: String): SplashScreen? {
        return when(this){
            SplashScreenType.DEFAULT -> null
            SplashScreenType.NATIVE -> getNativeSplashScreenForPackage(context, packageName)
            SplashScreenType.CORE -> getCoreSplashScreenForPackage(context, packageName)
            SplashScreenType.WINDOW_BACKGROUND -> getWindowBackgroundSplashScreenForPackage(context, packageName)
            SplashScreenType.ICON_BACKGROUND -> getIconBackgroundForPackage(context, packageName)
            SplashScreenType.MONET_BACKGROUND -> getMonetBackgroundForPackage(context, packageName)
            SplashScreenType.TRANSPARENT -> SplashScreen.Transparent
        }
    }

    /**
     *  Get all the supported [RemoteSplashLoader.SplashScreen] options for a package, which may include
     *  [RemoteSplashLoader.SplashScreen.Native], [RemoteSplashLoader.SplashScreen.WindowBackground]
     *  [RemoteSplashLoader.SplashScreen.IconBackground] and [RemoteSplashLoader.SplashScreen.MonetBackground],
     *  and will always include [RemoteSplashLoader.SplashScreen.Transparent]
     */
    override suspend fun getRemoteSplashScreenOptions(context: Context, packageName: String): List<SplashScreen> {
        return withContext(Dispatchers.IO){
            val options = ArrayList<SplashScreen>()
            getNativeSplashScreenForPackage(context, packageName)?.let {
                options.add(it)
            }
            getWindowBackgroundSplashScreenForPackage(context, packageName)?.let {
                options.add(it)
            }
            getIconBackgroundForPackage(context, packageName)?.let {
                options.add(it)
            }
            getCoreSplashScreenForPackage(context, packageName)?.let {
                options.add(it)
            }
            getMonetBackgroundForPackage(context, packageName)?.let {
                options.add(it)
            }
            options.add(SplashScreen.Transparent)
            options
        }
    }

    /**
     *  Attempts to get the native splash screen for the launch activity (+ application if null) using the Android 12 splash theme attributes.
     *  This will also work on < Android 12 *in theory*, if the app is declaring the attributes on lower SDKs
     */
    override fun getNativeSplashScreenForPackage(context: Context, packageName: String): SplashScreen.Native? {
        return getNativeSplashScreenForPackageActivity(context, packageName) ?: getNativeSplashScreenForPackageApplication(context, packageName)
    }

    private fun getNativeSplashScreenForPackageActivity(context: Context, packageName: String): SplashScreen.Native? {
        val themedContext = getThemedContextForPackage(context, packageName) ?: return null
        return getNativeSplashScreenForContext(themedContext)
    }

    private fun getNativeSplashScreenForPackageApplication(context: Context, packageName: String): SplashScreen.Native? {
        val themedContext = getThemedContextForPackageApplication(context, packageName) ?: return null
        return getNativeSplashScreenForContext(themedContext)
    }

    @SuppressLint("InlinedApi")
    private fun getNativeSplashScreenForContext(themedContext: Context): SplashScreen.Native? {
        val backgroundColor = themedContext.theme.getResolvedAttribute(android.R.attr.windowSplashScreenBackground)?.getColorOrNull(themedContext, false)
            ?: themedContext.theme.getResolvedAttribute(android.R.attr.windowBackground)?.getColorOrNull(themedContext) ?: return null
        val splashIcon = themedContext.theme.getResolvedAttribute(android.R.attr.windowSplashScreenAnimatedIcon)?.getDrawable(themedContext) ?: return null
        val splashBrandingImage = themedContext.theme.getResolvedAttribute(android.R.attr.windowSplashScreenBrandingImage)?.getDrawable(themedContext)
        val splashIconBackgroundColor = themedContext.theme.getResolvedAttribute(android.R.attr.windowSplashScreenIconBackgroundColor)?.getColorOrNull(themedContext, false)
        return SplashScreen.Native(backgroundColor, splashIcon, splashBrandingImage, splashIconBackgroundColor)
    }

    /**
     *  Attempts to get the androidx.core.SplashScreen splash for the application (+ activity if null)
     */
    override fun getCoreSplashScreenForPackage(context: Context, packageName: String): SplashScreen.Core? {
        return getCoreSplashScreenForPackageApplication(context, packageName) ?: getCoreSplashScreenForPackageActivity(context, packageName)
    }

    private fun getCoreSplashScreenForPackageActivity(context: Context, packageName: String): SplashScreen.Core? {
        val themedContext = getThemedContextForPackage(context, packageName) ?: return null
        return getCoreSplashScreenForContext(themedContext)
    }

    private fun getCoreSplashScreenForPackageApplication(context: Context, packageName: String): SplashScreen.Core? {
        val themedContext = getThemedContextForPackageApplication(context, packageName) ?: return null
        return getCoreSplashScreenForContext(themedContext)
    }

    private fun getCoreSplashScreenForContext(themedContext: Context): SplashScreen.Core? {
        val backgroundColor = themedContext.theme.getResolvedAttribute(R.attr.windowSplashScreenBackground)?.getColorOrNull(themedContext, false)
            ?: themedContext.theme.getResolvedAttribute(android.R.attr.windowBackground)?.getColorOrNull(themedContext) ?: return null
        val splashIcon = themedContext.theme.getResolvedAttribute(R.attr.windowSplashScreenAnimatedIcon)?.getDrawable(themedContext) ?: return null
        return SplashScreen.Core(backgroundColor, splashIcon)
    }

    /**
     *  Attempts to get the window background for the launch activity, supports both solid color and drawables
     */
    @SuppressLint("InlinedApi")
    override fun getWindowBackgroundSplashScreenForPackage(context: Context, packageName: String): SplashScreen.WindowBackground? {
        val themedContext = getThemedContextForPackage(context, packageName) ?: return null
        //windowSplashScreenBackground is an alternative window background-style method for Android O. But barely anyone used it...
        val background = themedContext.theme.getResolvedAttribute(android.R.attr.windowSplashScreenBackground)?.getDrawable(themedContext) ?:
            themedContext.theme.getResolvedAttribute(android.R.attr.windowBackground)?.getDrawable(themedContext) ?: return null
        return SplashScreen.WindowBackground(background)
    }

    /**
     *  Attempts to create a splash screen from the icon and dominant color, either using the raw background (Adaptive Icons)
     *  or the whole icon (non-adaptive) as the input. If the icon is adaptive, the foreground drawable is also split out
     *  for use, otherwise the whole icon is included.
     */
    override fun getIconBackgroundForPackage(context: Context, packageName: String): SplashScreen.IconBackground? {
        val launchComponent = context.packageManager.getLaunchIntentForPackage(packageName)?.resolveActivity(context.packageManager) ?: return null
        val icon = context.packageManager.getActivityIcon(launchComponent)
        return if(icon is AdaptiveIconDrawable){
            val backgroundColor = icon.background.getDominantColor(context) ?: return null
            SplashScreen.IconBackground(backgroundColor, icon.foreground)
        }else{
            val backgroundColor = icon.getDominantColor(context) ?: return null
            SplashScreen.IconBackground(backgroundColor, icon)
        }
    }

    /**
     *  Attempts to create a splash screen from the icon and Monet background color, using the foreground icon and
     *  tinting it to Monet accent if adaptive. Otherwise, just uses the background and returns the full icon.
     */
    override fun getMonetBackgroundForPackage(context: Context, packageName: String): SplashScreen.MonetBackground? {
        val packageManager = context.packageManager
        val launchComponent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val launchIcon = packageManager.getActivityIcon(launchComponent)
        val monet = MonetCompat.getInstance()
        val monetBackground = monet.getBackgroundColor(context)
        val monetAccent = monet.getAccentColor(context)
        val foregroundIcon = if(launchIcon is AdaptiveIconDrawable){
            launchIcon.foreground.constantState!!.newDrawable().mutate().apply {
                setTint(monetAccent)
            }
        }else launchIcon
        return SplashScreen.MonetBackground(monetBackground, foregroundIcon)
    }

    /**
     *  Inflates [R.layout.splash] and sets it up for a given Splash type. If the Splash type returns null,
     *  it will be set up as DEFAULT
     */
    override suspend fun inflateSplashScreen(context: Context, splashScreenType: SplashScreenType, packageName: String, parent: ViewGroup?, isPreview: Boolean): View = withContext(Dispatchers.Main) {
        val splashScreen = withContext(Dispatchers.IO) {
            splashScreenType.toSplashScreen(context, packageName) ?: getDefaultSplashForPackage(context, packageName)
        }
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val splashScreenView = SplashBinding.inflate(layoutInflater, parent, false)

        //Set up background
        splashScreenView.root.background = when(splashScreen){
            is SplashScreen.Native -> ColorDrawable(splashScreen.backgroundColor)
            is SplashScreen.Core -> ColorDrawable(splashScreen.backgroundColor)
            is SplashScreen.WindowBackground -> splashScreen.backgroundDrawable
            is SplashScreen.IconBackground -> ColorDrawable(splashScreen.backgroundColor)
            is SplashScreen.MonetBackground -> ColorDrawable(splashScreen.backgroundColor)
            is SplashScreen.Transparent -> {
                if(isPreview){
                    getWallpaperDrawable(context)
                }else{
                    ColorDrawable(Color.TRANSPARENT)
                }
            }
        }
        //Set up icon
        splashScreenView.splashIcon.setImageDrawable(when(splashScreen){
            is SplashScreen.Native -> splashScreen.iconDrawable
            is SplashScreen.Core -> splashScreen.iconDrawable
            is SplashScreen.IconBackground -> splashScreen.iconDrawable
            is SplashScreen.MonetBackground -> splashScreen.iconDrawable
            else -> ColorDrawable(Color.TRANSPARENT)
        })
        //Set up icon background
        splashScreenView.splashIcon.background = when(splashScreen){
            is SplashScreen.Native -> ColorDrawable(splashScreen.splashIconBackgroundColor ?: Color.TRANSPARENT)
            //TODO add support for Core when it supports icon background
            else -> ColorDrawable(Color.TRANSPARENT)
        }
        //Set up branding
        splashScreenView.splashBranding.setImageDrawable(when(splashScreen){
            is SplashScreen.Native -> splashScreen.splashBrandingImage
            //TODO add support for Core when it supports branding image
            else -> ColorDrawable(Color.TRANSPARENT)
        })
        return@withContext splashScreenView.root
    }

    @SuppressLint("MissingPermission")
    private fun getWallpaperDrawable(context: Context): Drawable {
        val wallpaperManager = context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
        return try {
            wallpaperManager.drawable ?: wallpaperManager.builtInDrawable
        } catch (e: SecurityException){
            //Not worth asking for the permission just for this
            wallpaperManager.builtInDrawable
        }
    }

    override suspend fun inflateSplashScreenIntoBitmap(
        context: Context,
        splashScreenType: SplashScreenType,
        packageName: String
    ): Bitmap {
        return withContext(Dispatchers.IO) {
            val width = Resources.getSystem().displayMetrics.widthPixels
            val height = Resources.getSystem().displayMetrics.heightPixels
            val splashView = inflateSplashScreen(
                context,
                splashScreenType,
                packageName,
                isPreview = true
            ).also {
                it.layoutParams = LinearLayout.LayoutParams(width, height)
                it.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                it.layout(0, 0, width, height)
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            splashView.draw(canvas)
            return@withContext bitmap
        }
    }

    /**
     *  Use Palette to extract the dominant color from a drawable, which must be a [BitmapDrawable], or `null` will be returned.
     */
    private fun Drawable.getDominantColor(context: Context): Int? {
        if(this is ColorDrawable) return color
        //Only supporting BitmapDrawable for now
        if(this !is BitmapDrawable) return null
        return Palette.from(this.bitmap).generate().getDominantColor(if(context.isDarkMode) Color.BLACK else Color.WHITE)
    }

    /**
     *  Get a drawable from a TypedValue, supports either loading a drawable from a resourceId,
     *  or using the data value to create a ColorDrawable if it's a color type.
     */
    private fun TypedValue.getDrawable(context: Context): Drawable? {
        return when {
            isColorType -> {
                ColorDrawable(data)
            }
            resourceId != 0 -> {
                ContextCompat.getDrawable(context, resourceId)
            }
            else -> null
        }
    }

    @ColorInt
    private fun TypedValue.getColorOrNull(context: Context, allowTransparent: Boolean = true): Int? {
        return if(isColorType){
            return if(resourceId != 0 && context.resources.getResourceTypeName(resourceId) == "color"){
                if(!allowTransparent && resourceId == android.R.color.transparent) return null
                ContextCompat.getColor(context, resourceId)
            }else null
        }else null
    }

    /**
     *  Create a context for a specified package, gets the launch component for it and applies
     *  its or the application's theme. This can then be used to load attributes.
     */
    private fun getThemedContextForPackage(context: Context, packageName: String): Context? {
        val packageManager = context.packageManager
        val remoteContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        val launchComponent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val themeResource = packageManager.getActivityInfo(launchComponent.resolveActivity(packageManager), 0).themeResource
        return if(themeResource != 0){
            remoteContext.setTheme(themeResource)
            remoteContext
        }else null
    }

    /**
     *  Create a context for a specified package, gets the launch component for it and applies
     *  its or the application's theme. This can then be used to load attributes.
     */
    private fun getThemedContextForPackageApplication(context: Context, packageName: String): Context? {
        val packageManager = context.packageManager
        val remoteContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        val themeResource = packageManager.getApplicationInfo(packageName, 0).theme
        return if(themeResource != 0){
            remoteContext.setTheme(themeResource)
            remoteContext
        }else null
    }

    /**
     *  Cleanly resolve an attribute, returning null if it's not found.
     */
    private fun Resources.Theme.getResolvedAttribute(@AttrRes attribute: Int): TypedValue? {
        val typedValue = TypedValue().apply {
            resolveAttribute(attribute, this, true)
        }
        return if(typedValue.type == TypedValue.TYPE_NULL) null
        else typedValue
    }

}