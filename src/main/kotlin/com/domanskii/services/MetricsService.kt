package com.domanskii.services

import com.domanskii.common.Metric
import com.domanskii.storage.Storage
import com.domanskii.providers.Provider
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class MetricsService(
    private val storage: Storage,
    private val provider: Provider,
    private val markdownService: MarkdownService,
    private val calculatorService: CalculatorService
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

        val isReference = provider.isReferenceCommit(commitSha)
        val metric = Metric(commitSha, name, value, units, threshold, isReference, isIncreaseBad)
        storage.submitMetric(metric)

        if (isReference) return

        // get all metrics for commit
        val allMetrics = storage.getMetricsForCommit(metric.commitSha)
        log.debug { "${allMetrics.size} metrics found for ${metric.commitSha.substring(0, 7)} commit" }

        // get reference version for each metric
        val metricsWithReferences = allMetrics.map {
            Pair(it, storage.getReferenceForMetric(it.name))
        }
        log.debug {
            "${metricsWithReferences.filter { it.second != null }.size} metric references found for ${
                metric.commitSha.substring(0, 7)
            } commit"
        }

        val metricsWithDiffs = calculatorService.getMetricsDiff(metricsWithReferences)
        val mdText = markdownService.generateMetricsTable(metricsWithDiffs)
        provider.publishMetrics(metric.commitSha, mdText)
    }
}