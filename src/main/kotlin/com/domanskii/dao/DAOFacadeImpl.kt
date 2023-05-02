package com.domanskii.dao

import com.domanskii.dao.DatabaseFactory.dbQuery
import com.domanskii.models.Metric
import com.domanskii.models.Metrics
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*

private val log = KotlinLogging.logger {}

class DAOFacadeImpl : DAOFacade {
    private fun resultRowToMetric(row: ResultRow) = Metric(
        id = row[Metrics.id],
        commitSha = row[Metrics.commitSha],
        name = row[Metrics.name],
        value = row[Metrics.value],
        units = row[Metrics.units],
        isReference = row[Metrics.isReference],
        isIncreaseBad = row[Metrics.isIncreaseBad]
    )

    override suspend fun postMetric(
        commitSha: String, name: String, value: Double, units: String, isReference: Boolean, isIncreaseBad: Boolean
    ): Metric? = dbQuery {
        log.debug { "Inserting metrics into DB table..." }
        val insertStatement = Metrics.insert {
            it[Metrics.commitSha] = commitSha
            it[Metrics.name] = name
            it[Metrics.value] = value
            it[Metrics.units] = units
            it[Metrics.isReference] = isReference
            it[Metrics.isIncreaseBad] = isIncreaseBad
        }
        insertStatement.resultedValues?.singleOrNull()?.let(::resultRowToMetric)
    }

    override suspend fun getMetricsForCommit(commitSha: String): List<Metric> = dbQuery {
        log.debug { "Getting metrics for the commit from DB..." }
        Metrics
            .select { Metrics.commitSha eq commitSha }
            .map(::resultRowToMetric)
    }

    override suspend fun getReferenceForMetric(name: String): Metric? = dbQuery {
        log.debug { "Getting reference for the metric from DB..." }
        Metrics
            .select { (Metrics.name eq name) and (Metrics.isReference eq true) }
            .orderBy(Metrics.id to SortOrder.DESC)
            .limit(1)
            .map(::resultRowToMetric)
            .singleOrNull()
    }
}