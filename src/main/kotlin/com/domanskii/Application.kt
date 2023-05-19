package com.domanskii

import com.domanskii.dao.DAOFacadeImpl
import com.domanskii.dao.DatabaseFactory
import com.domanskii.github.GitHub
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.domanskii.plugins.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun main() {
    log.info { "Application starting..." }
    assertEnvVariables()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
    log.info { "Application finished" }
}

fun Application.module() {
    configureAuthentication()
    configureSerialization()
    DatabaseFactory.init()
    val dao = DAOFacadeImpl()
    val github = GitHub(System.getenv("GH_TOKEN"), dao, System.getenv("GH_REPO"), System.getenv("GH_DEFAULT_BRANCH"))
    configureRouting(dao, github)
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
