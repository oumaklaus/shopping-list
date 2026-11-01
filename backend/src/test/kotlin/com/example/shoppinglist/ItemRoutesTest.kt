package com.example.shoppinglist

import com.example.shoppinglist.model.AuthResponse
import com.example.shoppinglist.model.Item
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `health endpoint remains public`() = testApplication {
        application {
            module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET)
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"status":"ok"}""", response.bodyAsText())
    }

    @Test
    fun `items reject requests without a bearer token`() = testApplication {
        application {
            module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET)
        }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/items").status)
    }

    @Test
    fun `authenticated user can create update and delete an item`() = testApplication {
        application {
            module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET)
        }
        val token = registerAndGetToken("shopper@example.com")

        val createResponse = client.post("/items") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Milk","quantity":2}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = json.decodeFromString<Item>(createResponse.bodyAsText())
        assertEquals("Milk", created.name)
        assertFalse(created.purchased)

        val updateResponse = client.put("/items/${created.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Oat milk","quantity":3,"purchased":true}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(json.decodeFromString<Item>(updateResponse.bodyAsText()).purchased)

        assertEquals(
            HttpStatusCode.NoContent,
            client.delete("/items/${created.id}") { bearerAuth(token) }.status
        )
        val remaining = client.get("/items") { bearerAuth(token) }
        assertEquals(emptyList(), json.decodeFromString<List<Item>>(remaining.bodyAsText()))
    }

    @Test
    fun `users only see their own shopping items`() = testApplication {
        application {
            module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET)
        }
        val firstToken = registerAndGetToken("first@example.com")
        val secondToken = registerAndGetToken("second@example.com")
        createItem(firstToken, "First user's milk")
        createItem(secondToken, "Second user's bread")

        val firstItems = client.get("/items") { bearerAuth(firstToken) }
        val secondItems = client.get("/items") { bearerAuth(secondToken) }

        assertEquals(
            listOf("First user's milk"),
            json.decodeFromString<List<Item>>(firstItems.bodyAsText()).map(Item::name)
        )
        assertEquals(
            listOf("Second user's bread"),
            json.decodeFromString<List<Item>>(secondItems.bodyAsText()).map(Item::name)
        )
    }

    @Test
    fun `user cannot update or delete another users item`() = testApplication {
        application {
            module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET)
        }
        val ownerToken = registerAndGetToken("owner@example.com")
        val otherToken = registerAndGetToken("other@example.com")
        val item = createItem(ownerToken, "Private item")

        val update = client.put("/items/${item.id}") {
            bearerAuth(otherToken)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Stolen","quantity":1,"purchased":true}""")
        }
        val delete = client.delete("/items/${item.id}") { bearerAuth(otherToken) }

        assertEquals(HttpStatusCode.NotFound, update.status)
        assertEquals(HttpStatusCode.NotFound, delete.status)
        val ownerItems = client.get("/items") { bearerAuth(ownerToken) }
        assertEquals(
            listOf("Private item"),
            json.decodeFromString<List<Item>>(ownerItems.bodyAsText()).map(Item::name)
        )
    }

    @Test
    fun `post item still validates input`() = testApplication {
        application {
            module(InMemoryItemRepository(), InMemoryUserRepository(), TEST_JWT_SECRET)
        }
        val token = registerAndGetToken("shopper@example.com")

        val response = client.post("/items") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"  ","quantity":0}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    private suspend fun ApplicationTestBuilder.registerAndGetToken(email: String): String {
        val response = register(email, "correct-horse")
        assertEquals(HttpStatusCode.Created, response.status)
        return json.decodeFromString<AuthResponse>(response.bodyAsText()).token
    }

    private suspend fun ApplicationTestBuilder.createItem(token: String, name: String): Item {
        val response = client.post("/items") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"name":"$name","quantity":1}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return json.decodeFromString(response.bodyAsText())
    }
}

suspend fun ApplicationTestBuilder.register(email: String, password: String): HttpResponse =
    client.post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody("""{"email":"$email","password":"$password"}""")
    }
