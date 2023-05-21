package com.domanskii.storage

import com.domanskii.common.Metric

interface Storage {
    suspend fun submitMetric(
        metric: Metric
    ): Metric?

    suspend fun getMetricsForCommit(commitSha: String): List<Metric>

    suspend fun getReferenceForMetric(name: String): Metric?
}