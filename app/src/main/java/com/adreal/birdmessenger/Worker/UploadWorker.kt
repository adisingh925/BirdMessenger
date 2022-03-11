package com.adreal.birdmessenger.Worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.json.JSONObject
import java.io.IOException

class UploadWorker(appContext: Context, workerParameters: WorkerParameters) : Worker(appContext,workerParameters) {

    override fun doWork(): Result {

        val allMessages = Database.getDatabase(applicationContext).Dao().readAllMessagesForWorker()

        for(it in allMessages)
        {
            if(it.messageStatus == 0 && it.mediaType == 0
            )
            {
                val jsonObject = JSONObject()
                val dataJson = JSONObject()
                val priority = JSONObject()
                val message = JSONObject()

                jsonObject.put("id",it.messageId)
                jsonObject.put("senderId",it.senderId)
                jsonObject.put("senderToken",it.senderToken)
                jsonObject.put("sendTime",it.sendTime)
                jsonObject.put("senderName",it.senderName)
                jsonObject.put("receiverId",it.receiverId)
                jsonObject.put("receiverToken",it.receiverToken)
                jsonObject.put("receiveTime",it.receiveTime)
                jsonObject.put("msg",it.msg)
                jsonObject.put("messageStatus",it.messageStatus)
                jsonObject.put("category","chat")

                dataJson.put("data",jsonObject)
                dataJson.put("android",priority)
                dataJson.put("to",it.receiverToken)

                priority.put("priority","high")

                message.put("message",dataJson)

                sendData(dataJson.toString(),jsonObject)
            }
        }

        return Result.success()
    }

    private fun sendData(
        data : String,
        json : JSONObject)
    {
        val client = OkHttpClient()
        val JSON = MediaType.parse("application/json; charset=utf-8")
        val body = RequestBody.create(JSON,data)
        val request = Request.Builder()
            .url(Constants.FCM_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader(
                "Authorization",
                "key=${Constants.FCM_API_KEY}"
            )
            .build()
        try {
            val response = client.newCall(request).execute()
            Log.d("FCM Response", response.toString())
            if (response.isSuccessful) {
                if(json.getString("category").toString() == "sending")
                {
                    val data = ChatModel(
                        json.getString("id").toString().toLong(),
                        json.getString("senderId"),
                        json.getString("senderToken"),
                        json.getString("sendTime").toString().toLong(),
                        json.getString("senderName"),
                        json.getString("receiverId"),
                        json.getString("receiverToken"),
                        json.getString("receiverTime").toString().toLong(),
                        json.getString("msg"),
                        1,
                        0,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )

                    updateChatData(data)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun updateChatData(data : ChatModel) {
        Database.getDatabase(applicationContext).Dao().updateChatData(data)
    }
}