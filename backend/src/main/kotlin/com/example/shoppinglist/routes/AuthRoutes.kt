package com.example.shoppinglist.routes

import com.example.shoppinglist.auth.AuthResult
import com.example.shoppinglist.auth.AuthService
import com.example.shoppinglist.model.AuthRequest
import com.example.shoppinglist.model.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            when (val result = authService.registerRequest(call.receive())) {
                is AuthResult.Success -> call.respond(HttpStatusCode.Created, result.response)
                is AuthResult.Invalid -> call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(result.message)
                )
                AuthResult.DuplicateEmail -> call.respond(
                    HttpStatusCode.Conflict,
                    ErrorResponse("Email is already registered")
                )
                AuthResult.InvalidCredentials -> error("Unexpected registration result")
            }
        }

        post("/login") {
            when (val result = authService.loginRequest(call.receive())) {
                is AuthResult.Success -> call.respond(result.response)
                AuthResult.InvalidCredentials -> call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid email or password")
                )
                is AuthResult.Invalid -> call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(result.message)
                )
                AuthResult.DuplicateEmail -> error("Unexpected login result")
            }
        }
    }
}

private suspend fun AuthService.registerRequest(request: AuthRequest) =
    register(request.email, request.password)

private suspend fun AuthService.loginRequest(request: AuthRequest) =
    login(request.email, request.password)
