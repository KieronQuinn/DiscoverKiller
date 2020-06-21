package com.kieronquinn.app.discoverkiller

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.kieronquinn.app.discoverkiller.settings.SettingsActivity
import com.kieronquinn.app.discoverkiller.settings.SharedPrefsProvider
import com.kieronquinn.app.discoverkiller.settings.holders.DiscoverBehaviour
import java.io.File


fun runAfter(delayMillis: Long, method: () -> Unit){
    Handler().postDelayed({
        method.invoke()
    }, delayMillis)
}

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

const val KEY_DISCOVER_BEHAVIOUR = "discover_behaviour"
const val KEY_CUSTOM_APP = "custom_app"
const val KEY_CUSTOM_APP_LAUNCH_COMPONENT = "custom_app_component"
const val KEY_SWIPE_CLOSE = "swipe_close"

fun Context.getSelectedBehaviour(): DiscoverBehaviour {
    val sharedPreferences = getSharedPreferences(packageName + "_prefs", Context.MODE_PRIVATE)
    return DiscoverBehaviour.valueOf(sharedPreferences.getString(KEY_DISCOVER_BEHAVIOUR, DiscoverBehaviour.UPDATES.name)!!)
}

fun Context.setSelectedBehaviour(discoverBehaviour: DiscoverBehaviour) {
    val sharedPreferences = getSharedPreferences(packageName + "_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString(KEY_DISCOVER_BEHAVIOUR, discoverBehaviour.name).commit()
    setSharedPrefsPublic(packageName + "_prefs")
}

fun Context.setSharedPrefsPublic(prefFileName: String) {
    val file = File(Environment.getDataDirectory(), "data/$packageName/shared_prefs/$prefFileName.xml")
    file.setReadable(true, false)
}

fun Context.getCustomApp(): String {
    val sharedPreferences = getSharedPreferences(packageName + "_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString(KEY_CUSTOM_APP, "")
}

fun Context.setCustomApp(customApp: String) {
    val sharedPreferences = getSharedPreferences(packageName + "_prefs", Context.MODE_PRIVATE)
    val launchComponent = packageManager.getLaunchIntentForPackage(customApp).component.className
    sharedPreferences.edit().putString(KEY_CUSTOM_APP, customApp).putString(KEY_CUSTOM_APP_LAUNCH_COMPONENT, launchComponent).commit()
    setSharedPrefsPublic(packageName + "_prefs")
}

fun Fragment.getNavController(): NavController? {
    return (activity as? SettingsActivity)?.findNavController(R.id.nav_host_fragment)
}

fun Fragment.setToolbarElevationEnabled(enabled: Boolean) {
    (activity as? SettingsActivity)?.setToolbarElevationEnabled(enabled)
}

fun String.concat(length: Float): String {
    return TextUtils.ellipsize(this, TextPaint(), length, TextUtils.TruncateAt.END).toString()
}

fun Context.getIsSwipeEnabled(): Boolean {
    val sharedPreferences = getSharedPreferences(packageName + "_prefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean(KEY_SWIPE_CLOSE, false)
}

fun Context.setSwipeEnabled(enabled: Boolean) {
    val sharedPreferences = getSharedPreferences(packageName + "_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putBoolean(KEY_SWIPE_CLOSE, enabled).commit()
    setSharedPrefsPublic(packageName + "_prefs")
}

//Following methods based off https://code.highspec.ru/Mikanoshi/CustoMIUIzer
fun stringPrefToUri(name: String, defValue: String): Uri {
    return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/string/" + name + "/" + defValue)
}

fun intPrefToUri(name: String, defValue: Int): Uri {
    return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/integer/" + name + "/" + defValue.toString())
}

fun boolPrefToUri(name: String, defValue: Boolean): Uri {
    return Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/boolean/" + name + "/" + if (defValue) '1' else '0')
}

fun getSharedStringPref(context: Context, name: String, defValue: String): String? {
    val uri: Uri = stringPrefToUri(name, defValue)
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    return if (cursor != null) {
        cursor.moveToFirst()
        val prefValue: String = cursor.getString(0)
        cursor.close()
        prefValue
    } else null
}

fun getSharedIntPref(context: Context, name: String, defValue: Int): Int {
    val uri: Uri = intPrefToUri(name, defValue)
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    return if (cursor != null) {
        cursor.moveToFirst()
        val prefValue: Int = cursor.getInt(0)
        cursor.close()
        prefValue
    } else defValue
}

fun getSharedBoolPref(context: Context, name: String, defValue: Boolean): Boolean {
    val uri: Uri = boolPrefToUri(name, defValue)
    Log.d("XDiscoverKiller", "getSharedBoolPref ${uri.toString()}")
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    return if (cursor != null) {
        cursor.moveToFirst()
        val prefValue: Int = cursor.getInt(0)
        cursor.close()
        prefValue == 1
    } else defValue
}