package com.domanskii.github

import com.domanskii.dao.DAOFacadeImpl
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.text.DecimalFormat
import kotlin.math.absoluteValue

private val log = KotlinLogging.logger {}

data class OutputRow(
    val name: String, val value: Double, val units: String, val sign: String, val diff: Double?, val symbol: String
)

class GitHub(
    token: String, private val db: DAOFacadeImpl, repo: String, private val defaultBranch: String
) {
    private val PLACEHOLDER_REGEX = "(<!-- PR-METRICS-PUBLISHER:.*?-->.*)([\\s\\S]*)".toRegex()
    private val gh: GitHub = GitHubBuilder().withOAuthToken(token).build()
    private val repo = repo.trim()

    fun isMergeToMasterCommit(commitSha: String): Boolean {
        log.debug { "isMergeToMasterCommit() for ${commitSha.substring(0, 7)} commit..." }

        // Check if commit belongs to merged PR to default branch
        val prList = gh.getRepository(repo).getCommit(commitSha).listPullRequests().toList().filter {
            it.isMerged && it.base.ref == defaultBranch
        }

        return prList.isNotEmpty()
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

            val diff = when {
                pair.second != null -> decimalFormat.format(pair.first.value - pair.second!!.value).toDouble()
                else -> null
            }

            val sign = when {
                diff != null && diff > 0 -> "+"
                diff != null && diff < 0 -> "-"
                else -> ""
            }

            val symbol = when {
                (diff != null && diff > 0 && pair.first.isIncreaseBad) -> "\uD83D\uDD3A"
                (diff != null && diff < 0 && !pair.first.isIncreaseBad) -> "\uD83D\uDD3B"
                else -> ""
            }

            OutputRow(name, value, units, sign, diff, symbol)
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
                it.diff != null -> "${it.sign} ${it.diff.absoluteValue } ${it.units} ${it.symbol}"
                else -> "no ref"
            }

            "| ${it.name} | ${it.value} ${it.units} | $diffString |"
        }.joinToString("\n")

        val table = tableHeader.plus(tableRows)

        prList.forEach { pr ->
            log.debug { "Updating #${pr.id} PR body for ${commitSha.substring(0, 7)} commit" }
            val body = pr.body

            if (body.isBlank()) {
                log.info { "PR #${pr.id} doesn't have a body!" }
                return
            }
            if (!PLACEHOLDER_REGEX.containsMatchIn(body)) {
                log.info { "PR #${pr.id} doesn't have the placeholder!" }
                return
            }

            val result = body.replace(PLACEHOLDER_REGEX) { matchResult ->
                matchResult.groupValues[1] + "\n" + table
            }
            pr.body = result
        }
    }
}
