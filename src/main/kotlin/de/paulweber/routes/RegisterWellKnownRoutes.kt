package de.paulweber.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import java.awt.SystemColor.text
import java.io.File

fun Application.registerWellKnownRoutes() {
    routing {
        static(".well-known") {
            staticRootFolder = File("certs")
            file("jwks.json")
            file("apple-app-site-association", "apple-app-site-association.json")
            file("assetlinks.json")
        }
    }
}