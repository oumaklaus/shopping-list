package com.example.shoppinglist.data

import java.io.IOException

interface ItemRepository {
    suspend fun getItems(): Result<List<Item>>
    suspend fun addItem(name: String, quantity: Int): Result<Item>
    suspend fun updateItem(item: Item): Result<Item>
    suspend fun deleteItem(id: String): Result<Unit>
}

class RemoteItemRepository(
    private val api: ApiService = ApiClient.apiService
) : ItemRepository {

    override suspend fun getItems(): Result<List<Item>> = runCatching {
        api.getItems()
    }

    override suspend fun addItem(name: String, quantity: Int): Result<Item> = runCatching {
        api.addItem(ItemRequest(name = name, quantity = quantity, purchased = false))
    }

    override suspend fun updateItem(item: Item): Result<Item> = runCatching {
        api.updateItem(
            id = item.id,
            request = ItemRequest(
                name = item.name,
                quantity = item.quantity,
                purchased = item.purchased
            )
        )
    }

    override suspend fun deleteItem(id: String): Result<Unit> = runCatching {
        val response = api.deleteItem(id)
        if (!response.isSuccessful) {
            throw IOException("Delete failed with HTTP ${response.code()}")
        }
    }
}
