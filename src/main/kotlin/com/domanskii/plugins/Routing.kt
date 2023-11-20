package com.domanskii.plugins

import com.domanskii.services.MetricsService
import com.domanskii.serialization.PostMetricsRequest
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*


fun Application.configureRouting(metricsService: MetricsService) {
    routing {
        authenticate {
            post("/api/v1/metrics") {
                val req = call.receive<PostMetricsRequest>()
                if (req.commitSha.isBlank()) call.respond(HttpStatusCode.BadRequest, "Commit SHA should not be empty!")
                if (req.name.isBlank()) call.respond(HttpStatusCode.BadRequest, "Name should not be empty!")

                call.respond(HttpStatusCode.OK)

                metricsService.processMetric(req.commitSha, req.name, req.value, req.units, req.threshold, req.isIncreaseBad)
            }
        }

        get("/healthz") {
            call.respondText("OK")
        }
    }
}
