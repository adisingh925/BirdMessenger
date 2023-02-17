package com.adreal.birdmessenger

import android.util.Log
import com.adreal.birdmessenger.Constants.Constants
import com.google.firebase.messaging.FirebaseMessaging

class Authentication {

    fun subscribeToCommonTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.COMMON_FCM_TOPIC).addOnSuccessListener {
            Log.d("FCM subscribe","success")
        }
    }
}