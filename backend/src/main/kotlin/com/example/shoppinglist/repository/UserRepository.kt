package com.example.shoppinglist.repository

import com.example.shoppinglist.model.User

interface UserRepository {
    suspend fun findByEmail(email: String): User?
    suspend fun create(email: String, passwordHash: String): User?
}
