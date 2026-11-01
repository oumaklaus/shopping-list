package com.example.shoppinglist.repository

import com.example.shoppinglist.model.User
import com.mongodb.MongoWriteException
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId

class MongoUserRepository(
    private val collection: MongoCollection<Document>
) : UserRepository {

    init {
        collection.createIndex(Indexes.ascending("email"), IndexOptions().unique(true))
    }

    override suspend fun findByEmail(email: String): User? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("email", email)).first()?.toUser()
    }

    override suspend fun create(email: String, passwordHash: String): User? =
        withContext(Dispatchers.IO) {
            val document = Document("_id", ObjectId())
                .append("email", email)
                .append("passwordHash", passwordHash)
                .append("createdAt", System.currentTimeMillis())

            try {
                collection.insertOne(document)
                document.toUser()
            } catch (error: MongoWriteException) {
                if (error.error.code == DUPLICATE_KEY_CODE) null else throw error
            }
        }

    private companion object {
        const val DUPLICATE_KEY_CODE = 11000
    }
}

private fun Document.toUser(): User = User(
    id = getObjectId("_id").toHexString(),
    email = getString("email"),
    passwordHash = getString("passwordHash"),
    createdAt = getLong("createdAt")
)
