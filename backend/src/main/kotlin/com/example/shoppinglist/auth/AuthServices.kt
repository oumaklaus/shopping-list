package com.example.shoppinglist.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.shoppinglist.model.AuthResponse
import com.example.shoppinglist.model.AuthUser
import com.example.shoppinglist.model.User
import com.example.shoppinglist.repository.UserRepository
import java.util.Date
import kotlin.text.Charsets.UTF_8

interface PasswordHasher {
    fun hash(password: String): String
    fun verify(password: String, passwordHash: String): Boolean
}

class BCryptPasswordHasher : PasswordHasher {
    override fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())

    override fun verify(password: String, passwordHash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), passwordHash).verified

    private companion object {
        const val BCRYPT_COST = 12
    }
}

class JwtService(secret: String) {
    init {
        require(secret.length >= MIN_SECRET_LENGTH) {
            "JWT secret must be at least $MIN_SECRET_LENGTH characters"
        }
    }

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()

    fun createToken(user: User): String = JWT.create()
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .withSubject(user.id)
        .withClaim("email", user.email)
        .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_LIFETIME_MILLIS))
        .sign(algorithm)

    companion object {
        const val ISSUER = "shopping-list-backend"
        const val AUDIENCE = "shopping-list-android"
        const val REALM = "shopping-list"
        private const val MIN_SECRET_LENGTH = 32
        private const val TOKEN_LIFETIME_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}

class AuthService(
    private val users: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService
) {
    suspend fun register(email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        validate(normalizedEmail, password)?.let { return AuthResult.Invalid(it) }

        val user = users.create(normalizedEmail, passwordHasher.hash(password))
            ?: return AuthResult.DuplicateEmail
        return AuthResult.Success(user.toResponse(jwtService.createToken(user)))
    }

    suspend fun login(email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        if (!passwordIsValid(password)) return AuthResult.InvalidCredentials
        val user = users.findByEmail(normalizedEmail) ?: return AuthResult.InvalidCredentials
        if (!passwordHasher.verify(password, user.passwordHash)) {
            return AuthResult.InvalidCredentials
        }
        return AuthResult.Success(user.toResponse(jwtService.createToken(user)))
    }

    private fun validate(email: String, password: String): String? = when {
        !EMAIL_REGEX.matches(email) -> "Enter a valid email address"
        !passwordIsValid(password) ->
            "Password must be 8 to 72 characters and at most 72 UTF-8 bytes"
        else -> null
    }

    private fun passwordIsValid(password: String): Boolean =
        password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
            password.toByteArray(UTF_8).size <= MAX_PASSWORD_BYTES

    private fun User.toResponse(token: String) = AuthResponse(
        token = token,
        user = AuthUser(id = id, email = email)
    )

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 72
        const val MAX_PASSWORD_BYTES = 72
    }
}

sealed interface AuthResult {
    data class Success(val response: AuthResponse) : AuthResult
    data class Invalid(val message: String) : AuthResult
    data object DuplicateEmail : AuthResult
    data object InvalidCredentials : AuthResult
}
