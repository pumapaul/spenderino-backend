package de.paulweber.routes.transaction

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    var amount: Long,
    var amountWithFees: Long,
    val paymentIntentId: String,
    val donator: Entity,
    val recipient: Entity,
    val timestamp: Instant,
    var state: State
) {
    @Serializable
    data class Entity(val username: String?, val id: String)

    enum class State { INIT, PENDING, FAILED, COMPLETE }

    override fun hashCode(): Int {
        return paymentIntentId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transaction

        if (paymentIntentId != other.paymentIntentId) return false

        return true
    }
}
