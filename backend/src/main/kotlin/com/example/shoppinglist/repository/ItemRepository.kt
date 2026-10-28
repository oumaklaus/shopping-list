package com.example.shoppinglist.repository

import com.example.shoppinglist.model.Item

interface ItemRepository {
    suspend fun findAll(): List<Item>
    suspend fun create(name: String, quantity: Int): Item
    suspend fun update(id: String, name: String, quantity: Int, purchased: Boolean): Item?
    suspend fun delete(id: String): Boolean
}
