package com.domanskii.storage

import com.domanskii.common.Metric
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageImplTest {

    private lateinit var storage: StorageImpl

    @BeforeAll
    fun setup() {
        // Initialize an in-memory H2 database for testing
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            // Create the Metrics table
            org.jetbrains.exposed.sql.SchemaUtils.create(Metrics)
        }
        storage = StorageImpl()
    }

    @AfterAll
    fun teardown() {
        // Drop the Metrics table after tests
        transaction {
            org.jetbrains.exposed.sql.SchemaUtils.drop(Metrics)
        }
    }

    @Test
    fun `submitMetric should insert metric and return it`() = runBlocking {
        // Arrange
        val metric = Metric("commitSha1", "test_metric", 10.0, "ms", 5.0, isReference = false, isIncreaseBad = true)

        // Act
        val result = storage.submitMetric(metric)

        // Assert
        assertEquals(metric, result)
    }

    @Test
    fun `getMetricsForCommit should return metrics for the given commit`() = runBlocking {
        // Arrange
        val commitSha = "commitSha2"
        val metric1 = Metric(commitSha, "metric1", 10.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        val metric2 = Metric(commitSha, "metric2", 20.0, "ms", 5.0, isReference = false, isIncreaseBad = true)
        storage.submitMetric(metric1)
        storage.submitMetric(metric2)

        // Act
        val metrics = storage.getMetricsForCommit(commitSha)

        // Assert
        assertEquals(2, metrics.size)
        assert(metrics.contains(metric1))
        assert(metrics.contains(metric2))
    }

    @Test
    fun `getReferenceForMetric should return the latest reference metric`() = runBlocking {
        // Arrange
        val metricName = "reference_metric"
        val referenceMetric1 = Metric("commitRef1", metricName, 30.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        val referenceMetric2 = Metric("commitRef2", metricName, 35.0, "ms", 5.0, isReference = true, isIncreaseBad = true)
        storage.submitMetric(referenceMetric1)
        storage.submitMetric(referenceMetric2)

        // Act
        val reference = storage.getReferenceForMetric(metricName)

        // Assert
        assertEquals(referenceMetric2, reference)
    }

    @Test
    fun `getReferenceForMetric should return null when no reference exists`() = runBlocking {
        // Arrange
        val metricName = "nonexistent_metric"

        // Act
        val reference = storage.getReferenceForMetric(metricName)

        // Assert
        assertNull(reference)
    }
}
