package com.domanskii.providers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kohsuke.github.*
import org.mockito.kotlin.*
import org.kohsuke.github.GitHub as KohsukeGitHub

class GitHubTest {

    private val repoName = "test/repo"
    private val defaultBranch = "main"
    private val gitHub: KohsukeGitHub = mock()
    private val gitHubProvider = GitHub(repoName, defaultBranch, minRequestIntervalMs = 0) { gitHub }

    @Test
    fun `isReferenceCommit should return true when commit is a merge to default branch`() = runBlocking {
        // Arrange
        val commitSha = "abc123"
        val repo = mock<GHRepository>()
        val commit = mock<GHCommit>()
        val pullRequest = mock<GHPullRequest>()
        val pullRequests = listOf(pullRequest)
        val pullRequestBranch = mock<GHCommitPointer>()
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.getCommit(commitSha)).thenReturn(commit)
        whenever(commit.listPullRequests()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(pullRequests)
        whenever(pullRequest.isMerged).thenReturn(true)
        whenever(pullRequest.base).thenReturn(pullRequestBranch)
        whenever(pullRequestBranch.ref).thenReturn(defaultBranch)

        // Act
        val result = gitHubProvider.isReferenceCommit(commitSha)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isReferenceCommit should return false when commit is not a merge to default branch`() = runBlocking {
        // Arrange
        val commitSha = "def456"
        val repo = mock<GHRepository>()
        val commit = mock<GHCommit>()
        val pullRequest = mock<GHPullRequest>()
        val pullRequests = listOf(pullRequest)
        val pullRequestBranch = mock<GHCommitPointer>()
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.getCommit(commitSha)).thenReturn(commit)
        whenever(commit.listPullRequests()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(pullRequests)
        whenever(pullRequest.isMerged).thenReturn(true)
        whenever(pullRequest.base).thenReturn(pullRequestBranch)
        whenever(pullRequestBranch.ref).thenReturn("feature-branch")

        // Act
        val result = gitHubProvider.isReferenceCommit(commitSha)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isReferenceCommit should return false when there are no PRs for the commit`() = runBlocking {
        // Arrange
        val commitSha = "ghi789"
        val repo = mock<GHRepository>()
        val commit = mock<GHCommit>()
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.getCommit(commitSha)).thenReturn(commit)
        whenever(commit.listPullRequests()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(emptyList())

        // Act
        val result = gitHubProvider.isReferenceCommit(commitSha)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `publishMetrics should update PR body when PR contains placeholder`() = runBlocking {
        // Arrange
        val commitSha = "jkl012"
        val mdText = "New Metrics Text"
        val repo = mock<GHRepository>()
        val commit = mock<GHCommit>()
        val pullRequest = mock<GHPullRequest>()
        val pullRequests = listOf(pullRequest)
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()
        val bodyWithPlaceholder = "This is the PR body.\n<!-- PR-METRICS-PUBLISHER:START -->\nOld Metrics Text"
        val placeholderRegex = "(<!-- PR-METRICS-PUBLISHER:.*?-->.*)([\\s\\S]*)".toRegex()

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.getCommit(commitSha)).thenReturn(commit)
        whenever(commit.listPullRequests()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(pullRequests)
        whenever(pullRequest.body).thenReturn(bodyWithPlaceholder)

        // Act
        gitHubProvider.publishMetrics(commitSha, mdText)

        // Assert
        val expectedBody = bodyWithPlaceholder.replace(
            placeholderRegex
        ) { matchResult ->
            matchResult.groupValues[1] + "\n" + mdText
        }

        verify(pullRequest).setBody(expectedBody)
    }

    @Test
    fun `publishMetrics should not update PR body when PR body does not contain placeholder`() = runBlocking {
        // Arrange
        val commitSha = "mno345"
        val mdText = "New Metrics Text"
        val repo = mock<GHRepository>()
        val commit = mock<GHCommit>()
        val pullRequest = mock<GHPullRequest>()
        val pullRequests = listOf(pullRequest)
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()
        val bodyWithoutPlaceholder = "This is the PR body without placeholder."

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.getCommit(commitSha)).thenReturn(commit)
        whenever(commit.listPullRequests()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(pullRequests)
        whenever(pullRequest.body).thenReturn(bodyWithoutPlaceholder)

        // Act
        gitHubProvider.publishMetrics(commitSha, mdText)

        // Assert
        verify(pullRequest, never()).setBody(any())
    }

    @Test
    fun `getOpenPullRequestHeadCommits should return head sha for each open PR`() = runBlocking {
        // Arrange
        val repo = mock<GHRepository>()
        val queryBuilder = mock<GHPullRequestQueryBuilder>()
        val pullRequest1 = mock<GHPullRequest>()
        val pullRequest2 = mock<GHPullRequest>()
        val head1 = mock<GHCommitPointer>()
        val head2 = mock<GHCommitPointer>()
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.queryPullRequests()).thenReturn(queryBuilder)
        whenever(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder)
        whenever(queryBuilder.list()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(listOf(pullRequest1, pullRequest2))
        whenever(pullRequest1.head).thenReturn(head1)
        whenever(pullRequest2.head).thenReturn(head2)
        whenever(head1.sha).thenReturn("sha1")
        whenever(head2.sha).thenReturn("sha2")

        // Act
        val result = gitHubProvider.getOpenPullRequestHeadCommits()

        // Assert
        assertTrue(result == listOf("sha1", "sha2"))
    }

    @Test
    fun `getOpenPullRequestHeadCommits should return empty list when there are no open PRs`() = runBlocking {
        // Arrange
        val repo = mock<GHRepository>()
        val queryBuilder = mock<GHPullRequestQueryBuilder>()
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.queryPullRequests()).thenReturn(queryBuilder)
        whenever(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder)
        whenever(queryBuilder.list()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(emptyList())

        // Act
        val result = gitHubProvider.getOpenPullRequestHeadCommits()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getOpenPullRequestHeadCommits should return empty list on error`() = runBlocking {
        // Arrange
        whenever(gitHub.getRepository(repoName)).thenThrow(RuntimeException("boom"))

        // Act
        val result = gitHubProvider.getOpenPullRequestHeadCommits()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `throttling should space out consecutive GitHub API calls by at least minRequestIntervalMs`() = runBlocking {
        // Arrange
        val throttledMs = 200L
        val throttledProvider = GitHub(repoName, defaultBranch, minRequestIntervalMs = throttledMs) { gitHub }
        val repo = mock<GHRepository>()
        val queryBuilder = mock<GHPullRequestQueryBuilder>()
        val pagedIterable = mock<PagedIterable<GHPullRequest>>()

        whenever(gitHub.getRepository(repoName)).thenReturn(repo)
        whenever(repo.queryPullRequests()).thenReturn(queryBuilder)
        whenever(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder)
        whenever(queryBuilder.list()).thenReturn(pagedIterable)
        whenever(pagedIterable.toList()).thenReturn(emptyList())

        // Act
        throttledProvider.getOpenPullRequestHeadCommits() // primes lastRequestAtMs
        val start = System.currentTimeMillis()
        throttledProvider.getOpenPullRequestHeadCommits()
        val elapsed = System.currentTimeMillis() - start

        // Assert
        assertTrue(elapsed >= throttledMs)
    }
}
