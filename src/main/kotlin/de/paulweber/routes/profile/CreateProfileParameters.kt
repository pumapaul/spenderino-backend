package de.paulweber.routes.profile

import kotlinx.serialization.Serializable

@Serializable
data class CreateProfileParameters(val username: String)
