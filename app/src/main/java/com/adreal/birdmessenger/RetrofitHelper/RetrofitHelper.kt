package com.adreal.birdmessenger.RetrofitHelper

import com.adreal.birdmessenger.Constants.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitHelper {
    private var client: OkHttpClient = OkHttpClient().newBuilder()
        .connectTimeout(Constants.CONNECTION_TIMEOUT.toLong(), TimeUnit.SECONDS)
        .readTimeout(Constants.READ_TIMEOUT.toLong(), TimeUnit.SECONDS)
        .writeTimeout(Constants.WRITE_TIMEOUT.toLong(), TimeUnit.SECONDS)
        .build()

    fun getInstance() : Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.FCM_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}