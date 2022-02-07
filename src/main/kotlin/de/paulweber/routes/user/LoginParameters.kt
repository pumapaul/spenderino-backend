package de.paulweber.routes.user

import kotlinx.serialization.Serializable

@Serializable
data class LoginParameters(val email: String, val password: String)
