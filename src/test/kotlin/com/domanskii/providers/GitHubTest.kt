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
    private val gitHubProvider = GitHub(repoName, defaultBranch, gitHub)

    @Test
    fun `isReferenceCommit should return true when commit is a merge to default branch`() {
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
    fun `isReferenceCommit should return false when commit is not a merge to default branch`() {
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
    fun `isReferenceCommit should return false when there are no PRs for the commit`() {
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
}
