package com.example.shoppinglist

import com.example.shoppinglist.model.Item
import com.example.shoppinglist.model.User
import com.example.shoppinglist.repository.ItemRepository
import com.example.shoppinglist.repository.UserRepository

class InMemoryItemRepository : ItemRepository {
    private val items = mutableListOf<OwnedItem>()
    private var nextId = 1

    override suspend fun findAll(userId: String): List<Item> =
        items.filter { it.userId == userId }.map { it.item }

    override suspend fun create(userId: String, name: String, quantity: Int): Item {
        val item = Item(
            id = "id-${nextId++}",
            name = name,
            quantity = quantity,
            purchased = false,
            createdAt = System.currentTimeMillis()
        )
        items += OwnedItem(userId, item)
        return item
    }

    override suspend fun update(
        userId: String,
        id: String,
        name: String,
        quantity: Int,
        purchased: Boolean
    ): Item? {
        val index = items.indexOfFirst { it.userId == userId && it.item.id == id }
        if (index == -1) return null
        val updated = items[index].item.copy(
            name = name,
            quantity = quantity,
            purchased = purchased
        )
        items[index] = OwnedItem(userId, updated)
        return updated
    }

    override suspend fun delete(userId: String, id: String): Boolean =
        items.removeIf { it.userId == userId && it.item.id == id }

    private data class OwnedItem(val userId: String, val item: Item)
}

class InMemoryUserRepository : UserRepository {
    private val users = mutableListOf<User>()
    private var nextId = 1

    override suspend fun findByEmail(email: String): User? =
        users.firstOrNull { it.email == email }

    override suspend fun create(email: String, passwordHash: String): User? {
        if (findByEmail(email) != null) return null
        return User(
            id = "user-${nextId++}",
            email = email,
            passwordHash = passwordHash,
            createdAt = System.currentTimeMillis()
        ).also(users::add)
    }
}
