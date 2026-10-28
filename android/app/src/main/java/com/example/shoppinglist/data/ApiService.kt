package com.example.shoppinglist.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("items")
    suspend fun getItems(): List<Item>

    @POST("items")
    suspend fun addItem(@Body request: ItemRequest): Item

    @PUT("items/{id}")
    suspend fun updateItem(@Path("id") id: String, @Body request: ItemRequest): Item

    @DELETE("items/{id}")
    suspend fun deleteItem(@Path("id") id: String): Response<Unit>
}
