package com.example.shoppinglist

import com.example.shoppinglist.auth.JwtService
import com.example.shoppinglist.model.AuthResponse
import com.example.shoppinglist.model.ErrorResponse
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class AuthRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `jwt service rejects a weak signing secret`() {
        assertFailsWith<IllegalArgumentException> { JwtService("too-short") }
    }

    @Test
    fun `register creates an account and returns a bearer token`() = testApplication {
        val users = InMemoryUserRepository()
        application { module(InMemoryItemRepository(), users, TEST_JWT_SECRET) }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":" Shopper@Example.com ","password":"correct-horse"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val auth = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals("shopper@example.com", auth.user.email)
        assertTrue(auth.token.isNotBlank())
        val stored = users.findByEmail("shopper@example.com")
        assertTrue(stored != null)
        assertNotEquals("correct-horse", stored.passwordHash)
    }

    @Test
    fun `register rejects a duplicate email regardless of case`() = testApplication {
        application { module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET) }
        register("shopper@example.com", "correct-horse")

        val response = register("SHOPPER@example.com", "different-pass")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(
            "Email is already registered",
            json.decodeFromString<ErrorResponse>(response.bodyAsText()).error
        )
    }

    @Test
    fun `login returns a token for valid credentials`() = testApplication {
        application { module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET) }
        register("shopper@example.com", "correct-horse")

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"SHOPPER@example.com","password":"correct-horse"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val auth = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals("shopper@example.com", auth.user.email)
        assertTrue(auth.token.isNotBlank())
    }

    @Test
    fun `login rejects an incorrect password without revealing which credential failed`() =
        testApplication {
            application {
                module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET)
            }
            register("shopper@example.com", "correct-horse")

            val response = client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"shopper@example.com","password":"wrong-password"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals(
                "Invalid email or password",
                json.decodeFromString<ErrorResponse>(response.bodyAsText()).error
            )
        }

    @Test
    fun `register validates email and password strength`() = testApplication {
        application { module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET) }

        val invalidEmail = register("not-an-email", "correct-horse")
        val shortPassword = register("shopper@example.com", "short")

        assertEquals(HttpStatusCode.BadRequest, invalidEmail.status)
        assertEquals(HttpStatusCode.BadRequest, shortPassword.status)
    }

    @Test
    fun `register rejects passwords beyond bcrypts utf8 byte limit`() = testApplication {
        application { module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET) }

        val asciiTooLong = register("ascii@example.com", "a".repeat(73))
        val unicodeTooLong = register("unicode@example.com", "界".repeat(25))

        assertEquals(HttpStatusCode.BadRequest, asciiTooLong.status)
        assertEquals(HttpStatusCode.BadRequest, unicodeTooLong.status)
    }
}

const val TEST_JWT_SECRET = "test-secret-that-is-long-enough-for-hmac-signing"
