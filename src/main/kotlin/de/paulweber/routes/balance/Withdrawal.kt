package de.paulweber.routes.balance

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Withdrawal(val id: String, val beneficiary: String, val amount: Long, var timestamp: Instant?)
