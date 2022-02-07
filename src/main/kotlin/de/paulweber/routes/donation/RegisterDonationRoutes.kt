package de.paulweber.routes.donation

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.PaymentIntent
import com.stripe.net.RequestOptions
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.PaymentIntentUpdateParams
import de.paulweber.*
import de.paulweber.plugins.getPrincipalIdentifier
import de.paulweber.routes.transaction.Transaction
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.datetime.Clock

fun Application.registerDonationRoutes() {
    Stripe.apiKey = environment.config.property("stripe.apiKey").getString()

    fun getOrCreateCustomerID(principalIdentifier: String): String {
        return customerStore[principalIdentifier] ?: run {
            val newCustomerId = Customer.create(mapOf()).id
            customerStore[principalIdentifier] = newCustomerId
            newCustomerId
        }
    }

    fun createPaymentIntentParameters(customerId: String): PaymentIntentCreateParams {
        val automaticPaymentMethod = PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
            .setEnabled(true)
            .build()
        return PaymentIntentCreateParams.builder()
            .setCustomer(customerId)
            .setCurrency("eur")
            .setAmount(129)
            .putMetadata("donationValue", "${100}")
            .setAutomaticPaymentMethods(automaticPaymentMethod)
            .build()
    }

    fun getEphemeralKey(customerId: String): String {
        val params = EphemeralKeyCreateParams.builder()
            .setCustomer(customerId)
            .build()
        val requestOptions = RequestOptions.builder()
            .setStripeVersionOverride("2020-08-27")
            .build()
        return EphemeralKey.create(params, requestOptions).secret
    }

    fun storeTransaction(paymentIntent: PaymentIntent, donatorId: String, recipientId: String) {
        val donatorName = profileStore[donatorId]?.username
        val recipientName = profileStore[recipientId]?.username
        val donator = Transaction.Entity(donatorName, donatorId)
        val recipient = Transaction.Entity(recipientName, recipientId)
        val transaction = Transaction(
            paymentIntent.metadata["donationValue"]!!.toLong(),
            paymentIntent.amount,
            paymentIntent.id,
            donator,
            recipient,
            Clock.System.now(),
            Transaction.State.INIT
        )
        transactionStore.add(transaction)
    }

    routing {
        authenticate("anonymous") {
            get("/r/{recipientId}") {
                try {
                    call.parameters["recipientId"]?.let { recipientId ->
                        receiverCodeStore[recipientId]?.let { recipientUserIdentifier ->
                            val principal = call.getPrincipalIdentifier()
                            val customerId = getOrCreateCustomerID(principal)
                            val customerKey = getEphemeralKey(customerId)
                            val params = createPaymentIntentParameters(customerId)
                            val paymentIntent = PaymentIntent.create(params)
                            paymentIntentStore[paymentIntent.id] = paymentIntent
                            val recipientName = profileStore[recipientUserIdentifier]!!.username
                            val response = DonationInformation(
                                Recipient(recipientName),
                                paymentIntent.clientSecret,
                                paymentIntent.id,
                                customerId,
                                customerKey
                            )
                            call.respond(response)
                            storeTransaction(paymentIntent, principal, recipientUserIdentifier)
                        } ?: run {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                } catch (e: Throwable) {
                    println("ERROR: $e")
                    throw(e)
                }
            }

            post("/donation/change-sum") {
                val params = call.receive<DonationUpdateSumParameters>()
                paymentIntentStore[params.paymentIntentId]?.let {
                    val updateParams = PaymentIntentUpdateParams.builder()
                        .putMetadata("donationValue", "${params.newDonationValue}")
                        .setAmount(params.newSum)
                        .build()
                    it.update(updateParams)
                    call.respond(HttpStatusCode.OK)
                } ?: run { call.respond(HttpStatusCode.NotFound) }
            }
        }
    }
}