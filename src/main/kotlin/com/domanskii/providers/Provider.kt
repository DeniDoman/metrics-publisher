package com.domanskii.providers

interface Provider {
    fun isReferenceCommit(commitSha: String): Boolean
    suspend fun publishMetrics(commitSha: String, mdText: String)
}
