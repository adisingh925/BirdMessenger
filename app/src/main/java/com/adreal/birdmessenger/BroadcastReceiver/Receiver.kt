package com.adreal.birdmessenger.BroadcastReceiver

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.adreal.birdmessenger.FcmMessagingService.FcmMessagingService
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences

@RequiresApi(Build.VERSION_CODES.P)
class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if(intent != null && context != null){
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if(intent.action == FcmMessagingService.MUTE){
                val senderId = intent.getStringExtra(FcmMessagingService.ID)
                val notificationId = intent.getIntExtra(FcmMessagingService.NOTIFICATION_ID,-1)
                if(senderId != null && notificationId != -1){
                    SharedPreferences.write("MUTE-$senderId","y")
                    manager.cancel(notificationId)
                }
            }

            if(intent.action == FcmMessagingService.REMOTE_INPUT){
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                if (remoteInput != null) {
                    val response = remoteInput.getCharSequence(FcmMessagingService.KEY_TEXT_REPLY).toString()
                }
            }

            if(intent.action == FcmMessagingService.DELETE){
                val delete = intent.getStringExtra(FcmMessagingService.DELETE)
                if (delete == "y") {
                    manager.cancelAll()
                }
            }

            if(intent.action == FcmMessagingService.READ){
                val senderId = intent.getStringExtra(FcmMessagingService.ID)
                val notificationId = intent.getIntExtra(FcmMessagingService.NOTIFICATION_ID,-1)
                val messages = intent.getLongArrayExtra(FcmMessagingService.MESSAGES)
                val token = intent.getStringExtra(FcmMessagingService.TOKEN)
                if (senderId != null && notificationId != -1 && messages != null && token != null) {
                    for(i in messages){
                        FcmMessagingService().createJson(3,"seen",i,token, context)
                    }
                    manager.cancel(notificationId)
                }
            }
        }
    }
}