package de.paulweber.routes.user

import kotlinx.serialization.Serializable

@Serializable
data class RefreshParameters(val identifier: String, val refreshToken: String)
