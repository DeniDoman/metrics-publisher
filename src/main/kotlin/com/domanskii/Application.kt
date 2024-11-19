package com.domanskii

import com.domanskii.plugins.configureAuthentication
import com.domanskii.plugins.configureRouting
import com.domanskii.plugins.configureSerialization
import com.domanskii.providers.GitHub
import com.domanskii.providers.GitHubApiClient
import com.domanskii.services.MarkdownService
import com.domanskii.services.MetricsService
import com.domanskii.storage.DatabaseFactory
import com.domanskii.storage.StorageImpl
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import mu.KotlinLogging
import org.kohsuke.github.GitHubBuilder

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
    val gitHubClient = GitHubBuilder().withOAuthToken(ghToken).build()
    val github = GitHub(ghRepo, ghDefaultBranch, gitHubClient as GitHubApiClient)
    val markdownService = MarkdownService()
    val metricsService = MetricsService(storage, github, markdownService)

    configureAuthentication(secretHeader)
    configureSerialization()
    configureRouting(metricsService)
}

fun assertEnvVariables() {
    log.info { "Asserting env variables..." }
    require(!System.getenv("SECRET_HEADER").isNullOrBlank()) { "SECRET_HEADER environment variable is missing or blank" }
    require(!System.getenv("GH_REPO").isNullOrBlank()) { "GH_REPO environment variable is missing or blank" }
    require(!System.getenv("GH_TOKEN").isNullOrBlank()) { "GH_TOKEN environment variable is missing or blank" }
    require(!System.getenv("GH_DEFAULT_BRANCH").isNullOrBlank()) { "GH_DEFAULT_BRANCH environment variable is missing or blank" }
    require(!System.getenv("DB_HOST").isNullOrBlank()) { "DB_HOST environment variable is missing or blank" }
    require(!System.getenv("DB_NAME").isNullOrBlank()) { "DB_NAME environment variable is missing or blank" }
    require(!System.getenv("DB_USERNAME").isNullOrBlank()) { "DB_USERNAME environment variable is missing or blank" }
    require(!System.getenv("DB_PASSWORD").isNullOrBlank()) { "DB_PASSWORD environment variable is missing or blank" }
}
