package com.example.shoppinglist.data

import com.example.shoppinglist.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = AuthSession.token
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }.build()
            val response = chain.proceed(request)
            if (response.code == 401 && token != null) {
                AuthSession.expire(token)
            }
            response
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}
