// File: main/kotlin/com/domanskii/providers/GitHub.kt
package com.domanskii.providers

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHub as KohsukeGitHub

private val log = KotlinLogging.logger {}

class GitHub(
    private val repoName: String,
    private val defaultBranch: String,
    /** Minimum delay enforced between calls that hit the GitHub API, to stay clear of GH's secondary rate limits. */
    private val minRequestIntervalMs: Long = 500,
    private val makeClient: () -> KohsukeGitHub
) : VcsProvider {
    private val placeholderRegex = "(<!-- PR-METRICS-PUBLISHER:.*?-->.*)([\\s\\S]*)".toRegex()
    private val gitHub: KohsukeGitHub
        get() = makeClient()

    private val throttleMutex = Mutex()
    private var lastRequestAtMs = 0L

    /**
     * Suspends the caller, if needed, so consecutive GitHub API calls are spaced out by at least
     * [minRequestIntervalMs]. Callers of this class (e.g. MetricsService.refreshAll(), which hits
     * GH once per open PR in a loop) can end up firing requests in quick succession otherwise.
     * Uses a suspending Mutex + delay() rather than a blocking lock, so a wait here just parks the
     * coroutine instead of tying up a Ktor worker thread.
     */
    private suspend fun throttle() {
        throttleMutex.withLock {
            val waitMs = minRequestIntervalMs - (System.currentTimeMillis() - lastRequestAtMs)
            if (waitMs > 0) delay(waitMs)
            lastRequestAtMs = System.currentTimeMillis()
        }
    }

    override suspend fun isReferenceCommit(commitSha: String): Boolean {
        log.debug { "isReferenceCommit() for ${commitSha.take(7)} commit..." }
        throttle()
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
        throttle()
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

    override suspend fun getOpenPullRequestHeadCommits(): List<String> {
        log.debug { "getOpenPullRequestHeadCommits()..." }
        throttle()
        return try {
            val repo = gitHub.getRepository(repoName)
            val prList = repo.queryPullRequests().state(GHIssueState.OPEN).list().toList()
            log.debug { "${prList.size} open PRs found" }
            prList.map { it.head.sha }
        } catch (e: Exception) {
            log.error(e) { "Error getting open pull requests." }
            emptyList()
        }
    }
}
