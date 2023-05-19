package com.domanskii.plugins

import com.domanskii.dao.DAOFacadeImpl
import com.domanskii.github.GitHub
import com.domanskii.json.PostMetricsRequest
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun Application.configureRouting(dao: DAOFacadeImpl, gitHub: GitHub) {
    routing {
        authenticate {
            post("/api/v1/metrics") {
                val req = call.receive<PostMetricsRequest>()
                if (req.commitSha.isBlank()) call.respond(HttpStatusCode.BadRequest, "Commit SHA should not be empty!")
                if (req.name.isBlank()) call.respond(HttpStatusCode.BadRequest, "Name should not be empty!")

                call.respond(HttpStatusCode.OK)

                val isReference = gitHub.isMergeToMasterCommit(req.commitSha)
                dao.postMetric(req.commitSha, req.name, req.value, req.units, req.threshold, isReference, req.isIncreaseBad)

                // Merge-to-master-commit doesn't have open PRs to post metrics
                if (!isReference) {
                    launch(Dispatchers.IO) {
                        gitHub.addMetricsToPrMessage(req.commitSha)
                    }
                }
            }
        }

        get("/healthz") {
            call.respondText("OK")
        }
    }
}
