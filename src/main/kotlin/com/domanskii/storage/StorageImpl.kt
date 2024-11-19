package com.domanskii.storage

import com.domanskii.common.Metric
import com.domanskii.storage.DatabaseFactory.dbQuery
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*

private val log = KotlinLogging.logger {}

class StorageImpl : Storage {
    private fun resultRowToMetric(row: ResultRow) = Metric(
        commitSha = row[Metrics.commitSha],
        name = row[Metrics.name],
        value = row[Metrics.value],
        units = row[Metrics.units],
        threshold = row[Metrics.threshold],
        isReference = row[Metrics.isReference],
        isIncreaseBad = row[Metrics.isIncreaseBad]
    )

    override suspend fun submitMetric(
        metric: Metric
    ): Metric? = dbQuery {
        log.debug { "Inserting metrics into DB table..." }

        val insertStatement = Metrics.insert {
            it[commitSha] = metric.commitSha
            it[name] = metric.name
            it[value] = metric.value
            it[units] = metric.units
            it[threshold] = metric.threshold
            it[isReference] = metric.isReference
            it[isIncreaseBad] = metric.isIncreaseBad
        }
        insertStatement.resultedValues?.singleOrNull()?.let(::resultRowToMetric)
    }

    override suspend fun getMetricsForCommit(commitSha: String): List<Metric> = dbQuery {
        log.debug { "Getting metrics for the commit from DB..." }

        val maxIdsSubQuery = Metrics
            .select(Metrics.id.max())
            .where { Metrics.commitSha eq commitSha }
            .groupBy(Metrics.name)

        Metrics
            .selectAll().where { Metrics.id inSubQuery maxIdsSubQuery }
            .map(::resultRowToMetric)
    }

    override suspend fun getReferenceForMetric(name: String): Metric? = dbQuery {
        log.debug { "Getting reference for the metric from DB..." }

        Metrics
            .selectAll().where { (Metrics.name eq name) and (Metrics.isReference eq true) }
            .orderBy(Metrics.id to SortOrder.DESC)
            .limit(1)
            .map(::resultRowToMetric)
            .singleOrNull()
    }
}