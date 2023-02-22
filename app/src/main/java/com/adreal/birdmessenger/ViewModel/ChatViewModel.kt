package com.adreal.birdmessenger.ViewModel

import android.app.Application
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Retrofit.SendChatObject
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Base64

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val realtimeDatabase = Firebase.database
    val liveData = MutableLiveData<String>()

    fun getStatus(receiverId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            realtimeDatabase.reference.child(receiverId).addChildEventListener(object :
                ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("result", "child added")
                    liveData.value = snapshot.value.toString()
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("result", "child changed")
                    liveData.value = snapshot.value.toString()
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    Log.d("result", "child removed")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("result", "child moved")
                    liveData.value = snapshot.value.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("result", "child cancelled")
                }
            })
        }
    }

    fun storeMsg(data: ChatModel) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().addChatData(data)
            Database.getDatabase(getApplication()).Dao().updateLastMessage(data.msg, data.sendTime, data.receiverId)
        }
    }

    fun updateMsg(data: ChatModel) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().updateChatData(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun sendMsg(data: ChatModel, receiverToken: String) {
        viewModelScope.launch(Dispatchers.IO) {

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

            jsonObject.put("ED",Base64.getEncoder().encodeToString(encryptedData.cipherText))
            jsonObject.put("IV", Base64.getEncoder().encodeToString(encryptedData.iv))
            jsonObject.put("SI",data.senderId)
            jsonObject.put("HASH", hash)
            jsonObject.put("category","chat")

            priority.put("priority", "medium")

            dataJson.put("data", jsonObject)
            dataJson.put("android", priority)
            dataJson.put("to", receiverToken)

            send(dataJson.toString(), data)
        }
    }

    fun send(dataJson: String, data: ChatModel) {
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
                        updateMsg(data)
                    } else {
                        data.messageStatus = 3
                        updateMsg(data)
                    }
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.d("msg sending failed", t.message.toString())
            }
        })
    }

    fun updateUnseenMessageCount(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().resetUnseenMessageCount(id)
        }
    }

    private val typingHandler = Handler(Looper.getMainLooper())

    private var isTyping = false

    // local mock status to show typing status on ui
    private val _mockStatus = MutableLiveData(false)
    val mockLiveStatus: LiveData<Boolean> = _mockStatus

    /*
    called on onTextChange */
    fun startTyping(msg: String) {
        if (msg.isEmpty()) {
            if (isTyping) {
                removeTypingCallbacks()
                _mockStatus.value = false
            }
            isTyping = false
        } else if (!isTyping) {
            _mockStatus.value = true
            isTyping = true
            typingHandler.postDelayed(typingThread, 3000)
        }
    }

    private val typingThread = Runnable {
        _mockStatus.value = false
        isTyping = false
    }

    private fun removeTypingCallbacks() {
        typingHandler.removeCallbacks(typingThread)
    }
}
