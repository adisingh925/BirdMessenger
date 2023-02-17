package com.adreal.birdmessenger.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StartActivityViewModel : ViewModel() {

    private val realtimeDatabase = Firebase.database
    private val auth = Firebase.auth
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    private val firebaseInstallations = FirebaseInstallations.getInstance()

    fun setStatus(status: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if(SharedPreferences.read("installationId","") != ""){
                realtimeDatabase.reference.child(SharedPreferences.read("installationId","").toString()).child("status")
                    .setValue(status)
            }else{
                firebaseInstallations.id.addOnSuccessListener {
                    realtimeDatabase.reference.child(it).child("status")
                        .setValue(status)
                }
            }
        }
    }

    fun saveToken() {
        if (SharedPreferences.read("token", "") == "") {
            messaging.token.addOnSuccessListener {
                SharedPreferences.write("token", it)
                Log.d("token fetch", "success")
            }
        }
    }

    fun saveInstallationId() {
        if (SharedPreferences.read("installationId", "") == "") {
            firebaseInstallations.id.addOnSuccessListener {
                SharedPreferences.write("installationId", it)
                Log.d("Firebase installation id fetch", "success")
            }
        }
    }

    fun subscribeToCommonTopic() {
        if (SharedPreferences.read("commonTopicSubscribe", "") == "") {
            messaging.subscribeToTopic(Constants.COMMON_FCM_TOPIC).addOnSuccessListener {
                Log.d("FCM common topic subscribe", "success")
                SharedPreferences.write("commonTopicSubscribe", "y")
            }
        }
    }

    fun subscribeToIndividualTopic() {
        if (SharedPreferences.read("individualTopicSubscribe", "") == "") {
            firebaseInstallations.id.addOnSuccessListener {
                messaging.subscribeToTopic(it).addOnSuccessListener {
                    Log.d("FCM individual topic subscribe", "success")
                    SharedPreferences.write("individualTopicSubscribe", "y")
                }
            }
        }
    }

    fun uploadToken() {
        if (SharedPreferences.read("isTokenUploaded", "") == "") {
            messaging.token.addOnSuccessListener {
                val data = hashMapOf("token" to it)
                firebaseInstallations.id.addOnSuccessListener { id ->
                    firestore.collection(Constants.Users).document(id).set(data)
                        .addOnSuccessListener {
                            Log.d("FCM token", "uploaded")
                            SharedPreferences.write("isTokenUploaded", "y")
                        }
                }
            }
        }
    }
}