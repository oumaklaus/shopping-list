package com.example.shoppinglist.data

data class Item(
    val id: String,
    val name: String,
    val quantity: Int,
    val purchased: Boolean,
    val createdAt: Long
)

data class ItemRequest(
    val name: String,
    val quantity: Int,
    val purchased: Boolean
)
