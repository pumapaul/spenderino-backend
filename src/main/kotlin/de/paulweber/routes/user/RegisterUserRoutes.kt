package de.paulweber.routes.user

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import de.paulweber.refreshTokenStore
import de.paulweber.userStore
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.text.toCharArray

fun Application.registerUserRoutes() {
    val privateKeyString = environment.config.property("jwt.privateKey").getString()
    val issuer = environment.config.property("jwt.issuer").getString()
    val jwkProvider = JwkProviderBuilder(issuer)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()


    fun createJWTHelper(user: User): JWTCreator.Builder {
        return JWT.create()
            .withIssuer(issuer)
            .withClaim("identifier", user.identifier)
            .withClaim("userType", user.getType().toString())
    }

    fun signJWT(builder: JWTCreator.Builder): String {
        val publicKey = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKey
        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString))
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)

        return builder.sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))
    }

    fun createRegisteredJWT(user: User): String {
        return signJWT(
            createJWTHelper(user)
                .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        )
    }

    fun createAnonymousJWT(user: User): String {
        return signJWT(createJWTHelper(user))
    }

    fun createJWT(user: User): String {
        return when (user) {
            is User.Anonymous -> createAnonymousJWT(user)
            is User.Registered -> createRegisteredJWT(user)
        }
    }

    fun createRefreshToken(user: User): String? {
        return if (user is User.Registered) {
            val refresh = UUID.randomUUID().toString()
            refreshTokenStore[user.identifier]?.add(refresh) ?: run {
                refreshTokenStore[user.identifier] = mutableSetOf(refresh)
            }
            refresh
        } else {
            null
        }
    }

    fun createTokenInfo(user: User): TokenInfo {
        val jwt = createJWT(user)
        val refreshToken = createRefreshToken(user)
        val email = if (user is User.Registered) user.email else null
        return TokenInfo(user.identifier, user.getType(), email, jwt, refreshToken)
    }


    routing {

        route("/user") {

            route("/register") {

                post("/anonymous") {
                    val newUser = User.Anonymous.factoryCreate()
                    userStore.add(newUser)
                    val tokenInfo = createTokenInfo(newUser)
                    call.respond(tokenInfo)
                }

                post("/email") {
                    val params = call.receive<RegisterParameters>()
                    if (params.password.length < 6) {
                        call.respond(HttpStatusCode.BadRequest, "password needs to be 6 or more characters")
                    } else {
                        val isDuplicate = userStore.any { it is User.Registered && it.email == params.email }

                        if (isDuplicate) {
                            call.respond(HttpStatusCode.Conflict, "user with that email already exists")
                        } else {
                            val newUser = User.Registered.factoryCreate(params.email, params.password)
                            userStore.add(newUser)
                            val tokenInfo = createTokenInfo(newUser)
                            call.respond(tokenInfo)
                        }
                    }
                }

            }

            post("/login") {
                val params = call.receive<LoginParameters>()

                userStore.find { it is User.Registered && it.email == params.email }?.let {
                    val registeredUser = it as User.Registered
                    val result = BCrypt.verifyer().verify(params.password.toCharArray(), registeredUser.hashedPassword)
                    if (result.verified) {
                        val tokenInfo = createTokenInfo(it)
                        call.respond(tokenInfo)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "wrong username or password")
                    }
                } ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "wrong username or password")
                }
            }

            post("/refresh") {
                val parameters = call.receive<RefreshParameters>()

                userStore.find { it.identifier == parameters.identifier }?.let {
                    if (refreshTokenStore[it.identifier]?.contains(parameters.refreshToken) == true) {
                        refreshTokenStore[it.identifier]?.remove(parameters.refreshToken)
                        val tokenInfo = createTokenInfo(it)
                        call.respond(tokenInfo)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "unknown refresh token")
                    }
                } ?: run {
                    call.respond(HttpStatusCode.Unauthorized, "unknown user")
                }
            }
        }
    }
}
