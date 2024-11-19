package com.domanskii.services

import com.domanskii.common.MetricDiff
import mu.KotlinLogging
import kotlin.math.absoluteValue

private val log = KotlinLogging.logger {}

data class OutputRow(
    val name: String, val value: Double, val units: String, val sign: String, val diff: String, val symbol: String
)

class MarkdownService {
    fun generateMetricsTable(metrics: List<MetricDiff>): String {
        log.debug { "generateMetricsTable()" }

        val outputRows = metrics.map { metric ->
            val name = metric.actual.name.split("_").map { word -> word.replaceFirstChar { c -> c.uppercase() } }
                .joinToString(" ")

            val value = metric.actual.value
            val units = metric.actual.units
            val diff = metric.diff

            val sign = when {
                diff == null -> ""
                diff > 0 -> "+"
                diff < 0 -> "-"
                else -> ""
            }

            val indicator = when {
                (diff == null) -> ""
                (diff > 0 && diff.absoluteValue >= metric.actual.threshold && metric.actual.isIncreaseBad) -> "\uD83D\uDD3A" // Red Triangle Pointed Up
                (diff < 0 && diff.absoluteValue >= metric.actual.threshold && !metric.actual.isIncreaseBad) -> "\uD83D\uDD3B" // Red Triangle Pointed Down
                else -> ""
            }

            val diffValue = when {
                diff == null -> ""
                else -> diff.absoluteValue.toString()
            }

            OutputRow(name, value, units, sign, diffValue, indicator)
        }.sortedBy { it.name }

        val headerSymbol = when {
            outputRows.any { it.symbol.isNotEmpty() } -> "⚠️"
            else -> "✅"
        }

        val tableHeader = """#### $headerSymbol PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
"""

        val tableRows = outputRows.map {
            val diffString = when {
                it.diff.isEmpty() -> "NEW METRIC"
                else -> "${it.sign} ${it.diff} ${it.units} ${it.symbol}"
            }

            "| ${it.name} | ${it.value} ${it.units} | $diffString |"
        }.joinToString("\n")

        return tableHeader.plus(tableRows)
    }
}