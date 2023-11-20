package com.domanskii.dao

import com.domanskii.models.Metric

interface DAOFacade {
    suspend fun postMetric(
        commitSha: String,
        name: String,
        value: Double,
        units: String,
        isReference: Boolean,
        isIncreaseBad: Boolean
    ): Metric?

    suspend fun getMetricsForCommit(commitSha: String): List<Metric>

    suspend fun getReferenceForMetric(name: String): Metric?
}