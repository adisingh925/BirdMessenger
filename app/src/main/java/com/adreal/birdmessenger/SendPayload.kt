package com.adreal.birdmessenger

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Retrofit.SendChatObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

object SendPayload {

    @RequiresApi(Build.VERSION_CODES.P)
    fun sendMsg(data: ChatModel, receiverToken: String, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {

            val jsonObject = JSONObject()
            val dataJson = JSONObject()
            val priority = JSONObject()
            val encryptedJson = JSONObject()

            encryptedJson.put("id", data.messageId)
            encryptedJson.put("senderId", data.senderId)
            encryptedJson.put("receiverId", data.receiverId)
            encryptedJson.put("msg", data.msg)
            encryptedJson.put("messageStatus", 0)
            encryptedJson.put("mediaType", 0)

            val encryptedData = Encryption().encryptUsingSymmetricKey(encryptedJson.toString(), data.receiverId)
            val hash = Encryption().generateHMAC(Base64.getEncoder().encodeToString(encryptedData.cipherText),data.receiverId)

            jsonObject.put("ED", Base64.getEncoder().encodeToString(encryptedData.cipherText))
            jsonObject.put("IV", Base64.getEncoder().encodeToString(encryptedData.iv))
            jsonObject.put("SI",data.senderId)
            jsonObject.put("HASH", hash)
            jsonObject.put("category","chat")

            priority.put("priority", "medium")

            dataJson.put("data", jsonObject)
            dataJson.put("android", priority)
            dataJson.put("to", receiverToken)

            send(dataJson.toString(), data, context)
        }
    }

    fun send(dataJson: String, data: ChatModel, context: Context) {
        val json = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = dataJson.toRequestBody(json)
        val chat = SendChatObject.sendChatInstance.sendChat("key=${Constants.FCM_API_KEY}", body)

        chat.enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                Log.d("response", response.toString())
                val details = response.body()
                if (details != null) {
                    if (data.messageStatus == 0) {
                        data.messageStatus = 1
                        updateMsg(data,context)
                    } else {
                        data.messageStatus = 3
                        updateMsg(data,context)
                    }
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.d("msg sending failed", t.message.toString())
            }
        })
    }

    fun storeMsg(data: ChatModel, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(context).Dao().addChatData(data)
            Database.getDatabase(context).Dao().updateLastMessage(data.msg, data.sendTime, data.receiverId)
        }
    }

    fun updateMsg(data: ChatModel, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(context).Dao().updateChatData(data)
        }
    }
}