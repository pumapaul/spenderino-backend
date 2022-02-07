package de.paulweber

import de.paulweber.plugins.*
import de.paulweber.routes.balance.registerBalanceRoutes
import de.paulweber.routes.donation.registerDonationRoutes
import de.paulweber.routes.profile.registerProfileRoutes
import de.paulweber.routes.registerWellKnownRoutes
import de.paulweber.routes.transaction.registerTransactionRoutes
import de.paulweber.routes.user.registerUserRoutes
import de.paulweber.routes.webhooks.registerWebhookRoutes
import io.ktor.application.*
import io.ktor.features.*
import org.slf4j.event.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
    }
    configureRouting()
    configureStatusPages()
    configureSecurity()
    configureSerialization()

    registerWellKnownRoutes()
    registerUserRoutes()
    registerProfileRoutes()
    registerDonationRoutes()
    registerWebhookRoutes()
    registerTransactionRoutes()
    registerBalanceRoutes()
}

fun getBaseUrl(): String {
//    return "http://192.168.69.4:8080"
    val host = "spenderino.herokuapp.com"
    return "https://$host"
}