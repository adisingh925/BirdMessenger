package com.adreal.birdmessenger.BroadcastReceiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class Receiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val manager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = intent?.getIntExtra("notificationId", -1)
        val delete = intent?.getStringExtra("delete")

        if (notificationId != -1) {
            if (notificationId != null) {
                manager.cancel(notificationId)
            }
        }

        if (delete == "y") {
            manager.cancelAll()
        }
    }
}