package com.example.shoppinglist.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteAuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteAuthRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        repository = RemoteAuthRepository(api)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `register posts credentials and parses the authenticated user`() = runBlocking {
        server.enqueue(authResponse(201))

        val result = repository.register("shopper@example.com", "correct-horse")

        assertTrue(result.isSuccess)
        assertEquals("signed-token", result.getOrThrow().token)
        assertEquals("shopper@example.com", result.getOrThrow().user.email)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/register", request.path)
        assertTrue(request.body.readUtf8().contains("\"password\":\"correct-horse\""))
    }

    @Test
    fun `login posts credentials to the login endpoint`() = runBlocking {
        server.enqueue(authResponse(200))

        val result = repository.login("shopper@example.com", "correct-horse")

        assertTrue(result.isSuccess)
        assertEquals("/auth/login", server.takeRequest().path)
    }

    @Test
    fun `server authentication error exposes its message`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"Invalid email or password"}""")
        )

        val result = repository.login("shopper@example.com", "wrong-password")

        assertTrue(result.isFailure)
        assertEquals("Invalid email or password", result.exceptionOrNull()?.message)
    }

    private fun authResponse(code: Int) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(
            """{"token":"signed-token","user":{"id":"user-1","email":"shopper@example.com"}}"""
        )
}
