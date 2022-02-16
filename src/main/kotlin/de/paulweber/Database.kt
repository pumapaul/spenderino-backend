package de.paulweber

import com.stripe.model.PaymentIntent
import de.paulweber.routes.balance.Withdrawal
import de.paulweber.routes.profile.Profile
import de.paulweber.routes.transaction.Transaction
import de.paulweber.routes.user.User

val userStore = mutableSetOf<User>(User.Registered.factoryCreate("apple@example.com", "aasdzuhjbnmhuawezajsd65"))
val refreshTokenStore = mutableMapOf<String, MutableSet<String>>()
val receiverCodeStore = mutableMapOf<String, String>()
val profileStore = mutableMapOf<String, Profile>()
val transactionStore = mutableSetOf<Transaction>()
val withdrawalStore = mutableMapOf<String, Withdrawal>()
val pastWithdrawalStore = mutableMapOf<String, MutableList<Withdrawal>>()

val customerStore = mutableMapOf<String, String>()
val paymentIntentStore = mutableMapOf<String, PaymentIntent>()
