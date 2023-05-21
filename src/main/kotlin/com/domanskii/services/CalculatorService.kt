package com.domanskii.services

import com.domanskii.common.Metric
import mu.KotlinLogging
import java.text.DecimalFormat

private val log = KotlinLogging.logger {}

class CalculatorService {
    fun getMetricsDiff(metrics: List<Pair<Metric, Metric?>>): List<Triple<Metric, Metric?, Double>> {
        log.debug { "getMetricsDiff()" }

        return metrics.map { pair ->
            val decimalFormat = DecimalFormat("#.##")

            val diff = decimalFormat.format(pair.first.value - (pair.second?.value ?: 0.0)).toDouble()

            Triple(pair.first, pair.second, diff)
        }.sortedBy { it.first.name }
    }
}