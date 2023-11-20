package com.domanskii.storage

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*

private val log = KotlinLogging.logger {}

object DatabaseFactory {
    fun init(dbHost: String, dbName: String, dbUsername: String, dbPassword: String) {
        log.debug { "Initializing Database..." }

        val jdbcURL = "jdbc:postgresql://$dbHost/$dbName"
        val driverClassName = "org.postgresql.Driver"
        val database = Database.connect(jdbcURL, driverClassName, dbUsername, dbPassword)

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Metrics)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}