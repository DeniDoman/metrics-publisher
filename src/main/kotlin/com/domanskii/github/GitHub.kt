package com.domanskii.github

import com.domanskii.dao.DAOFacadeImpl
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.text.DecimalFormat
import kotlin.math.absoluteValue

private val log = KotlinLogging.logger {}

data class OutputRow(val name: String, val value: Double, val units: String, val diff: Double, val symbol: String)

class GitHub(
    token: String, private val db: DAOFacadeImpl, private val repo: String, private val defaultBranch: String
) {
    private val PLACEHOLDER_REGEX = "(<!-- PR-METRICS-PUBLISHER:.*?-->.*)([\\s\\S]*)"
    private val gh: GitHub = GitHubBuilder().withOAuthToken(token).build()

    fun isMergeToMasterCommit(commitSha: String): Boolean {
        log.debug { "isMergeToMasterCommit() for ${commitSha.substring(0, 7)} commit..." }
        val defaultBranchSha = gh.getRepository(repo).getBranch(defaultBranch).shA1
        return gh.getRepository(repo)
            .getCommit(commitSha).parentSHA1s.let { it.size > 1 && it.contains(defaultBranchSha) }
    }

    suspend fun addMetricsToPrMessage(commitSha: String) {
        log.debug { "addMetricsToPrMessage() for ${commitSha.substring(0, 7)} commit..." }

        val prList = gh.getRepository(repo).getCommit(commitSha).listPullRequests().toList()
        if (prList.size == 0) {
            log.debug { "There are no PRs for ${commitSha.substring(0, 7)} commit..." }
            return
        }
        log.debug { "${prList.size} PRs found for ${commitSha.substring(0, 7)} commit" }

        // get all metrics for commit
        val metrics = db.getMetricsForCommit(commitSha)
        log.debug { "${metrics.size} metrics found for ${commitSha.substring(0, 7)} commit" }

        // get reference version for each metric
        val metricsWithReferences = metrics.map {
            Pair(it, db.getReferenceForMetric(it.name))
        }
        log.debug {
            "${metricsWithReferences.filter { it.second != null }.size} metric references found for ${
                commitSha.substring(0, 7)
            } commit"
        }

        // calculate diff
        val outputRows = metricsWithReferences.map { pair ->
            val name = pair.first.name.split("_").map { word -> word.replaceFirstChar { c -> c.uppercase() } }
                .joinToString(" ")

            val value = pair.first.value
            val units = pair.first.units

            val decimalFormat = DecimalFormat("#.##")
            // diff will be zero in case a Reference is absent
            val diff = decimalFormat.format(pair.first.value - (pair.second?.value ?: pair.first.value)).toDouble()

            val symbol = when {
                (diff > 0 && pair.first.isIncreaseBad) -> "\uD83D\uDD3A"
                (diff < 0 && !pair.first.isIncreaseBad) -> "\uD83D\uDD3B"
                else -> ""
            }

            OutputRow(name, value, units, diff, symbol)
        }

        val warnSymbol = when {
            outputRows.any { it.symbol.isNotEmpty() } -> "⚠"
            else -> ""
        }

        val tableHeader = """ ### $warnSymbol PR Metrics
            | Metric               | Value   | Diff        |
            |----------------------|---------|-------------|
        """

        val tableRows = outputRows.map {
            "| ${it.name} | ${it.value} ${it.units} | ${if (it.diff > 0) "+" else "-"} ${it.diff.absoluteValue} ${it.units} ${it.symbol} |"
        }.joinToString("\n")

        val table = tableHeader.plus(tableRows)

        prList.forEach { pr ->
            log.debug { "Updating ${pr.id} PR body for ${commitSha.substring(0, 7)} commit" }

            val result = pr.body.replace(Regex(PLACEHOLDER_REGEX)) { matchResult ->
                matchResult.groupValues[1] + table
            }
            pr.body = result
        }
    }
}
