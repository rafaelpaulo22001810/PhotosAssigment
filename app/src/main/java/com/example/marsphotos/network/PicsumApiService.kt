package com.example.marsphotos.network

import com.example.marsphotos.model.PicsumPhoto
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET

private const val BASE_URL = "https://picsum.photos"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
    .baseUrl(BASE_URL)
    .build()

interface PicsumApiService {
    @GET("v2/list")
    suspend fun getPhotos(): List<PicsumPhoto>
}

object PicsumApi {
    val retrofitService: PicsumApiService by lazy {
        retrofit.create(PicsumApiService::class.java)
    }
}