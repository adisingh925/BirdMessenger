package com.adreal.birdmessenger.ViewModel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.Retrofit.SendChatObject
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PeopleViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseInstallations = FirebaseInstallations.getInstance()

    fun storeNameAndImage(name: String, image: String) {
        SharedPreferences.write("name", name)
        SharedPreferences.write("image", image)
    }

    fun uploadImage(image: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (SharedPreferences.read("isImageUploaded", "") == "") {
                val data = hashMapOf("image" to image)
                firebaseInstallations.id.addOnSuccessListener {
                    firestore.collection(Constants.Users).document(it).set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("Image Upload", "success")
                            SharedPreferences.write(
                                "isImageUploaded",
                                "y"
                            )
                        }
                }
            }
        }
    }

    fun uploadName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (SharedPreferences.read("isNameUploaded", "") == "") {
                val data = hashMapOf("name" to name)
                firebaseInstallations.id.addOnSuccessListener {
                    firestore.collection(Constants.Users).document(it).set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("Name Upload", "success")
                            SharedPreferences.write(
                                "isNameUploaded",
                                "y"
                            )
                        }
                }
            }
        }
    }

    fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = if (height > width) width else height
        val newHeight = if (height > width) height - (height - width) else height
        var cropW = (width - height) / 2
        cropW = if (cropW < 0) 0 else cropW
        var cropH = (height - width) / 2
        cropH = if (cropH < 0) 0 else cropH
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight)
    }

    fun base64ToBitmap(data: String): Bitmap {
        val imageBytes = Base64.decode(data, 0)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun addUser(id: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            firestore.collection(Constants.Users).document(id).get().addOnSuccessListener {
                val receiverToken = it.get("token").toString()
                val senderId = SharedPreferences.read("installationId", "")
                val receiverName = it.get("name").toString()
                val receiverImage = it.get("image").toString()
                val time = System.currentTimeMillis()

                val jsonObject = JSONObject()
                val dataJson = JSONObject()
                jsonObject.put("id", senderId)
                    .put("time",time)
                    .put("category", "newUser")
                dataJson.put("data", jsonObject)
                    .put("to", receiverToken)

                sendData(dataJson.toString(), jsonObject)

                viewModelScope.launch(Dispatchers.IO) {
                    Database.getDatabase(context).Dao().addNewUser(
                        UserModel(
                            id,
                            receiverName,
                            receiverToken,
                            receiverImage,
                            "*you added $receiverName*",
                            0,
                            time
                        )
                    )
                }
            }
        }
    }

    private fun sendData(
        data: String,
        json: JSONObject
    ) {
        val applicationJson = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = data.toRequestBody(applicationJson)
        val chat = SendChatObject.sendChatInstance.sendChat("key=${Constants.FCM_API_KEY}", body)

        chat.enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                Log.d("response", response.toString())
                val details = response.body()
                if (details != null) {
                    Log.d("details", details.toString())
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {

            }
        })
    }
}