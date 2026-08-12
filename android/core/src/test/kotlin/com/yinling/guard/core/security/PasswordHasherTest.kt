package com.yinling.guard.core.security

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {

    @Test
    fun `hash produces consistent result`() {
        val hash1 = PasswordHasher.hash("password123")
        val hash2 = PasswordHasher.hash("password123")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hash produces different result for different input`() {
        val hash1 = PasswordHasher.hash("password1")
        val hash2 = PasswordHasher.hash("password2")
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `hash starts with sha256 prefix`() {
        val hash = PasswordHasher.hash("test")
        assertTrue(hash.startsWith("sha256:"))
    }

    @Test
    fun `hash is 71 chars total`() {
        val hash = PasswordHasher.hash("test")
        assertEquals(71, hash.length) // "sha256:" (7) + 64 hex chars
    }

    @Test
    fun `verify returns true for correct password`() {
        val hash = PasswordHasher.hash("mypassword")
        assertTrue(PasswordHasher.verify("mypassword", hash))
    }

    @Test
    fun `verify returns false for wrong password`() {
        val hash = PasswordHasher.hash("mypassword")
        assertFalse(PasswordHasher.verify("wrongpassword", hash))
    }

    @Test
    fun `verify returns false for null hash`() {
        assertFalse(PasswordHasher.verify("password", null))
    }

    @Test
    fun `verify returns false for blank hash`() {
        assertFalse(PasswordHasher.verify("password", ""))
    }

    @Test
    fun `verify returns false for empty password`() {
        val hash = PasswordHasher.hash("password")
        assertFalse(PasswordHasher.verify("", hash))
    }

    @Test
    fun `hash handles unicode characters`() {
        val hash = PasswordHasher.hash("密码测试🔐")
        assertTrue(hash.startsWith("sha256:"))
        assertTrue(PasswordHasher.verify("密码测试🔐", hash))
    }

    @Test
    fun `hash handles very long password`() {
        val longPassword = "a".repeat(10000)
        val hash = PasswordHasher.hash(longPassword)
        assertTrue(PasswordHasher.verify(longPassword, hash))
    }
}
