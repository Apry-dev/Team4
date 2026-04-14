package com.example.esnmessenger.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JamixService {
    @GET("apps/menuservice/rest/haku/menu/{customerId}/{kitchenId}")
    suspend fun getMenu(
        @Path("customerId") customerId: Int,
        @Path("kitchenId") kitchenId: Int,
        @Query("lang") lang: String = "en",
        @Query("type") type: String = "json",
        @Query("date") date: String? = null,
        @Query("date2") date2: String? = null
    ): List<JamixKitchen>

    companion object {
        val instance: JamixService by lazy {
            val gson = GsonBuilder().setLenient().create()
            Retrofit.Builder()
                .baseUrl("https://fi.jamix.cloud/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(JamixService::class.java)
        }
    }
}
