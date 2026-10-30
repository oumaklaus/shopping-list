package com.example.shoppinglist.repository

import com.example.shoppinglist.model.Item
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId

class MongoItemRepository(
    private val collection: MongoCollection<Document>
) : ItemRepository {

    override suspend fun findAll(): List<Item> = withContext(Dispatchers.IO) {
        collection.find()
            .map { it.toItem() }
            .toList()
            .sortedBy { it.createdAt }
    }

    override suspend fun create(name: String, quantity: Int): Item = withContext(Dispatchers.IO) {
        val document = Document("_id", ObjectId())
            .append("name", name)
            .append("quantity", quantity)
            .append("purchased", false)
            .append("createdAt", System.currentTimeMillis())

        collection.insertOne(document)
        document.toItem()
    }

    override suspend fun update(
        id: String,
        name: String,
        quantity: Int,
        purchased: Boolean
    ): Item? = withContext(Dispatchers.IO) {
        val objectId = id.toObjectId() ?: return@withContext null

        val result = collection.updateOne(
            Filters.eq("_id", objectId),
            Updates.combine(
                Updates.set("name", name),
                Updates.set("quantity", quantity),
                Updates.set("purchased", purchased)
            )
        )

        if (result.matchedCount == 0L) return@withContext null

        collection.find(Filters.eq("_id", objectId)).first()?.toItem()
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        val objectId = id.toObjectId() ?: return@withContext false
        collection.deleteOne(Filters.eq("_id", objectId)).deletedCount > 0
    }
}

private fun String.toObjectId(): ObjectId? =
    if (ObjectId.isValid(this)) ObjectId(this) else null

private fun Document.toItem(): Item = Item(
    id = getObjectId("_id").toHexString(),
    name = getString("name") ?: "",
    quantity = getInteger("quantity") ?: 0,
    purchased = getBoolean("purchased") ?: false,
    createdAt = getLong("createdAt") ?: 0L
)
