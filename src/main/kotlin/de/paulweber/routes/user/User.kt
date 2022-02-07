package de.paulweber.routes.user

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
enum class UserType {
    ANONYMOUS, REGISTERED
}

sealed class User(val identifier: String) {
    class Anonymous private constructor(identifier: String): User(identifier) {
        companion object {
            fun factoryCreate(): Anonymous {
                val id = UUID.randomUUID().toString()
                return Anonymous(id)
            }
        }
    }

    class Registered private constructor(identifier: String, val email: String, val hashedPassword: ByteArray): User(identifier) {
        companion object {
            fun factoryCreate(email: String, password: String): Registered {
                val id = UUID.randomUUID().toString()
                val hashedPassword = BCrypt.withDefaults().hash(6, password.toCharArray())

                return Registered(id, email, hashedPassword)
            }
        }
    }

    fun getType(): UserType {
        return when (this) {
            is Anonymous -> UserType.ANONYMOUS
            is Registered -> UserType.REGISTERED
        }
    }
}

