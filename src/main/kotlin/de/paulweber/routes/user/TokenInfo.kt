package de.paulweber.routes.user

import kotlinx.serialization.Serializable

@Serializable
data class TokenInfo(val identifier: String, val userType: UserType, val email: String?, val accessToken: String, val refreshToken: String?)
