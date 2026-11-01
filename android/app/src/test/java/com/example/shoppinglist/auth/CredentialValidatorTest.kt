package com.example.shoppinglist.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialValidatorTest {

    @Test
    fun `valid credentials are normalized`() {
        val result = CredentialValidator.validate(" Shopper@Example.com ", "correct-horse")

        assertTrue(result is CredentialResult.Valid)
        result as CredentialResult.Valid
        assertEquals("shopper@example.com", result.email)
        assertEquals("correct-horse", result.password)
    }

    @Test
    fun `invalid email is rejected`() {
        val result = CredentialValidator.validate("not-an-email", "correct-horse")

        assertEquals(
            CredentialError.INVALID_EMAIL,
            (result as CredentialResult.Invalid).emailError
        )
    }

    @Test
    fun `password shorter than eight characters is rejected`() {
        val result = CredentialValidator.validate("shopper@example.com", "short")

        assertEquals(
            CredentialError.INVALID_PASSWORD,
            (result as CredentialResult.Invalid).passwordError
        )
    }

    @Test
    fun `password beyond bcrypt utf8 byte limit is rejected`() {
        val ascii = CredentialValidator.validate("shopper@example.com", "a".repeat(73))
        val unicode = CredentialValidator.validate("shopper@example.com", "界".repeat(25))

        assertEquals(
            CredentialError.INVALID_PASSWORD,
            (ascii as CredentialResult.Invalid).passwordError
        )
        assertEquals(
            CredentialError.INVALID_PASSWORD,
            (unicode as CredentialResult.Invalid).passwordError
        )
    }
}
