package de.paulweber.routes.profile

import de.paulweber.getBaseUrl
import de.paulweber.plugins.getPrincipalIdentifier
import de.paulweber.profileStore
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.registerProfileRoutes() {
    routing {
        authenticate("registered") {
            route("/profile") {
                get {
                    val identifier = call.getPrincipalIdentifier()
                    profileStore[identifier]?.let {
                        call.respond(it)
                    } ?: run {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                get("/qrcode") {
                    val identifier = call.getPrincipalIdentifier()
                    profileStore[identifier]?.let {
                        val code = QRCodeGenerator.generate(it.recipientCode)
                        call.respondBytes(code)
                    } ?: run {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                post("/create") {
                    val params = call.receive<CreateProfileParameters>()
                    if (params.username == "") {
                        call.respond(HttpStatusCode.BadRequest, "illegal username")
                    } else {
                        val identifier = call.getPrincipalIdentifier()
                        val profile = Profile.create(params.username, identifier)
                        profileStore[identifier] = profile
                        call.respond(profile)
                    }
                }
            }
        }
    }
}