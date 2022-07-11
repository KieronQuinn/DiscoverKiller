package com.kieronquinn.app.discoverkiller.work

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.components.notifications.NotificationChannel
import com.kieronquinn.app.discoverkiller.components.notifications.NotificationId
import com.kieronquinn.app.discoverkiller.components.notifications.createNotification
import com.kieronquinn.app.discoverkiller.repositories.UpdateRepository
import com.kieronquinn.app.discoverkiller.ui.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.LocalDateTime

class UpdateWorker(
    private val context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams), KoinComponent {

    companion object {
        private const val TAG = "update_check"

        fun queueWorker(context: Context){
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(TAG)
            val now = LocalDateTime.now()
            val updateTime = if(now.hour >= 12) {
                now.plusDays(1).withHour(12).withMinute(0).withSecond(0)
            }else{
                now.withHour(12).withMinute(0).withSecond(0)
            }
            val delay = Duration.between(now, updateTime)
            workManager.enqueue(OneTimeWorkRequest.Builder(UpdateWorker::class.java).apply {
                addTag(TAG)
                setInitialDelay(delay)
            }.build())
        }
    }

    private val updatesRepository by inject<UpdateRepository>()

    override fun doWork(): Result {
        GlobalScope.launch {
            checkForUpdates()
        }
        return Result.success()
    }

    private suspend fun checkForUpdates() = withContext(Dispatchers.IO) {
        if(updatesRepository.isAnyUpdateAvailable()){
            context.showUpdateNotification()
        }
        queueWorker(context)
    }

    private fun Context.showUpdateNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val notification = createNotification(NotificationChannel.UPDATES) {
            it.setContentTitle(getString(R.string.notification_update_title))
            it.setContentText(getString(R.string.notification_update_subtitle))
            it.setSmallIcon(R.drawable.ic_notification)
            it.setOngoing(false)
            it.setAutoCancel(true)
            it.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NotificationId.UPDATES.ordinal,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            it.setTicker(getString(R.string.notification_update_title))
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationId.UPDATES.ordinal, notification)
    }

}