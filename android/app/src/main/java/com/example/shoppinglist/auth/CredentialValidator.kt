package com.example.shoppinglist.auth

object CredentialValidator {
    fun validate(rawEmail: String, rawPassword: String): CredentialResult {
        val email = rawEmail.trim().lowercase()
        val emailError = if (EMAIL_REGEX.matches(email)) null else CredentialError.INVALID_EMAIL
        val passwordError = if (
            rawPassword.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
            rawPassword.toByteArray(Charsets.UTF_8).size <= MAX_PASSWORD_BYTES
        ) {
            null
        } else {
            CredentialError.INVALID_PASSWORD
        }

        return if (emailError == null && passwordError == null) {
            CredentialResult.Valid(email, rawPassword)
        } else {
            CredentialResult.Invalid(emailError, passwordError)
        }
    }

    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private const val MIN_PASSWORD_LENGTH = 8
    private const val MAX_PASSWORD_LENGTH = 72
    private const val MAX_PASSWORD_BYTES = 72
}

enum class CredentialError {
    INVALID_EMAIL,
    INVALID_PASSWORD
}

sealed interface CredentialResult {
    data class Valid(val email: String, val password: String) : CredentialResult
    data class Invalid(
        val emailError: CredentialError? = null,
        val passwordError: CredentialError? = null
    ) : CredentialResult
}
