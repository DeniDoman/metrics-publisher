package com.domanskii

import com.domanskii.auth.GitHubAppAuth
import com.domanskii.plugins.configureAuthentication
import com.domanskii.plugins.configureRouting
import com.domanskii.plugins.configureSerialization
import com.domanskii.providers.GitHub
import com.domanskii.services.MarkdownService
import com.domanskii.services.MetricsService
import com.domanskii.storage.DatabaseFactory
import com.domanskii.storage.StorageImpl
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import mu.KotlinLogging
import java.nio.file.Files
import kotlin.io.path.absolutePathString

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
    val ghAppId = System.getenv("GH_APP_ID")
    val ghInstallationId = System.getenv("GH_APP_INSTALLATION_ID")
    val ghPrivateKey = System.getenv("GH_APP_PRIVATE_KEY")
    val ghDefaultBranch = System.getenv("GH_DEFAULT_BRANCH")
    val dbHost = System.getenv("DB_HOST")
    val dbName = System.getenv("DB_NAME")
    val dbUsername = System.getenv("DB_USERNAME")
    val dbPassword = System.getenv("DB_PASSWORD")

    DatabaseFactory.init(dbHost, dbName, dbUsername, dbPassword)
    val storage = StorageImpl()

    val ghPrivateKeyPath = Files.createTempFile(
        "gh_app_private_key",
        if (ghPrivateKey.startsWith("-----BEGIN")) ".pem" else ".key"
    ).apply {
        Files.writeString(this, ghPrivateKey)
    }.absolutePathString()
    
    // Create GitHub App authenticator and get authenticated client
    val gitHubAuth = GitHubAppAuth(
        appId = ghAppId,
        privateKeyPath = ghPrivateKeyPath,
        installationId = ghInstallationId
    )
    val gitHubClient = gitHubAuth.getAuthenticatedClient()
    
    val github = GitHub(ghRepo, ghDefaultBranch, gitHubClient)
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
    require(!System.getenv("GH_APP_ID").isNullOrBlank()) { "GH_APP_ID environment variable is missing or blank" }
    require(!System.getenv("GH_APP_INSTALLATION_ID").isNullOrBlank()) { "GH_APP_INSTALLATION_ID environment variable is missing or blank" }
    require(!System.getenv("GH_APP_PRIVATE_KEY").isNullOrBlank()) { "GH_APP_PRIVATE_KEY environment variable is missing or blank" }
    require(!System.getenv("GH_DEFAULT_BRANCH").isNullOrBlank()) { "GH_DEFAULT_BRANCH environment variable is missing or blank" }
    require(!System.getenv("DB_HOST").isNullOrBlank()) { "DB_HOST environment variable is missing or blank" }
    require(!System.getenv("DB_NAME").isNullOrBlank()) { "DB_NAME environment variable is missing or blank" }
    require(!System.getenv("DB_USERNAME").isNullOrBlank()) { "DB_USERNAME environment variable is missing or blank" }
    require(!System.getenv("DB_PASSWORD").isNullOrBlank()) { "DB_PASSWORD environment variable is missing or blank" }
}
