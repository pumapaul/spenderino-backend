package de.paulweber.routes.balance

import de.paulweber.getBaseUrl
import de.paulweber.pastWithdrawalStore
import de.paulweber.plugins.getPrincipalIdentifier
import de.paulweber.profileStore
import de.paulweber.routes.profile.QRCodeGenerator
import de.paulweber.withdrawalStore
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.datetime.Clock
import java.util.*

fun Application.registerBalanceRoutes() {
    routing {
        authenticate("registered") {
            get("/balance") {
                val principal = call.getPrincipalIdentifier()
                profileStore[principal]?.let { profile ->
                    val pastWithdrawals = pastWithdrawalStore[principal] ?: listOf()
                    val sortedWithdrawals = pastWithdrawals.sortedBy { it.timestamp }
                    val currentWithdrawal = withdrawalStore[principal]
                    val balance = Balance(profile.balance, currentWithdrawal, sortedWithdrawals)
                    call.respond(balance)
                } ?: run { call.respond(HttpStatusCode.NotFound, "no profile found for this user") }
            }

            route("/withdrawal") {

                get {
                    val principal = call.getPrincipalIdentifier()
                    withdrawalStore[principal]?.let {
                        call.respond(true)
                    } ?: run { call.respond(false) }
                }

                post("/create/{amount}") {
                    call.parameters["amount"]?.toLong()?.let { amount ->
                        val principal = call.getPrincipalIdentifier()
                        if ((profileStore[principal]?.balance ?: 0) < amount) {
                            call.respond(HttpStatusCode.BadRequest, "insufficient funds")
                        } else {
                            val id = UUID.randomUUID().toString()
                            val withdrawal = Withdrawal(id, principal, amount, null)
                            withdrawalStore[principal] = withdrawal
                            call.respond(withdrawal)
                        }
                    } ?: run { call.respond(HttpStatusCode.BadRequest) }
                }

                post("/cancel") {
                    val principal = call.getPrincipalIdentifier()

                    withdrawalStore.remove(principal)?.let {
                        call.respond(HttpStatusCode.OK)
                    } ?: run { call.respond(HttpStatusCode.NotFound) }
                }

                get("/qrcode") {
                    val identifier = call.getPrincipalIdentifier()
                    withdrawalStore[identifier]?.let {
                        val url = "${getBaseUrl()}/w/${it.id}"
                        val code = QRCodeGenerator.generate(url)
                        call.respondBytes(code)
                    } ?: run {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }

        get("/w/{withdrawalId}") {
            call.parameters["withdrawalId"]?.let { withdrawalId ->
                val foundPair = withdrawalStore.filter { it.value.id == withdrawalId }.asSequence().firstOrNull()
                foundPair?.key?.let { withdrawalKey ->
                    val withdrawal = withdrawalStore.remove(withdrawalKey)!!
                    profileStore[withdrawal.beneficiary]?.apply {
                        this.balance -= withdrawal.amount
                    }
                    withdrawal.timestamp = Clock.System.now()
                    if (pastWithdrawalStore[withdrawal.beneficiary] == null) {
                        pastWithdrawalStore[withdrawal.beneficiary] = mutableListOf(withdrawal)
                    } else {
                        pastWithdrawalStore[withdrawal.beneficiary]!!.add(withdrawal)
                    }
                    call.respond("You should pay the beneficiary ${withdrawal.amount} imaginary Euros.")
                } ?: run { call.respond(HttpStatusCode.NotFound) }
            } ?: run { call.respond(HttpStatusCode.BadRequest) }
        }
    }
}