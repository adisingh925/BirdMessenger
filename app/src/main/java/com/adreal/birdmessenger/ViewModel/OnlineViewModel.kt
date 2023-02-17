package com.adreal.birdmessenger.ViewModel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants.FCM_API_KEY
import com.adreal.birdmessenger.Constants.Constants.Users
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Retrofit.SendChatObject
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OnlineViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val realtimeDatabase = Firebase.database
    private val storage = Firebase.storage
    var downloadUrl = MutableLiveData<String?>()
    val liveData = MutableLiveData<String>()
    val isMsgSent = MutableLiveData<ChatResponse>()

    private val sharedPreferences: SharedPreferences = application.getSharedPreferences("myData", Context.MODE_PRIVATE)

    fun addUser(key : String)
    {
        viewModelScope.launch(Dispatchers.IO) {
            firestore.collection(Users).document(key).get().addOnSuccessListener {
                val receiverToken = it.get("token").toString()
                val senderToken = getSenderToken()
                val time = System.currentTimeMillis()
                val senderId = auth.uid
                val senderName = sharedPreferences.getString("name","null")

                val jsonObject = JSONObject()
                val dataJson = JSONObject()
                jsonObject.put("id",time)
                    .put("senderName",senderName)
                    .put("senderToken",senderToken)
                    .put("senderId",senderId)
                    .put("category","newUser")
                dataJson.put("data",jsonObject)
                    .put("to",receiverToken)

                sendData(dataJson.toString(),jsonObject)
            }
        }
    }

    fun uploadImage(image : String)
    {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf("image" to image)
            firestore.collection(Users).document(auth.uid!!).set(data, SetOptions.merge()).addOnSuccessListener {
                Log.d("Image Upload","success")
            }
        }
    }

    fun setStatus(status : String)
    {
        viewModelScope.launch(Dispatchers.IO) {
            realtimeDatabase.reference.child(auth.uid.toString()).child("status").setValue(status)
        }
    }

    fun getStatus(receiverId : String)
    {
        viewModelScope.launch(Dispatchers.IO) {
            realtimeDatabase.reference.child(receiverId).addChildEventListener(object : ChildEventListener{
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("result","child added")
                    liveData.value = snapshot.value.toString()
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("result","child changed")
                    liveData.value = snapshot.value.toString()
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    Log.d("result","child removed")
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("result","child moved")
                    liveData.value = snapshot.value.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("result","child cancelled")
                }

            })
        }
    }

    private fun getSenderToken(): String? {
        return sharedPreferences.getString("token","null")
    }

    private fun updateChatData(data : ChatModel) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().updateChatData(data)
        }
    }

    private fun updateMessageStatus(status : Int, id : Long) {
        viewModelScope.launch(Dispatchers.IO) {
            Database.getDatabase(getApplication()).Dao().updateMessageStatus(status,id)
        }
    }

    fun uploadToFirebase(uri: Uri?, id : Long)
    {
        viewModelScope.launch(Dispatchers.IO) {

            val uploadTask = uri?.let { storage.reference.child("${auth.uid}/$id").putFile(it) }

            uploadTask?.addOnFailureListener {
                Log.d("File Uploading","failed")
            }?.addOnSuccessListener {
                storage.reference.child("${auth.uid}/$id").downloadUrl.addOnSuccessListener{
                    Log.d("Download Url","retrieved $it")
                    downloadUrl.postValue(it.toString())
                }.addOnFailureListener{
                    Log.d("Download Url","retrieval failed")
                }
                Log.d("File Uploading","success")
            }
        }
    }

    fun sendData(
        data : String,
        json : JSONObject
    ) {
        val json = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = data.toRequestBody(json)
        val chat = SendChatObject.sendChatInstance.sendChat("key=$FCM_API_KEY",body)

        chat.enqueue(object : Callback<ChatResponse>{
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                Log.d("response",response.toString())
                val details = response.body()
                if(details != null){
                    Log.d("details",details.toString())
                    isMsgSent.postValue(details)
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {

            }
        })
    }
}