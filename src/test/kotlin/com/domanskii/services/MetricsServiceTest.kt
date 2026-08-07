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

    @Test
    fun `refreshAll should refresh every open PR's head commit`() = runBlocking {
        // Arrange
        val commitSha1 = "commitOpen111"
        val commitSha2 = "commitOpen222"
        val metric1 = Metric(commitSha1, "test_metric", 10.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val metric2 = Metric(commitSha2, "test_metric", 12.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val mdText = "Generated Markdown"

        whenever(vcsProvider.getOpenPullRequestHeadCommits()).thenReturn(listOf(commitSha1, commitSha2))
        whenever(vcsProvider.isReferenceCommit(commitSha1)).thenReturn(false)
        whenever(vcsProvider.isReferenceCommit(commitSha2)).thenReturn(false)
        whenever(storage.getMetricsForCommit(commitSha1)).thenReturn(listOf(metric1))
        whenever(storage.getMetricsForCommit(commitSha2)).thenReturn(listOf(metric2))
        whenever(storage.getReferenceForMetric("test_metric")).thenReturn(null)
        whenever(markdownService.generateMetricsTable(any())).thenReturn(mdText)

        // Act
        metricsService.refreshAll()

        // Assert
        verify(vcsProvider).getOpenPullRequestHeadCommits()
        verify(vcsProvider).publishMetrics(commitSha1, mdText)
        verify(vcsProvider).publishMetrics(commitSha2, mdText)
    }

    @Test
    fun `refreshAll should skip reference commits and do nothing when there are no open PRs`() = runBlocking {
        // Arrange
        whenever(vcsProvider.getOpenPullRequestHeadCommits()).thenReturn(emptyList())

        // Act
        metricsService.refreshAll()

        // Assert
        verify(vcsProvider).getOpenPullRequestHeadCommits()
        verifyNoInteractions(storage)
        verifyNoInteractions(markdownService)
        verify(vcsProvider, never()).publishMetrics(any(), any())
    }
}
