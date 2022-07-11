package com.kieronquinn.app.discoverkiller.components.notifications

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import com.kieronquinn.app.discoverkiller.R
import android.app.NotificationChannel as AndroidNotificationChannel

fun Context.createNotification(
    channel: NotificationChannel,
    builder: (NotificationCompat.Builder) -> Unit
): Notification {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationChannel =
        AndroidNotificationChannel(
            channel.id,
            getString(channel.titleRes),
            channel.importance
        ).apply {
            description = getString(channel.descRes)
        }
    notificationManager.createNotificationChannel(notificationChannel)
    return NotificationCompat.Builder(this, channel.id).apply(builder).build()
}

fun Activity.requestNotificationPermissionIfNeeded() {
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return //Not needed
    val isGranted = ContextCompat.checkSelfPermission(
        this, Manifest.permission.POST_NOTIFICATIONS
    )
    if(isGranted != PackageManager.PERMISSION_GRANTED) {
        //We don't actually care about the result - if it's denied then they just won't get updates
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }
}

enum class NotificationChannel(
    val id: String,
    val importance: Int,
    val titleRes: Int,
    val descRes: Int
) {
    UPDATES (
        "updates",
        NotificationManager.IMPORTANCE_HIGH,
        R.string.notification_channel_updates_title,
        R.string.notification_channel_updates_subtitle
    )
}

enum class NotificationId {
    UPDATES
}