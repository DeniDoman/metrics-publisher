package com.domanskii.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*

fun Application.configureAuthentication(secretHeader: String) {
    install(Authentication) {
        bearer {
            realm = "Access to the '/' path"
            authenticate { tokenCredential ->
                if (tokenCredential.token == secretHeader) {
                    UserIdPrincipal("user")
                } else {
                    null
                }
            }
        }
    }
}