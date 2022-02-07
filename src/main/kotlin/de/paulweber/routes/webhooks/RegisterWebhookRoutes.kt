package de.paulweber.routes.webhooks

import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import de.paulweber.profileStore
import de.paulweber.routes.transaction.Transaction
import de.paulweber.transactionStore
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.registerWebhookRoutes() {
    val webhookSecret = environment.config.property("stripe.webhookSecret").getString()
    val signatureHeader = "Stripe-Signature"

    fun onPaymentIntentSuccess(event: Event) {
        (event.dataObjectDeserializer.`object`?.get() as PaymentIntent).let { paymentIntent ->
            transactionStore.find { it.paymentIntentId == paymentIntent.id }?.let { transaction ->
                val donationValue = paymentIntent.metadata["donationValue"]!!.toLong()
                transaction.amount = donationValue
                transaction.amountWithFees = paymentIntent.amount
                transaction.state = Transaction.State.COMPLETE
                profileStore[transaction.recipient.id]?.balance?.let {
                    profileStore[transaction.recipient.id]?.balance = it + donationValue
                }
            }
        }
    }

    fun onPaymentIntentProcessing(event: Event) {
        (event.dataObjectDeserializer.`object`?.get() as PaymentIntent).let { paymentIntent ->
            transactionStore.find { it.paymentIntentId == paymentIntent.id }?.let { transaction ->
                transaction.amountWithFees = paymentIntent.amount
                transaction.amount = paymentIntent.metadata["donationValue"]!!.toLong()
                transaction.state = Transaction.State.PENDING
            }
        }
    }

    fun onPaymentIntentFailed(event: Event) {
        (event.dataObjectDeserializer.`object`?.get() as PaymentIntent).let { paymentIntent ->
            transactionStore.find { it.paymentIntentId == paymentIntent.id }?.let { transaction ->
                if (transaction.state == Transaction.State.PENDING) {
                    transaction.state = Transaction.State.FAILED
                }
                transaction.amountWithFees = paymentIntent.amount
                transaction.amount = paymentIntent.metadata["donationValue"]!!.toLong()
            }
        }
    }

    fun handleStripeEvent(event: Event) {
        when (event.type) {
            "payment_intent.succeeded" -> onPaymentIntentSuccess(event)
            "payment_intent.payment_failed" -> onPaymentIntentFailed(event)
            "payment_intent.processing" -> onPaymentIntentProcessing(event)
            else -> Unit
        }
    }

    routing {
        post("/webhooks") {
            call.request.headers[signatureHeader]?.let { signature ->
                val payload = call.receive<String>()
                Webhook.constructEvent(payload, signature, webhookSecret)?.let {
                    handleStripeEvent(it)
                    call.respond(HttpStatusCode.OK)
                } ?: run { call.respond(HttpStatusCode.BadRequest) }
            } ?: run { call.respond(HttpStatusCode.BadRequest) }
        }
    }
}