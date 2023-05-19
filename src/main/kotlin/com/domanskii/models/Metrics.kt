package com.domanskii.models

import org.jetbrains.exposed.sql.*

data class Metric(
    val id: Int,
    val commitSha: String,
    val name: String,
    val value: Double,
    val units: String,
    val threshold: Double,
    val isReference: Boolean,
    val isIncreaseBad: Boolean
)

object Metrics : Table() {
    val id = integer("id").autoIncrement()
    val commitSha = varchar("commit_sha", 40)
    val name = varchar("name", 128)
    val value = double("value")
    val units = varchar("units", 16)
    val threshold = double("threshold")
    val isReference = bool("is_reference")
    val isIncreaseBad = bool("is_increase_bad")

    override val primaryKey = PrimaryKey(id)
}
