package com.domanskii.providers

interface VcsProvider {
    fun isReferenceCommit(commitSha: String): Boolean
    suspend fun publishMetrics(commitSha: String, mdText: String)
}
