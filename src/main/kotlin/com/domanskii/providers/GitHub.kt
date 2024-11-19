// File: main/kotlin/com/domanskii/providers/GitHub.kt
package com.domanskii.providers

import mu.KotlinLogging
import org.kohsuke.github.GitHub as KohsukeGitHub

private val log = KotlinLogging.logger {}

class GitHub(
    private val repoName: String,
    private val defaultBranch: String,
    private val gitHub: KohsukeGitHub
) : VcsProvider {
    private val placeholderRegex = "(<!-- PR-METRICS-PUBLISHER:.*?-->.*)([\\s\\S]*)".toRegex()

    override fun isReferenceCommit(commitSha: String): Boolean {
        log.debug { "isReferenceCommit() for ${commitSha.take(7)} commit..." }
        return try {
            val repo = gitHub.getRepository(repoName)
            val commit = repo.getCommit(commitSha)
            val prList = commit.listPullRequests().toList().filter {
                it.isMerged && it.base.ref == defaultBranch
            }
            prList.isNotEmpty().also {
                if (it) log.debug { "${commitSha.take(7)} is a merge-to-$defaultBranch commit!" }
            }
        } catch (e: Exception) {
            log.error(e) { "Error checking if commit is a reference commit." }
            false
        }
    }

    override suspend fun publishMetrics(commitSha: String, mdText: String) {
        log.debug { "publishMetrics() for ${commitSha.take(7)} commit..." }
        try {
            val repo = gitHub.getRepository(repoName)
            val commit = repo.getCommit(commitSha)
            val prList = commit.listPullRequests().toList()

            if (prList.isEmpty()) {
                log.debug { "There are no PRs for ${commitSha.take(7)} commit..." }
                return
            }
            log.debug { "${prList.size} PRs found for ${commitSha.take(7)} commit" }

            prList.forEach { pr ->
                log.debug { "Updating #${pr.number} PR body for ${commitSha.take(7)} commit" }
                val body = pr.body ?: ""

                if (body.isBlank()) {
                    log.info { "PR #${pr.number} doesn't have a body!" }
                    return@forEach
                }
                if (!placeholderRegex.containsMatchIn(body)) {
                    log.info { "PR #${pr.number} doesn't have the placeholder!" }
                    return@forEach
                }

                val result = body.replace(placeholderRegex) { matchResult ->
                    matchResult.groupValues[1] + "\n" + mdText
                }
                pr.setBody(result)
            }
        } catch (e: Exception) {
            log.error(e) { "Error publishing metrics to PRs." }
        }
    }
}
