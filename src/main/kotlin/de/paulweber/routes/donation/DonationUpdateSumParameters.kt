package de.paulweber.routes.donation

import kotlinx.serialization.Serializable

@Serializable
data class DonationUpdateSumParameters(val paymentIntentId: String, val newSum: Long, val newDonationValue: Long)
