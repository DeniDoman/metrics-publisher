package com.domanskii

import com.domanskii.common.Metric
import com.domanskii.plugins.configureAuthentication
import com.domanskii.plugins.configureRouting
import com.domanskii.plugins.configureSerialization
import com.domanskii.providers.GitHub
import com.domanskii.providers.GitHubApiClient
import com.domanskii.services.MarkdownService
import com.domanskii.services.MetricsService
import com.domanskii.storage.DatabaseFactory
import com.domanskii.storage.StorageImpl
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrationTest {

    private val secretHeader = "test-secret"
    private lateinit var storage: StorageImpl
    private lateinit var githubApiClient: GitHubApiClient
    private lateinit var metricsService: MetricsService

    @BeforeEach
    fun setup() {
        // Initialize an in-memory H2 database for testing
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        runBlocking {
            DatabaseFactory.dbQuery {
                SchemaUtils.createMissingTablesAndColumns(com.domanskii.storage.Metrics)
            }
        }

        storage = StorageImpl()
        githubApiClient = mock()
        val github = GitHub("repo", "main", githubApiClient)
        val markdownService = MarkdownService()
        metricsService = MetricsService(storage, github, markdownService)
    }

    @AfterEach
    fun teardown() {
        runBlocking {
            DatabaseFactory.dbQuery {
                SchemaUtils.drop(com.domanskii.storage.Metrics)
            }
        }
    }

    @Test
    fun `POST a new metric should process metric and update PR body`() = testApplication {
        // Create a mock PullRequest object
        val pullRequest = object : com.domanskii.providers.PullRequest {
            override val number: Int = 42
            override var body: String = "PR Body\n<!-- PR-METRICS-PUBLISHER:START -->"
            override val base: com.domanskii.providers.Base = object : com.domanskii.providers.Base {
                override val ref: String = "feature-branch"
            }
            override fun isMerged(): Boolean {
                return false
            }
        }

        // Set up the MockGitHubApiClient to return this PullRequest
        val mockGitHubApiClient = object : GitHubApiClient {
            override fun getRepository(repoName: String): com.domanskii.providers.Repository {
                return object : com.domanskii.providers.Repository {
                    override fun getCommit(sha: String): com.domanskii.providers.Commit {
                        return object : com.domanskii.providers.Commit {
                            override fun listPullRequests(): List<com.domanskii.providers.PullRequest> {
                                return listOf(pullRequest)
                            }
                        }
                    }
                }
            }
        }

        // Create a GitHub instance using the mockGitHubApiClient
        val github = GitHub("repo", "main", mockGitHubApiClient)
        val markdownService = MarkdownService()
        // Create a new MetricsService with the new GitHub instance
        metricsService = MetricsService(storage, github, markdownService)

        application {
            configureAuthentication(secretHeader)
            configureSerialization()
            configureRouting(metricsService)
        }

        val response = client.post("/api/v1/metrics") {
            header(HttpHeaders.Authorization, "Bearer $secretHeader")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "commitSha": "ghi789",
                    "name": "test_metric",
                    "value": 15.0,
                    "units": "ms",
                    "threshold": 5.0,
                    "isIncreaseBad": true
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        // Verify that the metric was stored and the PR body was updated
        val expectedBody = """
            PR Body
            <!-- PR-METRICS-PUBLISHER:START -->
            #### ✅ PR Metrics
            | Metric               | Value   | Diff        |
            |----------------------|---------|-------------|
            | Test Metric | 15.0 ms | NEW METRIC |
        """.trimIndent()

        runBlocking {
            val metrics = storage.getMetricsForCommit("ghi789")
            assertEquals(1, metrics.size)
            assertEquals("test_metric", metrics[0].name)
            assertEquals(15.0, metrics[0].value)
            assertEquals(false, metrics[0].isReference)

            // Verify that the PR body was updated
            assertEquals(pullRequest.body, expectedBody)
        }
    }

    @Test
    fun `POST an existing metric should process metric and update PR body with diff`() = testApplication {
        // Create a mock PullRequest object
        val pullRequest = object : com.domanskii.providers.PullRequest {
            override val number: Int = 42
            override var body: String = "PR Body\n<!-- PR-METRICS-PUBLISHER:START -->"
            override val base: com.domanskii.providers.Base = object : com.domanskii.providers.Base {
                override val ref: String = "feature-branch" // Not the default branch
            }
            override fun isMerged(): Boolean {
                return false // PR is not merged
            }
        }

        // Set up the MockGitHubApiClient to return this PullRequest
        val mockGitHubApiClient = object : GitHubApiClient {
            override fun getRepository(repoName: String): com.domanskii.providers.Repository {
                return object : com.domanskii.providers.Repository {
                    override fun getCommit(sha: String): com.domanskii.providers.Commit {
                        return object : com.domanskii.providers.Commit {
                            override fun listPullRequests(): List<com.domanskii.providers.PullRequest> {
                                return listOf(pullRequest)
                            }
                        }
                    }
                }
            }
        }

        // Create a GitHub instance using the mockGitHubApiClient
        val github = GitHub("repo", "main", mockGitHubApiClient)
        val markdownService = MarkdownService()
        // Create a new MetricsService with the new GitHub instance
        val storage = StorageImpl()
        val metricsService = MetricsService(storage, github, markdownService)

        // Insert a reference metric into the database
        runBlocking {
            val referenceMetric = Metric(
                commitSha = "ref123",
                name = "test_metric",
                value = 10.0,
                units = "ms",
                threshold = 5.0,
                isReference = true,
                isIncreaseBad = true
            )
            storage.submitMetric(referenceMetric)
        }

        application {
            configureAuthentication(secretHeader)
            configureSerialization()
            configureRouting(metricsService)
        }

        // Post the new metric
        val response = client.post("/api/v1/metrics") {
            header(HttpHeaders.Authorization, "Bearer $secretHeader")
            contentType(ContentType.Application.Json)
            setBody(
                """
            {
                "commitSha": "ghi789",
                "name": "test_metric",
                "value": 15.0,
                "units": "ms",
                "threshold": 5.0,
                "isIncreaseBad": true
            }
            """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        // Expected PR body with diff
        val expectedBody = """
            PR Body
            <!-- PR-METRICS-PUBLISHER:START -->
            #### ⚠️ PR Metrics
            | Metric               | Value   | Diff        |
            |----------------------|---------|-------------|
            | Test Metric | 15.0 ms | + 5.0 ms 🔺 |
    """.trimIndent()

        runBlocking {
            val metrics = storage.getMetricsForCommit("ghi789")
            assertEquals(1, metrics.size)
            assertEquals("test_metric", metrics[0].name)
            assertEquals(15.0, metrics[0].value)
            assertEquals(false, metrics[0].isReference) // Should be false since it's not a reference commit

            // Verify that the PR body was updated
            assertEquals(expectedBody, pullRequest.body)
        }
    }

    @Test
    fun `POST metrics for reference commit should store metric as reference`() = testApplication {
        application {
            configureAuthentication(secretHeader)
            configureSerialization()
            configureRouting(metricsService)
        }

        // Mock the GitHub API client to identify the commit as a reference commit
        runBlocking {
            val mockCommit = mock<com.domanskii.providers.Commit>()
            val mockRepo = mock<com.domanskii.providers.Repository>()
            val mockPR = mock<com.domanskii.providers.PullRequest>()
            whenever(githubApiClient.getRepository(any())).thenReturn(mockRepo)
            whenever(mockRepo.getCommit(any())).thenReturn(mockCommit)
            whenever(mockCommit.listPullRequests()).thenReturn(listOf(mockPR))
            whenever(mockPR.isMerged()).thenReturn(true)
            whenever(mockPR.base).thenReturn(object : com.domanskii.providers.Base {
                override val ref: String = "main"
            })
        }

        val response = client.post("/api/v1/metrics") {
            header(HttpHeaders.Authorization, "Bearer $secretHeader")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "commitSha": "def456",
                    "name": "ref_metric",
                    "value": 20.0,
                    "units": "ms",
                    "threshold": 5.0,
                    "isIncreaseBad": true
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        // Verify that the metric was stored as a reference metric
        runBlocking {
            val metrics = storage.getMetricsForCommit("def456")
            assertEquals(1, metrics.size)
            assertEquals("ref_metric", metrics[0].name)
            assertEquals(20.0, metrics[0].value)
            assertEquals(true, metrics[0].isReference)
        }
    }

    @Test
    fun `POST metrics without authorization should return Unauthorized`() = testApplication {
        application {
            configureAuthentication(secretHeader)
            configureSerialization()
            configureRouting(metricsService)
        }

        val response = client.post("/api/v1/metrics") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
