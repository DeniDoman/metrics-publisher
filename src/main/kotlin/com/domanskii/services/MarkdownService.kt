package com.domanskii.services

import com.domanskii.common.Metric
import mu.KotlinLogging
import kotlin.math.absoluteValue

private val log = KotlinLogging.logger {}

data class OutputRow(
    val name: String, val value: Double, val units: String, val sign: String, val diff: Double, val symbol: String
)

class MarkdownService {
    fun generateMetricsTable(metrics: List<Triple<Metric, Metric?, Double>>): String {
        log.debug { "generateMetricsTable()" }

        val outputRows = metrics.map { triple ->
            val name = triple.first.name.split("_").map { word -> word.replaceFirstChar { c -> c.uppercase() } }
                .joinToString(" ")

            val value = triple.first.value
            val units = triple.first.units

            val sign = when {
                triple.third > 0 -> "+"
                triple.third < 0 -> "-"
                else -> ""
            }

            val diff = triple.third

            val symbol = when {
                (diff > 0 && diff.absoluteValue > triple.first.threshold && triple.first.isIncreaseBad) -> "\uD83D\uDD3A" // Up arrow
                (diff < 0 && diff.absoluteValue > triple.first.threshold && !triple.first.isIncreaseBad) -> "\uD83D\uDD3B" // Down arrow
                else -> ""
            }

            OutputRow(name, value, units, sign, diff.absoluteValue, symbol)
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
            val diffString = "${it.sign} ${it.diff} ${it.units} ${it.symbol}"
            return "| ${it.name} | ${it.value} ${it.units} | $diffString |"
        }.joinToString("\n")

        return tableHeader.plus(tableRows)
    }
}