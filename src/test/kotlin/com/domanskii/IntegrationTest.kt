package com.domanskii

import com.domanskii.plugins.configureAuthentication
import com.domanskii.plugins.configureRouting
import com.domanskii.plugins.configureSerialization
import com.domanskii.providers.VcsProvider
import com.domanskii.services.MarkdownService
import com.domanskii.services.MetricsService
import com.domanskii.storage.Metrics
import com.domanskii.storage.StorageImpl
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.*

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class IntegrationTest {

    private val secretHeader = "test-secret"
    private lateinit var storage: StorageImpl
    private lateinit var mockVcsProvider: VcsProvider
    private lateinit var markdownService: MarkdownService
    private lateinit var metricsService: MetricsService

    @BeforeEach
    fun setup() {
        // Initialize in-memory H2 database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            // Create the Metrics table
            SchemaUtils.create(Metrics)
        }

        storage = StorageImpl()
        mockVcsProvider = mock()
        markdownService = MarkdownService()
        metricsService = MetricsService(storage, mockVcsProvider, markdownService)
    }

    @AfterEach
    fun teardown() {
        transaction {
            // Drop the Metrics table after tests
            SchemaUtils.drop(Metrics)
        }
    }

    @Test
    fun `POST a new metric should process metric and update PR body`() = testApplication {
        // Mock the VcsProvider behavior
        whenever(mockVcsProvider.isReferenceCommit("commitSha1")).thenReturn(false)

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
                    "commitSha": "commitSha1",
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

        val expectedBody = """#### ✅ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Test Metric | 15.0 ms | NEW METRIC |""".trimIndent()

        // Capture the arguments passed to publishMetrics
        argumentCaptor<String>().apply {
            verify(mockVcsProvider).publishMetrics(eq("commitSha1"), capture())
            assertEquals(expectedBody, firstValue)
        }
    }

    @Test
    fun `POST an existing metric should process metric and update PR body with diff`() = testApplication {
        application {
            configureAuthentication(secretHeader)
            configureSerialization()
            configureRouting(metricsService)
        }

        // Mock the VcsProvider behavior for reference commit
        whenever(mockVcsProvider.isReferenceCommit("commitSha1")).thenReturn(true)

        // Submit the reference metric
        val responseRef = client.post("/api/v1/metrics") {
            header(HttpHeaders.Authorization, "Bearer $secretHeader")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "commitSha": "commitSha1",
                    "name": "test_metric",
                    "value": 10.0,
                    "units": "ms",
                    "threshold": 5.0,
                    "isIncreaseBad": true
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, responseRef.status)

        // Mock the VcsProvider behavior for new commit
        whenever(mockVcsProvider.isReferenceCommit("commitSha2")).thenReturn(false)

        // Submit the new metric
        val response = client.post("/api/v1/metrics") {
            header(HttpHeaders.Authorization, "Bearer $secretHeader")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "commitSha": "commitSha2",
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

        val expectedBody = """#### ⚠️ PR Metrics
| Metric               | Value   | Diff        |
|----------------------|---------|-------------|
| Test Metric | 15.0 ms | + 5.0 ms 🔺 |""".trimIndent()

        // Capture the arguments passed to publishMetrics
        argumentCaptor<String>().apply {
            verify(mockVcsProvider).publishMetrics(eq("commitSha2"), capture())
            assertEquals(expectedBody, firstValue)
        }
    }

    @Test
    fun `POST metrics for reference commit should store metric as reference`() = testApplication {
        application {
            configureAuthentication(secretHeader)
            configureSerialization()
            configureRouting(metricsService)
        }

        // Mock the VcsProvider behavior
        whenever(mockVcsProvider.isReferenceCommit("commitRef123")).thenReturn(true)

        val response = client.post("/api/v1/metrics") {
            header(HttpHeaders.Authorization, "Bearer $secretHeader")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "commitSha": "commitRef123",
                    "name": "reference_metric",
                    "value": 20.0,
                    "units": "ms",
                    "threshold": 5.0,
                    "isIncreaseBad": true
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        // Verify that publishMetrics was not called for reference commit
        verify(mockVcsProvider, times(0)).publishMetrics(any(), any())

        // Verify that the metric is stored as reference in the database
        val storedMetric = runBlocking { storage.getReferenceForMetric("reference_metric") }
        assertNotNull(storedMetric)
        assertTrue(storedMetric!!.isReference)
        assertEquals(20.0, storedMetric.value)
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
            setBody(
                """
                {
                    "commitSha": "commitShaNoAuth",
                    "name": "unauthorized_metric",
                    "value": 25.0,
                    "units": "ms",
                    "threshold": 5.0,
                    "isIncreaseBad": true
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
