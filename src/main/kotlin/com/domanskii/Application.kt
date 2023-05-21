package com.domanskii

import com.domanskii.storage.StorageImpl
import com.domanskii.storage.DatabaseFactory
import com.domanskii.providers.GitHub
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.domanskii.plugins.*
import com.domanskii.services.CalculatorService
import com.domanskii.services.MarkdownService
import com.domanskii.services.MetricsService
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun main() {
    log.info { "Application starting..." }
    assertEnvVariables()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
    log.info { "Application finished" }
}

fun Application.module() {
    val secretHeader = System.getenv("SECRET_HEADER")
    val ghRepo = System.getenv("GH_REPO")
    val ghToken = System.getenv("GH_TOKEN")
    val ghDefaultBranch = System.getenv("GH_DEFAULT_BRANCH")
    val dbHost = System.getenv("DB_HOST")
    val dbName = System.getenv("DB_NAME")
    val dbUsername = System.getenv("DB_USERNAME")
    val dbPassword = System.getenv("DB_PASSWORD")

    DatabaseFactory.init(dbHost, dbName, dbUsername, dbPassword)
    val storage = StorageImpl()
    val github = GitHub(ghToken, ghRepo, ghDefaultBranch)
    val markdownService = MarkdownService()
    val calculatorService = CalculatorService()
    val metricsService = MetricsService(storage, github, markdownService, calculatorService)

    configureAuthentication(secretHeader)
    configureSerialization()
    configureRouting(metricsService)
}

fun assertEnvVariables() {
    log.info { "Asserting env variables..." }
    assert(System.getenv("SECRET_HEADER").isNotBlank())
    assert(System.getenv("GH_REPO").isNotBlank())
    assert(System.getenv("GH_TOKEN").isNotBlank())
    assert(System.getenv("GH_DEFAULT_BRANCH").isNotBlank())
    assert(System.getenv("DB_HOST").isNotBlank())
    assert(System.getenv("DB_NAME").isNotBlank())
    assert(System.getenv("DB_USERNAME").isNotBlank())
    assert(System.getenv("DB_PASSWORD").isNotEmpty())
}
