package de.paulweber.routes.user

import kotlinx.serialization.Serializable

@Serializable
data class RegisterParameters(val email: String, val password: String)
