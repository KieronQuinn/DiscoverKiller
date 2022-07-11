package com.kieronquinn.app.discoverkiller.repositories

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.repositories.AppRepository.App
import com.kieronquinn.app.discoverkiller.utils.extensions.toComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

interface AppRepository {

    suspend fun getLaunchableApps(): List<App>
    fun getActivityName(component: String): CharSequence

    data class App(
        val componentName: ComponentName,
        val label: CharSequence,
        val shouldShowPackageName: Boolean
    )

}

class AppRepositoryImpl(context: Context) : AppRepository {

    companion object {
        private val LAUNCH_INTENT = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
    }

    private val packageManager = context.packageManager
    private val resources = context.resources

    override suspend fun getLaunchableApps() = withContext(Dispatchers.IO) {
        val activities = packageManager.queryIntentActivities(LAUNCH_INTENT, 0).map {
            Pair(it.activityInfo.toComponent(), it.loadLabel(packageManager).trim())
        }
        activities.map { activity ->
            //Show Package Name if there is more than one of this label in the list
            val shouldShowPackageName = activities.count { activity.second == it.second } > 1
            App(activity.first, activity.second, shouldShowPackageName)
        }.sortedBy { it.label.toString().lowercase() }
    }

    override fun getActivityName(component: String): CharSequence {
        val componentName = try {
            ComponentName.unflattenFromString(component)
        }catch (e: Exception){
            null
        } ?: return resources.getString(R.string.overlay_unset)
        val intent = Intent(LAUNCH_INTENT).apply {
            this.component = componentName
        }
        val info = packageManager.queryIntentActivities(intent, 0)
        if(info.isEmpty()) return resources.getString(R.string.overlay_unset)
        return info.first().loadLabel(packageManager)
    }

}