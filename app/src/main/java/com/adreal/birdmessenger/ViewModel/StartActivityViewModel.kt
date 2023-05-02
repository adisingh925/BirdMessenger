package com.adreal.birdmessenger.ViewModel

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@RequiresApi(Build.VERSION_CODES.P)
class StartActivityViewModel : ViewModel() {

    private val realtimeDatabase by lazy {
        Firebase.database
    }

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val messaging by lazy {
        FirebaseMessaging.getInstance()
    }

    private val firebaseInstallations by lazy {
        FirebaseInstallations.getInstance()
    }

    fun setStatus(status: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (SharedPreferences.read(
                    Constants.INSTALLATION_ID,
                    Constants.BLANK
                ) != Constants.BLANK
            ) {
                realtimeDatabase.reference.child(
                    SharedPreferences.read(Constants.INSTALLATION_ID, Constants.BLANK).toString()
                ).child(Constants.STATUS)
                    .setValue(status)
            } else {
                firebaseInstallations.id.addOnSuccessListener {
                    realtimeDatabase.reference.child(it).child(Constants.STATUS)
                        .setValue(status)
                }
            }
        }
    }

    fun saveToken() {
        if (SharedPreferences.read(Constants.TOKEN, Constants.BLANK) == Constants.BLANK) {
            messaging.token.addOnSuccessListener {
                SharedPreferences.write(Constants.TOKEN, it)
            }
        }
    }

    fun saveInstallationId() {
        if (SharedPreferences.read(Constants.INSTALLATION_ID, Constants.BLANK) == Constants.BLANK) {
            firebaseInstallations.id.addOnSuccessListener {
                SharedPreferences.write(Constants.INSTALLATION_ID, it)
            }
        }
    }

    fun subscribeToCommonTopic() {
        if (SharedPreferences.read(
                Constants.COMMON_TOPIC_SUBSCRIBE,
                Constants.BLANK
            ) == Constants.BLANK
        ) {
            messaging.subscribeToTopic(Constants.COMMON_FCM_TOPIC).addOnSuccessListener {
                SharedPreferences.write(Constants.COMMON_TOPIC_SUBSCRIBE, Constants.YES)
            }
        }
    }

    fun subscribeToIndividualTopic() {
        if (SharedPreferences.read(
                Constants.INDIVIDUAL_TOPIC_SUBSCRIBE,
                Constants.BLANK
            ) == Constants.BLANK
        ) {
            firebaseInstallations.id.addOnSuccessListener {
                messaging.subscribeToTopic(it).addOnSuccessListener {
                    SharedPreferences.write(Constants.INDIVIDUAL_TOPIC_SUBSCRIBE, Constants.YES)
                }
            }
        }
    }

    fun uploadToken() {
        if (SharedPreferences.read(
                Constants.IS_TOKEN_UPLOADED,
                Constants.BLANK
            ) == Constants.BLANK
        ) {
            messaging.token.addOnSuccessListener {
                val data = hashMapOf(Constants.TOKEN to it)
                firebaseInstallations.id.addOnSuccessListener { id ->
                    firestore.collection(Constants.Users).document(id).set(data, SetOptions.merge()).addOnSuccessListener {
                            SharedPreferences.write(Constants.IS_TOKEN_UPLOADED, Constants.YES)
                    }
                }
            }
        }
    }

    fun storeDHKeyPair() {
        if (SharedPreferences.read(Constants.DH_KEY_PAIR, Constants.BLANK) == Constants.BLANK) {
            val DHKey = Encryption().generateDHKeyPair()

            SharedPreferences.write(
                Constants.DH_PUBLIC,
                encodedString(DHKey.public.encoded)
            )

            SharedPreferences.write(
                Constants.DH_PRIVATE,
                encodedString(DHKey.private.encoded)
            )

            SharedPreferences.write(Constants.DH_KEY_PAIR, Constants.YES)
            uploadDHPublicKey(encodedString(DHKey.public.encoded))
        }
    }

    private fun uploadDHPublicKey(DHPublic: String) {
        if (SharedPreferences.read(
                Constants.IS_DH_KEY_UPLOADED,
                Constants.BLANK
            ) == Constants.BLANK
        ) {
            firebaseInstallations.id.addOnSuccessListener {
                val data = hashMapOf(Constants.DH_PUBLIC to DHPublic)
                firestore.collection(Constants.Users).document(it).set(data, SetOptions.merge())
                SharedPreferences.write(Constants.IS_DH_KEY_UPLOADED, Constants.YES)
            }
        }
    }

    private fun uploadECDHPublicKey(ECDHPublic: String) {
        if (SharedPreferences.read(
                Constants.IS_ECDH_KEY_UPLOADED,
                Constants.BLANK
            ) == Constants.BLANK
        ) {
            firebaseInstallations.id.addOnSuccessListener {
                val data = hashMapOf(Constants.ECDH_PUBLIC to ECDHPublic)
                firestore.collection(Constants.Users).document(it).set(data, SetOptions.merge())
                SharedPreferences.write(Constants.IS_ECDH_KEY_UPLOADED, Constants.YES)
            }
        }
    }

    fun storeECDHKeyPair() {
        if (SharedPreferences.read(Constants.ECDH_KEY_PAIR, Constants.BLANK) == Constants.BLANK) {
            val ECDHKey = Encryption().generateECDHKeyPair()

            SharedPreferences.write(
                Constants.ECDH_PUBLIC,
                encodedString(ECDHKey.public.encoded)
            )

            SharedPreferences.write(
                Constants.ECDH_PRIVATE,
                encodedString(ECDHKey.private.encoded)
            )

            SharedPreferences.write(Constants.ECDH_KEY_PAIR, Constants.YES)
            uploadECDHPublicKey(encodedString(ECDHKey.public.encoded))
        }
    }

    fun dismissNotifications(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }

    private fun encodedString(data : ByteArray) : String{
        return Base64.getEncoder().encodeToString(data)
    }
}