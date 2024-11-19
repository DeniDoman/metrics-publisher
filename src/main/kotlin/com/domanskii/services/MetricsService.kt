package com.domanskii.services

import com.domanskii.common.Metric
import com.domanskii.common.MetricDiff
import com.domanskii.providers.VcsProvider
import com.domanskii.storage.Storage
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class MetricsService(
    private val storage: Storage,
    private val vcsProvider: VcsProvider,
    private val markdownService: MarkdownService,
) {
    suspend fun processMetric(
        commitSha: String,
        name: String,
        value: Double,
        units: String,
        threshold: Double,
        isIncreaseBad: Boolean
    ) {
        log.debug { "processMetric()" }

        val isReference = vcsProvider.isReferenceCommit(commitSha)
        val metric = Metric(commitSha, name, value, units, threshold, isReference, isIncreaseBad)
        storage.submitMetric(metric)

        if (isReference) return

        // get all metrics for commit
        val allMetrics = storage.getMetricsForCommit(metric.commitSha)
        log.debug { "${allMetrics.size} metrics found for ${metric.commitSha.take(7)} commit" }

        // get a reference version for each metric
        val metricsWithReferences = allMetrics.map {
            MetricDiff(it, storage.getReferenceForMetric(it.name))
        }
        log.debug {
            "${metricsWithReferences.filter { it.reference != null }.size} metric references found for ${
                metric.commitSha.take(7)
            } commit"
        }

        val mdText = markdownService.generateMetricsTable(metricsWithReferences)
        vcsProvider.publishMetrics(metric.commitSha, mdText)
    }
}