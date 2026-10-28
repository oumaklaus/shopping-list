package com.example.shoppinglist.model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String,
    val name: String,
    val quantity: Int,
    val purchased: Boolean,
    val createdAt: Long
)

@Serializable
data class ItemRequest(
    val name: String,
    val quantity: Int,
    val purchased: Boolean = false
)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class HealthResponse(val status: String)
