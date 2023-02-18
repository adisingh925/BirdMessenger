package com.adreal.birdmessenger.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Database.Database
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

class ChatViewModel(application : Application) : AndroidViewModel(application) {

    private val realtimeDatabase = Firebase.database
    val liveData = MutableLiveData<String>()

    fun getStatus(receiverId : String) {
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

    fun storeMsg(data : ChatModel){
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().addChatData(data)
            Database.getDatabase(getApplication()).Dao().updateLastMessage(data.msg.toString(), data.sendTime!!, data.receiverId.toString())
        }
    }

    fun updateMsg(data : ChatModel){
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().updateChatData(data)
        }
    }

    fun sendMsg(data : ChatModel, receiverToken : String){
        viewModelScope.launch(Dispatchers.IO) {
            val jsonObject = JSONObject()
            val dataJson = JSONObject()
            val priority = JSONObject()

            jsonObject.put("id",data.messageId)
            jsonObject.put("senderId",data.senderId)
            jsonObject.put("sendTime",data.sendTime)
            jsonObject.put("receiverId",data.receiverId)
            jsonObject.put("msg",data.msg)
            jsonObject.put("messageStatus",0)
            jsonObject.put("mediaType",0)
            jsonObject.put("category","chat")

            priority.put("priority","medium")

            dataJson.put("data",jsonObject)
            dataJson.put("android",priority)
            dataJson.put("to",receiverToken)

            send(dataJson.toString(), data)
        }
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
                        updateMsg(data)
                    }else{
                        data.messageStatus = 3
                        updateMsg(data)
                    }
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.d("msg sending failed",t.message.toString())
            }
        })
    }

    fun updateUnseenMessageCount(id : String) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().resetUnseenMessageCount(id)
        }
    }
}