package com.example.shoppinglist

import com.example.shoppinglist.model.ErrorResponse
import com.example.shoppinglist.model.HealthResponse
import com.example.shoppinglist.repository.ItemRepository
import com.example.shoppinglist.repository.MongoItemRepository
import com.example.shoppinglist.routes.itemRoutes
import com.mongodb.client.MongoClients
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull()
        ?: System.getenv("SERVER_PORT")?.toIntOrNull()
        ?: 8080
    val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017"
    val databaseName = System.getenv("MONGODB_DB") ?: "shopping_list"

    val mongoClient = MongoClients.create(mongoUri)
    val repository = MongoItemRepository(
        mongoClient.getDatabase(databaseName).getCollection("items")
    )

    Runtime.getRuntime().addShutdownHook(Thread { mongoClient.close() })

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module(repository)
    }.start(wait = true)
}

fun Application.module(repository: ItemRepository) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<ContentTransformationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Something went wrong")
            )
        }
    }

    routing {
        get("/health") {
            call.respond(HealthResponse("ok"))
        }
        itemRoutes(repository)
    }
}
