package com.example.shoppinglist.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteItemRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteItemRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        repository = RemoteItemRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `get items parses the server response`() = runBlocking {
        server.enqueue(
            jsonResponse(
                code = 200,
                body = """[{"id":"1","name":"Milk","quantity":2,"purchased":false,"createdAt":123}]"""
            )
        )

        val result = repository.getItems()

        assertTrue(result.isSuccess)
        val item = result.getOrThrow().single()
        assertEquals("1", item.id)
        assertEquals("Milk", item.name)
        assertEquals(2, item.quantity)
        assertFalse(item.purchased)
        assertEquals(123L, item.createdAt)
    }

    @Test
    fun `add item sends the expected body and parses the created item`() = runBlocking {
        server.enqueue(
            jsonResponse(
                code = 201,
                body = """{"id":"2","name":"Bread","quantity":3,"purchased":false,"createdAt":456}"""
            )
        )

        val result = repository.addItem("Bread", 3)

        assertTrue(result.isSuccess)
        assertEquals("2", result.getOrThrow().id)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/items", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"name\":\"Bread\""))
        assertTrue(body.contains("\"quantity\":3"))
        assertTrue(body.contains("\"purchased\":false"))
    }

    @Test
    fun `update item sends the full item and parses the response`() = runBlocking {
        server.enqueue(
            jsonResponse(
                code = 200,
                body = """{"id":"2","name":"Oat milk","quantity":4,"purchased":true,"createdAt":456}"""
            )
        )

        val result = repository.updateItem(
            Item(id = "2", name = "Oat milk", quantity = 4, purchased = true, createdAt = 456L)
        )

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().purchased)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/items/2", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"name\":\"Oat milk\""))
        assertTrue(body.contains("\"purchased\":true"))
    }

    @Test
    fun `delete item succeeds on 204`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = repository.deleteItem("2")

        assertTrue(result.isSuccess)
        assertEquals("/items/2", server.takeRequest().path)
    }

    @Test
    fun `delete item fails on an error status`() = runBlocking {
        server.enqueue(jsonResponse(code = 404, body = """{"error":"Item not found"}"""))

        val result = repository.deleteItem("missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `get items fails on a server error`() = runBlocking {
        server.enqueue(jsonResponse(code = 500, body = """{"error":"Something went wrong"}"""))

        val result = repository.getItems()

        assertTrue(result.isFailure)
    }

    private fun jsonResponse(code: Int, body: String): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
}
