package com.example.shoppinglist.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.shoppinglist.data.AuthRepository
import com.example.shoppinglist.data.AuthResponse
import com.example.shoppinglist.data.AuthUser
import com.example.shoppinglist.data.SessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var session: FakeSessionStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        session = FakeSessionStorage()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `successful login stores the session and requests navigation`() = runTest(dispatcher) {
        val viewModel = AuthViewModel(repository, session)

        viewModel.submit(" Shopper@Example.com ", "correct-horse")
        advanceUntilIdle()

        assertEquals("signed-token", session.token)
        assertEquals("shopper@example.com", session.email)
        assertTrue(viewModel.authenticated.value == true)
        assertFalse(requireNotNull(viewModel.uiState.value).isSubmitting)
    }

    @Test
    fun `registration mode uses the registration endpoint`() = runTest(dispatcher) {
        val viewModel = AuthViewModel(repository, session)
        viewModel.toggleMode()

        viewModel.submit("shopper@example.com", "correct-horse")
        advanceUntilIdle()

        assertEquals(1, repository.registerCalls)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `invalid credentials show field errors without calling the server`() = runTest(dispatcher) {
        val viewModel = AuthViewModel(repository, session)

        viewModel.submit("bad-email", "short")
        advanceUntilIdle()

        val state = requireNotNull(viewModel.uiState.value)
        assertTrue(state.emailError != null)
        assertTrue(state.passwordError != null)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun `server failure is shown and does not save a session`() = runTest(dispatcher) {
        repository.failure = IOException("Invalid email or password")
        val viewModel = AuthViewModel(repository, session)

        viewModel.submit("shopper@example.com", "wrong-password")
        advanceUntilIdle()

        assertEquals("Invalid email or password", viewModel.uiState.value?.message)
        assertEquals(null, session.token)
    }

    @Test
    fun `submit ignores reentrant requests while authentication is in progress`() =
        runTest(dispatcher) {
            repository.gate = CompletableDeferred()
            val viewModel = AuthViewModel(repository, session)

            viewModel.submit("shopper@example.com", "correct-horse")
            viewModel.submit("shopper@example.com", "correct-horse")
            advanceUntilIdle()

            assertEquals(1, repository.loginCalls)
            repository.gate?.complete(Unit)
            advanceUntilIdle()
        }
}

private class FakeAuthRepository : AuthRepository {
    var loginCalls = 0
    var registerCalls = 0
    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun login(email: String, password: String): Result<AuthResponse> {
        loginCalls++
        gate?.await()
        return result(email)
    }

    override suspend fun register(email: String, password: String): Result<AuthResponse> {
        registerCalls++
        return result(email)
    }

    private fun result(email: String): Result<AuthResponse> = failure?.let(Result.Companion::failure)
        ?: Result.success(AuthResponse("signed-token", AuthUser("user-1", email)))
}

private class FakeSessionStorage : SessionStorage {
    override var token: String? = null
    override var email: String? = null

    override fun save(auth: AuthResponse) {
        token = auth.token
        email = auth.user.email
    }

    override fun clear() {
        token = null
        email = null
    }
}
