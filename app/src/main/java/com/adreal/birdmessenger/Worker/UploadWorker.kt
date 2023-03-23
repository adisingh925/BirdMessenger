package com.adreal.birdmessenger.Worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.FcmMessagingService.FcmMessagingService
import com.adreal.birdmessenger.SendPayload
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UploadWorker(appContext: Context, workerParameters: WorkerParameters) : Worker(appContext,workerParameters) {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun doWork(): Result {
        CoroutineScope(Dispatchers.IO).launch {

            val allMessages = Database.getDatabase(applicationContext).Dao().readAllMessagesForWorker()

            for(it in allMessages) {
                if (it.messageStatus == 0 && it.mediaType == 0) {
                    SendPayload.sendMsg(it,Database.getDatabase(applicationContext).Dao().getToken(it.receiverId),applicationContext,1)
                }else if(it.messageStatus == 4){
                    FcmMessagingService().createJson(
                        3,
                        "seen",
                        it.messageId,
                        Database.getDatabase(applicationContext).Dao().getToken(it.senderId),
                        applicationContext
                    )
                }else if(it.messageStatus == 5){
                    FcmMessagingService().createJson(
                        2,
                        "delivered",
                        it.messageId,
                        Database.getDatabase(applicationContext).Dao().getToken(it.senderId),
                        applicationContext
                    )
                }
            }

            SharedPreferences.write("isWorkEnqueued","f")
        }

        return Result.success()
    }
}