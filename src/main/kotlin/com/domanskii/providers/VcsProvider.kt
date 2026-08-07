package com.domanskii.providers

interface VcsProvider {
    suspend fun isReferenceCommit(commitSha: String): Boolean
    suspend fun publishMetrics(commitSha: String, mdText: String)
    suspend fun getOpenPullRequestHeadCommits(): List<String>
}
