package com.adreal.birdmessenger.ViewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class StartActivityViewModel : ViewModel() {

    private val realtimeDatabase = Firebase.database
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    private val firebaseInstallations = FirebaseInstallations.getInstance()

    fun setStatus(status: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (SharedPreferences.read("installationId", "") != "") {
                realtimeDatabase.reference.child(
                    SharedPreferences.read("installationId", "").toString()
                ).child("status")
                    .setValue(status)
            } else {
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
                    firestore.collection(Constants.Users).document(id).set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("FCM token", "uploaded")
                            SharedPreferences.write("isTokenUploaded", "y")
                        }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun storeAESPublicKey() {
        if (SharedPreferences.read("AESPublic", "") == "") {
            val asymmetricKey = Encryption().getAsymmetricKeyPair()
            SharedPreferences.write(
                "AESPublic",
                Base64.getEncoder().encodeToString(asymmetricKey.public.encoded)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun storeDHKeyPair() {
        if (SharedPreferences.read("DHKeyPair", "") == "") {
            val DHKey = Encryption().generateDHKeyPair()
            SharedPreferences.write("DHPublic", Base64.getEncoder().encodeToString(DHKey.public.encoded))
            SharedPreferences.write("DHPrivate",Base64.getEncoder().encodeToString(DHKey.private.encoded))
            SharedPreferences.write("DHKeyPair","y")
            Log.d("DH Key Pair storing","success")
            uploadDHPublicKey(Base64.getEncoder().encodeToString(DHKey.public.encoded))
        }
    }

    private fun uploadDHPublicKey(DHPublic : String) {
        if(SharedPreferences.read("isKeyUploaded","") == ""){
            firebaseInstallations.id.addOnSuccessListener {
                val data = hashMapOf("DHPublic" to DHPublic)
                firestore.collection(Constants.Users).document(it).set(data, SetOptions.merge())
                SharedPreferences.write("isKeyUploaded","y")
                Log.d("DH Public Key","uploaded")
            }
        }
    }

    private fun uploadECDHPublicKey(DHPublic : String) {
        if(SharedPreferences.read("isKeyUploaded","") == ""){
            firebaseInstallations.id.addOnSuccessListener {
                val data = hashMapOf("ECDHPublic" to DHPublic)
                firestore.collection(Constants.Users).document(it).set(data, SetOptions.merge())
                SharedPreferences.write("isKeyUploaded","y")
                Log.d("ECDH Public Key","uploaded")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun storeECDHKeyPair() {
        if (SharedPreferences.read("ECDHKeyPair", "") == "") {
            val ECDHKey = Encryption().generateECDHKeyPair()
            SharedPreferences.write("ECDHPublic", Base64.getEncoder().encodeToString(ECDHKey.public.encoded))
            SharedPreferences.write("ECDHPrivate",Base64.getEncoder().encodeToString(ECDHKey.private.encoded))
            SharedPreferences.write("ECDHKeyPair","y")
            Log.d("ECDH Key Pair storing","success")
            uploadECDHPublicKey(Base64.getEncoder().encodeToString(ECDHKey.public.encoded))
        }
    }
}