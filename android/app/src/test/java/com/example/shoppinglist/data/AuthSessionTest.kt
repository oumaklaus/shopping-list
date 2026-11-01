package com.example.shoppinglist.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthSessionTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @After
    fun tearDown() {
        AuthSession.configure(null) {}
        AuthSession.expirationHandled()
    }

    @Test
    fun `current token unauthorized response clears persistence and remains observable`() {
        var persistedClearCalled = false
        AuthSession.configure("current-token") { persistedClearCalled = true }

        AuthSession.expire("current-token")
        AuthSession.configure(null) { persistedClearCalled = true }

        assertNull(AuthSession.token)
        assertTrue(persistedClearCalled)
        assertEquals(true, AuthSession.expired.value)
    }

    @Test
    fun `stale unauthorized response cannot clear a replacement session`() {
        var persistedClearCalled = false
        AuthSession.configure("replacement-token") { persistedClearCalled = true }

        AuthSession.expire("old-token")

        assertEquals("replacement-token", AuthSession.token)
        assertFalse(persistedClearCalled)
        assertEquals(false, AuthSession.expired.value)
    }
}
