package com.domanskii.providers

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class GitHub(
    repo: String, private val defaultBranch: String, private val ghApiClient: GitHubApiClient
): VcsProvider {
    private val placeholderRegex = "(<!-- PR-METRICS-PUBLISHER:.*?-->.*)([\\s\\S]*)".toRegex()
    private val repo = repo.trim()

    override fun isReferenceCommit(commitSha: String): Boolean {
        log.debug { "isReferenceCommit() for ${commitSha.substring(0, 7)} commit..." }

        // Check if commit belongs to merged PR to default branch
        val prList = ghApiClient.getRepository(repo).getCommit(commitSha).listPullRequests().toList().filter {
            it.isMerged && it.base.ref == defaultBranch
        }

        if (prList.isNotEmpty()) {
            log.debug { "${commitSha.substring(0, 7)} is a merge-to-$defaultBranch commit!" }
            return true
        } else {
            return false
        }
    }

    override suspend fun publishMetrics(commitSha: String, mdText: String) {
        log.debug { "publishMetrics() for ${commitSha.substring(0, 7)} commit..." }

        val prList = ghApiClient.getRepository(repo).getCommit(commitSha).listPullRequests().toList()
        if (prList.size == 0) {
            log.debug { "There are no PRs for ${commitSha.substring(0, 7)} commit..." }
            return
        }
        log.debug { "${prList.size} PRs found for ${commitSha.substring(0, 7)} commit" }

        prList.forEach { pr ->
            log.debug { "Updating #${pr.number} PR body for ${commitSha.substring(0, 7)} commit" }
            val body = pr.body

            if (body.isBlank()) {
                log.info { "PR #${pr.number} doesn't have a body!" }
                return
            }
            if (!placeholderRegex.containsMatchIn(body)) {
                log.info { "PR #${pr.number} doesn't have the placeholder!" }
                return
            }

            val result = body.replace(placeholderRegex) { matchResult ->
                matchResult.groupValues[1] + "\n" + mdText
            }
            pr.body = result
        }
    }
}
