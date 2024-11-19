package com.domanskii.services

import com.domanskii.common.Metric
import com.domanskii.common.MetricDiff
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarkdownServiceTest {

    private val markdownService = MarkdownService()

    @Test
    fun `generateMetricsTable with empty list should return header only`() {
        val result = markdownService.generateMetricsTable(emptyList())

        val expected = """#### ✅ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
"""

        assertEquals(expected, result)
    }

    @Test
    fun `generateMetricsTable with single metric should return correct table`() {
        val metrics = listOf(
            MetricDiff(
                Metric("sha1", "test_metric", 10.0, "ms", 5.0, false, true), null
            )
        )
        val result = markdownService.generateMetricsTable(metrics)

        val expected = """#### ✅ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Test Metric | 10.0 ms | NEW METRIC |"""

        assertEquals(expected, result)
    }

    @Test
    fun `generateMetricsTable with multiple metrics should return sorted table`() {
        val metrics = listOf(
            MetricDiff(
                Metric("sha1", "cpu_usage", 30.0, "%", 5.0, false, true),
                Metric("sha1", "cpu_usage", 20.0, "%", 5.0, false, true)
            ), MetricDiff(
                Metric("sha1", "performance", 50.0, "%", 25.0, false, false),
                Metric("sha1", "performance", 100.0, "%", 25.0, false, false)
            )
        )
        val result = markdownService.generateMetricsTable(metrics)

        val expected = """#### ⚠️ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Cpu Usage | 30.0 % | + 10.0 % 🔺 |
| Performance | 50.0 % | - 50.0 % 🔻 |"""

        assertEquals(expected, result)
    }

    @Test
    fun `generateMetricsTable with metric having no difference should return correct table`() {
        val metrics = listOf(
            MetricDiff(
                Metric("sha1", "memory_usage", 500.0, "MB", 100.0, false, true),
                Metric("sha1", "memory_usage", 500.0, "MB", 100.0, false, true)
            )
        )
        val result = markdownService.generateMetricsTable(metrics)

        val expected = """#### ✅ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Memory Usage | 500.0 MB |  0.0 MB  |"""

        assertEquals(expected, result)
    }

    @Test
    fun `generateMetricsTable with metric exceeding threshold and increase is bad should return table with up arrow`() {
        val metrics = listOf(
            MetricDiff(
                Metric("sha1", "load_average", 15.0, "", 10.0, false, true),
                Metric("sha1", "load_average", 5.0, "", 10.0, false, true)
            )
        )
        val result = markdownService.generateMetricsTable(metrics)

        val expected = """#### ⚠️ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Load Average | 15.0  | + 10.0  🔺 |"""

        assertEquals(expected, result)
    }

    @Test
    fun `generateMetricsTable with metric below threshold and increase is good should return table without symbol`() {
        val metrics = listOf(
            MetricDiff(
                Metric("sha1", "test_pass_rate", 100.0, "%", 5.0, false, false),
                Metric("sha1", "test_pass_rate", 90.0, "%", 5.0, false, false)
            )
        )
        val result = markdownService.generateMetricsTable(metrics)

        val expected = """#### ✅ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Test Pass Rate | 100.0 % | + 10.0 %  |"""

        assertEquals(expected, result)
    }

    @Test
    fun `generateMetricsTable with metric exceeding threshold and decrease is bad should return table with down arrow`() {
        val metrics = listOf(
            MetricDiff(
                Metric("sha1", "battery_life", 2.0, "hrs", 1.0, false, false),
                Metric("sha1", "battery_life", 4.0, "hrs", 1.0, false, false)
            )
        )
        val result = markdownService.generateMetricsTable(metrics)

        val expected = """#### ⚠️ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Battery Life | 2.0 hrs | - 2.0 hrs 🔻 |"""

        assertEquals(expected, result)
    }

    @Test
    fun `generateMetricsTable with metric having negative difference below threshold should return table without symbol`() {
        val metrics = listOf(
            MetricDiff(
                Metric("sha1", "error_count", 3.0, "errors", 5.0, false, true),
                Metric("sha1", "error_count", 5.0, "errors", 5.0, false, true)
            )
        )
        val result = markdownService.generateMetricsTable(metrics)

        val expected = """#### ✅ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Error Count | 3.0 errors | - 2.0 errors  |"""

        assertEquals(expected, result)
    }
}
