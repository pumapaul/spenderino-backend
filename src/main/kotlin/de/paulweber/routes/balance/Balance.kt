package de.paulweber.routes.balance

import kotlinx.serialization.Serializable

@Serializable
data class Balance(val amount: Long, val withdrawal: Withdrawal?, val pastWithdrawals: List<Withdrawal>)

