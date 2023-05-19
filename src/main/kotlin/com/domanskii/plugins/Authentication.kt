package com.domanskii.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication() {
    install(Authentication) {
        bearer {
            realm = "Access to the '/' path"
            authenticate { tokenCredential ->
                if (tokenCredential.token == System.getenv("SECRET_HEADER")) {
                    UserIdPrincipal("user")
                } else {
                    null
                }
            }
        }
    }
}