package com.adreal.birdmessenger.Worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Retrofit.SendChatObject
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class UploadWorker(appContext: Context, workerParameters: WorkerParameters) : Worker(appContext,workerParameters) {

    override fun doWork(): Result {

        val allMessages = Database.getDatabase(applicationContext).Dao().readAllMessagesForWorker()

        for(it in allMessages)
        {
            if(it.messageStatus == 0 && it.mediaType == 0)
            {
                val jsonObject = JSONObject()
                val dataJson = JSONObject()
                val priority = JSONObject()
                val message = JSONObject()

                jsonObject.put("id",it.messageId)
                jsonObject.put("senderId",it.senderId)
                jsonObject.put("sendTime",it.sendTime)
                jsonObject.put("receiverId",it.receiverId)
                jsonObject.put("msg",it.msg)
                jsonObject.put("messageStatus",it.messageStatus)
                jsonObject.put("category","chat")

                dataJson.put("data",jsonObject)
                dataJson.put("android",priority)
                dataJson.put("to",Database.getDatabase(applicationContext).Dao().getToken(it.receiverId.toString()))

                priority.put("priority","high")

                send(dataJson.toString(),it)
            }
        }

        return Result.success()
    }

    fun send(dataJson: String, data: ChatModel) {
        val json = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = dataJson.toRequestBody(json)
        val chat = SendChatObject.sendChatInstance.sendChat("key=${Constants.FCM_API_KEY}",body)

        chat.enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                Log.d("response",response.toString())
                val details = response.body()
                if(details != null){
                    Log.d("details",details.toString())
                    if(data.messageStatus == 0){
                        data.messageStatus = 1
                        updateChatData(data)
                    }else{
                        data.messageStatus = 3
                        updateChatData(data)
                    }
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.d("msg sending failed",t.message.toString())
            }
        })
    }

    private fun updateChatData(data : ChatModel) {
        Database.getDatabase(applicationContext).Dao().updateChatData(data)
    }
}