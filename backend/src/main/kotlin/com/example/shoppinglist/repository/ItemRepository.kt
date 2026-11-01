package com.example.shoppinglist.repository

import com.example.shoppinglist.model.Item

interface ItemRepository {
    suspend fun findAll(userId: String): List<Item>
    suspend fun create(userId: String, name: String, quantity: Int): Item
    suspend fun update(
        userId: String,
        id: String,
        name: String,
        quantity: Int,
        purchased: Boolean
    ): Item?
    suspend fun delete(userId: String, id: String): Boolean
}
