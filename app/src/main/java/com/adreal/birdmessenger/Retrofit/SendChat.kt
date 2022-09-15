package com.adreal.birdmessenger.Retrofit

import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.RetrofitHelper.RetrofitHelper
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SendChat {
    @POST("fcm/send")
    fun sendChat(@Header("Authorization") apiKey : String,
                 @Body requestBody : RequestBody
    ) : Call<ChatResponse>
}

object SendChatObject{
    val sendChatInstance : SendChat = RetrofitHelper.getInstance().create(SendChat::class.java)
}