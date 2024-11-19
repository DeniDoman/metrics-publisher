package com.domanskii.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.text.DecimalFormat

class MetricDiffTest {

    private val decimalFormat = DecimalFormat("#.##")

    @Test
    fun `test diff when reference is null`() {
        val actualMetric = Metric("commitSha1", "metricName", 10.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val metricDiff = MetricDiff(actualMetric, null)

        assertEquals(null, metricDiff.diff)
    }

    @Test
    fun `test diff when reference is not null and actual value is greater`() {
        val actualMetric = Metric("commitSha1", "metricName", 15.5, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val referenceMetric = Metric("commitSha2", "metricName", 10.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        val metricDiff = MetricDiff(actualMetric, referenceMetric)

        assertEquals(5.5, metricDiff.diff)
    }

    @Test
    fun `test diff when reference is not null and actual value is less`() {
        val actualMetric = Metric("commitSha1", "metricName", 7.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val referenceMetric = Metric("commitSha2", "metricName", 10.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        val metricDiff = MetricDiff(actualMetric, referenceMetric)

        assertEquals(-3.0, metricDiff.diff)
    }

    @Test
    fun `test diff when reference is not null and values are equal`() {
        val actualMetric = Metric("commitSha1", "metricName", 10.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val referenceMetric = Metric("commitSha2", "metricName", 10.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        val metricDiff = MetricDiff(actualMetric, referenceMetric)

        assertEquals(0.0, metricDiff.diff)
    }

    @Test
    fun `test diff with decimal precision`() {
        val actualMetric = Metric("commitSha1", "metricName", 10.123, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val referenceMetric = Metric("commitSha2", "metricName", 10.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        val metricDiff = MetricDiff(actualMetric, referenceMetric)

        assertEquals(decimalFormat.format(0.123).toDouble(), metricDiff.diff)
    }
}
