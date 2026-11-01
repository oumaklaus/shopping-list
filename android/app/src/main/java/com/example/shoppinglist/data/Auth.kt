package com.example.shoppinglist.data

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthUser(
    val id: String,
    val email: String
)

data class AuthResponse(
    val token: String,
    val user: AuthUser
)

private data class ApiError(val error: String?)

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthResponse>
    suspend fun register(email: String, password: String): Result<AuthResponse>
}

class RemoteAuthRepository(
    private val api: ApiService = ApiClient.apiService
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AuthResponse> =
        request { api.login(AuthRequest(email, password)) }

    override suspend fun register(email: String, password: String): Result<AuthResponse> =
        request { api.register(AuthRequest(email, password)) }

    private suspend fun request(block: suspend () -> AuthResponse): Result<AuthResponse> =
        try {
            Result.success(block())
        } catch (error: HttpException) {
            val message = runCatching {
                Gson().fromJson(error.response()?.errorBody()?.string(), ApiError::class.java).error
            }.getOrNull().orEmpty().ifBlank { "Authentication failed" }
            Result.failure(IOException(message, error))
        } catch (error: Throwable) {
            Result.failure(error)
        }
}
