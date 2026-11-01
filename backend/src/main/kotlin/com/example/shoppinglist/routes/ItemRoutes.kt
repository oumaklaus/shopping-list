package com.example.shoppinglist.routes

import com.example.shoppinglist.model.ErrorResponse
import com.example.shoppinglist.model.ItemRequest
import com.example.shoppinglist.repository.ItemRepository
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.itemRoutes(repository: ItemRepository) {
    route("/items") {
        get {
            call.respond(repository.findAll(call.userId()))
        }

        post {
            val request = call.receive<ItemRequest>()
            val validationError = request.validationError()
            if (validationError != null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(validationError))
                return@post
            }

            val created = repository.create(
                call.userId(),
                request.name.trim(),
                request.quantity
            )
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing item id"))
                return@put
            }

            val request = call.receive<ItemRequest>()
            val validationError = request.validationError()
            if (validationError != null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(validationError))
                return@put
            }

            val updated = repository.update(
                userId = call.userId(),
                id = id,
                name = request.name.trim(),
                quantity = request.quantity,
                purchased = request.purchased
            )

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Item not found"))
            } else {
                call.respond(updated)
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing item id"))
                return@delete
            }

            if (repository.delete(call.userId(), id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Item not found"))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.userId(): String =
    requireNotNull(principal<JWTPrincipal>()?.payload?.subject)

private fun ItemRequest.validationError(): String? {
    val trimmedName = name.trim()
    return when {
        trimmedName.isEmpty() -> "Name must not be empty"
        trimmedName.length > 80 -> "Name must be 80 characters or fewer"
        quantity !in 1..9999 -> "Quantity must be between 1 and 9999"
        else -> null
    }
}
