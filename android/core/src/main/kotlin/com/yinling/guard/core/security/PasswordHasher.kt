package com.yinling.guard.core.security

import java.security.MessageDigest

object PasswordHasher {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return "sha256:" + bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(password: String, hash: String?): Boolean {
        if (hash.isNullOrBlank()) return false
        return hash == hash(password)
    }
}
