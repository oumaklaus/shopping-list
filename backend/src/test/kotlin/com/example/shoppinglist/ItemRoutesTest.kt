package com.example.shoppinglist

import com.example.shoppinglist.model.Item
import com.example.shoppinglist.repository.ItemRepository
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val repository = InMemoryItemRepository()

    @BeforeTest
    fun setUp() {
        repository.clear()
    }

    @Test
    fun `health endpoint reports ok`() = testApplication {
        application { module(repository) }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"status":"ok"}""", response.bodyAsText())
    }

    @Test
    fun `get items returns empty list initially`() = testApplication {
        application { module(repository) }

        val response = client.get("/items")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), json.decodeFromString<List<Item>>(response.bodyAsText()))
    }

    @Test
    fun `post item creates item and returns it`() = testApplication {
        application { module(repository) }

        val response = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Milk","quantity":2}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val item = json.decodeFromString<Item>(response.bodyAsText())
        assertEquals("Milk", item.name)
        assertEquals(2, item.quantity)
        assertFalse(item.purchased)
        assertTrue(item.id.isNotBlank())
    }

    @Test
    fun `post item rejects blank name`() = testApplication {
        application { module(repository) }

        val response = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"  ","quantity":2}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `post item rejects non positive quantity`() = testApplication {
        application { module(repository) }

        val response = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Milk","quantity":0}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `put item updates name quantity and purchased`() = testApplication {
        application { module(repository) }
        val created = repository.create("Milk", 2)

        val response = client.put("/items/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Oat milk","quantity":3,"purchased":true}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = json.decodeFromString<Item>(response.bodyAsText())
        assertEquals("Oat milk", updated.name)
        assertEquals(3, updated.quantity)
        assertTrue(updated.purchased)
    }

    @Test
    fun `put missing item returns 404`() = testApplication {
        application { module(repository) }

        val response = client.put("/items/64b7f9a2c2a4e4a1b2c3d4e5") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Milk","quantity":2,"purchased":false}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `delete item removes it`() = testApplication {
        application { module(repository) }
        val created = repository.create("Milk", 2)

        val response = client.delete("/items/${created.id}")

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(emptyList(), repository.findAll())
    }

    @Test
    fun `delete missing item returns 404`() = testApplication {
        application { module(repository) }

        val response = client.delete("/items/64b7f9a2c2a4e4a1b2c3d4e5")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `malformed json returns bad request`() = testApplication {
        application { module(repository) }

        val response = client.post("/items") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}

private class InMemoryItemRepository : ItemRepository {
    private val items = mutableListOf<Item>()
    private var nextId = 1

    fun clear() {
        items.clear()
        nextId = 1
    }

    override suspend fun findAll(): List<Item> = items.toList()

    override suspend fun create(name: String, quantity: Int): Item {
        val item = Item(
            id = "id-${nextId++}",
            name = name,
            quantity = quantity,
            purchased = false,
            createdAt = System.currentTimeMillis()
        )
        items.add(item)
        return item
    }

    override suspend fun update(id: String, name: String, quantity: Int, purchased: Boolean): Item? {
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = items[index].copy(name = name, quantity = quantity, purchased = purchased)
        items[index] = updated
        return updated
    }

    override suspend fun delete(id: String): Boolean = items.removeIf { it.id == id }
}
