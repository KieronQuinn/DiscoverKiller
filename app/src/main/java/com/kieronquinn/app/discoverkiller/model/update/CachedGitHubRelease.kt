package com.kieronquinn.app.discoverkiller.model.update

import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.discoverkiller.model.github.GitHubRelease

data class CachedGitHubRelease(
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("release")
    val release: GitHubRelease
)