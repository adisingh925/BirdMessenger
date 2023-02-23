package com.adreal.birdmessenger.Worker

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.SendPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UploadWorker(appContext: Context, workerParameters: WorkerParameters) : Worker(appContext,workerParameters) {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun doWork(): Result {
        CoroutineScope(Dispatchers.IO).launch {
            val message = Database.getDatabase(applicationContext).Dao().getMessage(inputData.getLong("id",0))
            SendPayload.sendMsg(message,Database.getDatabase(applicationContext).Dao().getToken(message.receiverId),applicationContext,1)
        }

        return Result.success()
    }
}