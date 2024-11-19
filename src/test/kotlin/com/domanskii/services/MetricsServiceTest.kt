package com.domanskii.services

import com.domanskii.common.Metric
import com.domanskii.common.MetricDiff
import com.domanskii.providers.VcsProvider
import com.domanskii.storage.Storage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class MetricsServiceTest {

    private val storage: Storage = mock()
    private val vcsProvider: VcsProvider = mock()
    private val markdownService: MarkdownService = mock()
    private val metricsService = MetricsService(storage, vcsProvider, markdownService)

    @Test
    fun `processMetric should store metric and not publish when commit is reference commit`() = runBlocking {
        // Arrange
        val commitSha = "commitRef123"
        val metric = Metric(commitSha, "test_metric", 10.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        whenever(vcsProvider.isReferenceCommit(commitSha)).thenReturn(true)

        // Act
        metricsService.processMetric(commitSha, metric.name, metric.value, metric.units, metric.threshold, metric.isIncreaseBad)

        // Assert
        verify(storage).submitMetric(metric)
        verify(vcsProvider, times(0)).publishMetrics(any(), any())
        verifyNoInteractions(markdownService)
    }

    @Test
    fun `processMetric should store metric and publish when commit is not reference commit and reference exists`() = runBlocking {
        // Arrange
        val commitSha = "commitNonRef123"
        val metric = Metric(commitSha, "test_metric", 15.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val referenceMetric = Metric("commitRef456", "test_metric", 10.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        val metricDiff = MetricDiff(metric, referenceMetric)
        val mdText = "Generated Markdown"

        whenever(vcsProvider.isReferenceCommit(commitSha)).thenReturn(false)
        whenever(storage.submitMetric(metric)).thenReturn(metric)
        whenever(storage.getMetricsForCommit(commitSha)).thenReturn(listOf(metric))
        whenever(storage.getReferenceForMetric(metric.name)).thenReturn(referenceMetric)
        whenever(markdownService.generateMetricsTable(listOf(metricDiff))).thenReturn(mdText)

        // Act
        metricsService.processMetric(commitSha, metric.name, metric.value, metric.units, metric.threshold, metric.isIncreaseBad)

        // Assert
        verify(storage).submitMetric(metric)
        verify(storage).getMetricsForCommit(commitSha)
        verify(storage).getReferenceForMetric(metric.name)
        verify(markdownService).generateMetricsTable(listOf(metricDiff))
        verify(vcsProvider).publishMetrics(commitSha, mdText)
    }

    @Test
    fun `processMetric should store metric and publish when commit is not reference commit and reference does not exist`() = runBlocking {
        // Arrange
        val commitSha = "commitNonRef123"
        val metric = Metric(commitSha, "new_metric", 20.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val metricDiff = MetricDiff(metric, null)
        val mdText = "Generated Markdown for new metric"

        whenever(vcsProvider.isReferenceCommit(commitSha)).thenReturn(false)
        whenever(storage.submitMetric(metric)).thenReturn(metric)
        whenever(storage.getMetricsForCommit(commitSha)).thenReturn(listOf(metric))
        whenever(storage.getReferenceForMetric(metric.name)).thenReturn(null)
        whenever(markdownService.generateMetricsTable(listOf(metricDiff))).thenReturn(mdText)

        // Act
        metricsService.processMetric(commitSha, metric.name, metric.value, metric.units, metric.threshold, metric.isIncreaseBad)

        // Assert
        verify(storage).submitMetric(metric)
        verify(storage).getMetricsForCommit(commitSha)
        verify(storage).getReferenceForMetric(metric.name)
        verify(markdownService).generateMetricsTable(listOf(metricDiff))
        verify(vcsProvider).publishMetrics(commitSha, mdText)
    }
}
