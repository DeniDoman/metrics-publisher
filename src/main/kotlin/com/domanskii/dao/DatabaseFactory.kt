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

        val dbAddress = System.getenv("DB_ADDRESS")
        val dbUser = System.getenv("DB_USER")
        val dbPassword = System.getenv("DB_PASSWORD")

        val jdbcURL = "jdbc:postgresql://$dbAddress"
        val driverClassName = "org.postgresql.Driver"
        val database = Database.connect(jdbcURL, driverClassName, dbUser, dbPassword)

        transaction(database) {
            SchemaUtils.create(Metrics)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}