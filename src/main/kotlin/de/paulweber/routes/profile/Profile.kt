package de.paulweber.routes.profile

import de.paulweber.getBaseUrl
import de.paulweber.receiverCodeStore
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Profile(val username: String, val recipientCode: String, var balance: Long) {
    companion object {
        fun create(username: String, identifier: String): Profile {
            val uuid = UUID.randomUUID().toString()
            val recipientCode = "${getBaseUrl()}/r/$uuid"
            receiverCodeStore[uuid] = identifier
            return Profile(username, recipientCode, 0)
        }
    }
}
