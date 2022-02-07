package de.paulweber.plugins

import com.auth0.jwk.JwkProviderBuilder
import de.paulweber.routes.user.UserType
import de.paulweber.userStore
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.routing.*
import java.io.File
import java.util.concurrent.TimeUnit

fun Application.configureSecurity() {
    val issuer = environment.config.property("jwt.issuer").getString()
    val jwkProvider = JwkProviderBuilder(issuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    fun configureJWTConfiguration(configuration: JWTAuthenticationProvider.Configuration, userTypes: List<String>) {
        configuration.verifier(jwkProvider, issuer) {
            acceptLeeway(3)
        }
        configuration.validate { credential ->
            val identifierClaim = credential.payload.getClaim("identifier").asString()
            val userTypeClaim = credential.payload.getClaim("userType").asString()
            val userExists = userStore.any { it.identifier == identifierClaim }
            val isCorrectUserType = userTypes.contains(userTypeClaim)
            if (userExists && isCorrectUserType) {
                JWTPrincipal(credential.payload)
            } else {
                null
            }
        }
    }

    install(Authentication) {
        jwt("anonymous") {
            realm = "anonymous user routes"
            val userTypes = UserType.values().map { it.toString() }
            configureJWTConfiguration(this, userTypes)
        }
        jwt("registered") {
            realm = "registered user routes"
            val userTypes = listOf(UserType.REGISTERED.toString())
            configureJWTConfiguration(this, userTypes)
        }
    }
}

fun ApplicationCall.getPrincipalIdentifier(): String {
    return this.principal<JWTPrincipal>()?.getClaim("identifier", String::class)
        ?: throw InternalError("no jwt principal")
}