package com.adreal.birdmessenger.BroadcastReceiver

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.FcmMessagingService.FcmMessagingService
import com.adreal.birdmessenger.Fragments.ChatFragment
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Retrofit.SendChatObject
import com.adreal.birdmessenger.SendPayload
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
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

@RequiresApi(Build.VERSION_CODES.P)
class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent != null && context != null) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (intent.action == FcmMessagingService.MUTE) {
                val senderId = intent.getStringExtra(FcmMessagingService.ID)
                val notificationId = intent.getIntExtra(FcmMessagingService.NOTIFICATION_ID, -1)
                if (senderId != null && notificationId != -1) {
                    SharedPreferences.write("MUTE-$senderId", "y")
                    manager.cancel(notificationId)
                }
            }

            if (intent.action == FcmMessagingService.REMOTE_INPUT) {
                CoroutineScope(Dispatchers.IO).launch {
                    val remoteInput = RemoteInput.getResultsFromIntent(intent)
                    val senderId = intent.getStringExtra(FcmMessagingService.ID)
                    val token = intent.getStringExtra(FcmMessagingService.TOKEN)
                    val senderName = intent.getStringExtra(FcmMessagingService.SENDER_NAME)
                    if (remoteInput != null && senderId != null && token != null && senderName != null) {
                        val response = remoteInput.getCharSequence(FcmMessagingService.KEY_TEXT_REPLY).toString()
                        val time = System.currentTimeMillis()

                        val data = ChatModel(
                            time,
                            SharedPreferences.read("installationId", "").toString(),
                            time,
                            senderId,
                            null,
                            response,
                            0,
                            0
                        )

                        SendPayload.sendMsg(data,token,context,1)

                        CoroutineScope(Dispatchers.IO).launch {
                            SendPayload.storeMsg(data,context)
                        }.invokeOnCompletion {
                            FcmMessagingService().prepareChatNotification(senderId,senderName,token,context, manager)
                        }
                    }
                }
            }

            if (intent.action == FcmMessagingService.DELETE) {
                val delete = intent.getStringExtra(FcmMessagingService.DELETE)
                if (delete == "y") {
                    manager.cancelAll()
                }
            }

            if (intent.action == FcmMessagingService.READ) {
                val senderId = intent.getStringExtra(FcmMessagingService.ID)
                val notificationId = intent.getIntExtra(FcmMessagingService.NOTIFICATION_ID, -1)
                val messages = intent.getLongArrayExtra(FcmMessagingService.MESSAGES)
                val token = intent.getStringExtra(FcmMessagingService.TOKEN)
                if (senderId != null && notificationId != -1 && messages != null && token != null) {
                    for (i in messages) {
                        if(i != "0".toLong()){
                            FcmMessagingService().createJson(3, "seen", i, token, context)
                        }
                    }
                    manager.cancel(notificationId)
                }
            }
        }
    }
}