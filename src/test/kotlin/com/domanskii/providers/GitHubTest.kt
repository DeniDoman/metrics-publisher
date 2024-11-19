package com.domanskii.providers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GitHubTest {

    @Test
    fun `isReferenceCommit should return true when commit is a merge to default branch`() {
        // Arrange
        val defaultBranch = "main"
        val commitSha = "1234567"
        val pullRequest = object : PullRequest {
            override val number: Int
                get() = 1
            override var body: String = "PR Body with placeholder <!-- PR-METRICS-PUBLISHER:START -->"
            override val base: Base
                get() = object : Base {
                    override val ref: String
                        get() = defaultBranch
                }

            override fun isMerged(): Boolean {
                return true
            }
        }
        val ghClientApi = MockGitHubApiClient(pullRequests = listOf(pullRequest))
        val ghClient = GitHub("repo", defaultBranch, ghClientApi)
        // Act
        val isReferenceCommit = ghClient.isReferenceCommit(commitSha)
        // Assert
        assertTrue(isReferenceCommit)
    }

    @Test
    fun `isReferenceCommit should return false when commit is not a merge to default branch`() {
        // Arrange
        val defaultBranch = "main"
        val commitSha = "1234567"
        val pullRequest = object : PullRequest {
            override val number: Int
                get() = 2
            override var body: String = "PR Body without placeholder"
            override val base: Base
                get() = object : Base {
                    override val ref: String
                        get() = "feature-branch"
                }
            override fun isMerged(): Boolean {
                return true
            }
        }
        val ghClientApi = MockGitHubApiClient(pullRequests = listOf(pullRequest))
        val ghClient = GitHub("repo", defaultBranch, ghClientApi)
        // Act
        val isReferenceCommit = ghClient.isReferenceCommit(commitSha)
        // Assert
        assertFalse(isReferenceCommit)
    }

    @Test
    fun `isReferenceCommit should return false when there are no PRs for the commit`() {
        // Arrange
        val defaultBranch = "main"
        val commitSha = "1234567"
        val ghClientApi = MockGitHubApiClient(pullRequests = emptyList())
        val ghClient = GitHub("repo", defaultBranch, ghClientApi)
        // Act
        val isReferenceCommit = ghClient.isReferenceCommit(commitSha)
        // Assert
        assertFalse(isReferenceCommit)
    }

    @Test
    fun `publishMetrics should update PR body when PR contains placeholder`() = runBlocking {
        // Arrange
        val defaultBranch = "main"
        val commitSha = "1234567"
        val placeholder = "<!-- PR-METRICS-PUBLISHER:START -->"
        val initialBody = "$placeholder\nInitial body content."
        val mdText = "## Metrics Report\nMetric details..."
        val pullRequest = object : PullRequest {
            override val number: Int
                get() = 1
            override var body: String = initialBody
            override val base: Base
                get() = object : Base {
                    override val ref: String
                        get() = defaultBranch
                }
            override fun isMerged(): Boolean {
                return true
            }
        }
        val ghClientApi = MockGitHubApiClient(pullRequests = listOf(pullRequest))
        val ghClient = GitHub("repo", defaultBranch, ghClientApi)
        // Act
        ghClient.publishMetrics(commitSha, mdText)
        // Assert
        val expectedBody = "$placeholder\n$mdText"
        assertEquals(expectedBody, pullRequest.body)
    }

    @Test
    fun `publishMetrics should not update PR body when PR body does not contain placeholder`() = runBlocking {
        // Arrange
        val defaultBranch = "main"
        val commitSha = "1234567"
        val initialBody = "Some PR body without placeholder."
        val mdText = "## Metrics Report\nMetric details..."
        val pullRequest = object : PullRequest {
            override val number: Int
                get() = 3
            override var body: String = initialBody
            override val base: Base
                get() = object : Base {
                    override val ref: String
                        get() = defaultBranch
                }
            override fun isMerged(): Boolean {
                return true
            }
        }
        val ghClientApi = MockGitHubApiClient(pullRequests = listOf(pullRequest))
        val ghClient = GitHub("repo", defaultBranch, ghClientApi)
        // Act
        ghClient.publishMetrics(commitSha, mdText)
        // Assert
        assertEquals(initialBody, pullRequest.body)
    }
}
