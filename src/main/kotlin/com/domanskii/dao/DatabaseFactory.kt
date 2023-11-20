package com.domanskii.dao

import com.domanskii.models.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*

private val log = KotlinLogging.logger {}

object DatabaseFactory {
    fun init() {
        log.debug { "Initializing Database..." }

        val dbHost = System.getenv("DB_HOST")
        val dbName = System.getenv("DB_NAME")
        val dbUsername = System.getenv("DB_USERNAME")
        val dbPassword = System.getenv("DB_PASSWORD")

        val jdbcURL = "jdbc:postgresql://$dbHost/$dbName"
        val driverClassName = "org.postgresql.Driver"
        val database = Database.connect(jdbcURL, driverClassName, dbUsername, dbPassword)

        transaction(database) {
            SchemaUtils.create(Metrics)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}