package com.kieronquinn.app.discoverkiller.repositories

import android.content.Context
import com.google.gson.Gson
import com.kieronquinn.app.discoverkiller.BuildConfig
import com.kieronquinn.app.discoverkiller.model.github.GitHubRelease
import com.kieronquinn.app.discoverkiller.model.update.CachedGitHubRelease
import com.kieronquinn.app.discoverkiller.providers.GitHubProvider
import com.kieronquinn.app.discoverkiller.repositories.UpdateRepository.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.Instant

interface UpdateRepository {

    val containerCheckUpdatesBus: Flow<Long>

    fun getUpdatesFolder(context: Context): File
    fun clearUpdatesFolder(folder: File)

    suspend fun containerCheckForUpdates()

    suspend fun isAnyUpdateAvailable(): Boolean

    suspend fun getUpdateState(
        ignoreCache: Boolean = false
    ): UpdateState

    sealed class UpdateState {
        data class NotInstalled(
            val remoteVersion: String,
            val release: GitHubRelease
        ): UpdateState()
        object FailedToFetchUpdate: UpdateState()
        object FailedToFetchInitial: UpdateState()
        data class UpToDate(
            val localVersion: String
        ): UpdateState()
        data class UpdateAvailable(
            val localVersion: String,
            val remoteVersion: String,
            val release: GitHubRelease
        ): UpdateState()
    }

}

class UpdateRepositoryImpl(context: Context): UpdateRepository {

    companion object {
        private val CACHE_TIMEOUT = Duration.ofHours(12).toMillis()
    }

    private val packageManager = context.packageManager

    private val updatesCacheDir = File(context.cacheDir, "updates").apply {
        mkdirs()
    }

    private val gson = Gson()

    private val discoverKillerProvider = GitHubProvider.getGitHubProvider("DiscoverKiller")

    override val containerCheckUpdatesBus = MutableStateFlow(System.currentTimeMillis())

    override fun getUpdatesFolder(context: Context): File {
        return File(context.externalCacheDir, "updates").apply {
            mkdirs()
        }
    }

    override fun clearUpdatesFolder(folder: File) {
        if(!folder.exists()) return //Already deleted
        folder.listFiles()?.forEach { it.delete() }
    }

    override suspend fun containerCheckForUpdates() {
        containerCheckUpdatesBus.emit(System.currentTimeMillis())
    }

    override suspend fun getUpdateState(ignoreCache: Boolean): UpdateState = withContext(
        Dispatchers.IO
    ) {
        getRelease(BuildConfig.TAG_NAME, discoverKillerProvider, "DiscoverKiller", ignoreCache)
    }

    override suspend fun isAnyUpdateAvailable(): Boolean {
        return when (getUpdateState()) {
            is UpdateState.UpdateAvailable -> true
            is UpdateState.NotInstalled -> true
            else -> false
        }
    }

    private fun getRelease(
        localVersion: String?,
        provider: GitHubProvider,
        repository: String,
        ignoreCache: Boolean = false
    ): UpdateState {
        val cachedRelease = if(ignoreCache) null else getUpdateCache(repository)
        val remoteRelease = cachedRelease ?: provider.getCurrentRelease()?.also {
            it.cacheRelease(repository)
        }
        val remoteVersion = remoteRelease?.tag
        return when {
            localVersion != null && remoteRelease != null && remoteVersion != null &&
                    remoteVersion != localVersion -> {
                UpdateState.UpdateAvailable(
                    localVersion,
                    remoteVersion,
                    remoteRelease
                )
            }
            localVersion == null && remoteRelease !=  null && remoteVersion != null -> {
                UpdateState.NotInstalled(remoteVersion, remoteRelease)
            }
            localVersion != null && remoteVersion != null && localVersion == remoteVersion -> {
                UpdateState.UpToDate(localVersion)
            }
            remoteVersion == null && localVersion != null -> {
                UpdateState.FailedToFetchUpdate
            }
            else -> UpdateState.FailedToFetchInitial
        }
    }

    private fun GitHubProvider.getCurrentRelease(): GitHubRelease? {
        val releases = try {
            getReleases().execute().body()
        }catch (e: Exception){
            null
        } ?: return null
        return releases.firstOrNull()
    }

    private fun getUpdateCache(repository: String): GitHubRelease? {
        val file = File(updatesCacheDir, repository)
        if(!file.exists()) return null
        val cachedRelease = try {
            gson.fromJson(file.readText(), CachedGitHubRelease::class.java)
        }catch (e: Exception){
            null
        } ?: return null
        val cacheAge = Duration.between(
            Instant.ofEpochMilli(cachedRelease.timestamp), Instant.now()
        ).toMillis()
        if(cacheAge > CACHE_TIMEOUT) return null
        return cachedRelease.release
    }

    private fun GitHubRelease.cacheRelease(repository: String) {
        val file = File(updatesCacheDir, repository)
        val cachedRelease = gson.toJson(CachedGitHubRelease(System.currentTimeMillis(), this))
        file.writeText(cachedRelease)
    }

}