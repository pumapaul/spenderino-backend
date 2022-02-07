package de.paulweber.routes.transaction

import de.paulweber.pastWithdrawalStore
import de.paulweber.plugins.getPrincipalIdentifier
import de.paulweber.transactionStore
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.registerTransactionRoutes() {
    routing {
        authenticate("anonymous") {
            get("/transactions") {
                val principal = call.getPrincipalIdentifier()

                val transactions = transactionStore.filter {
                    it.state != Transaction.State.INIT
                            && (it.donator.id == principal || it.recipient.id == principal)
                }
                call.respond(transactions)
            }
        }
        authenticate("registered") {
            get("withdrawals") {
                val principal = call.getPrincipalIdentifier()
                val withdrawals = pastWithdrawalStore[principal] ?: listOf()
                call.respond(withdrawals)
            }
        }
    }
}