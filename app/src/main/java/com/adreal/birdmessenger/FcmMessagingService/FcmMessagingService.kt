package com.adreal.birdmessenger.FcmMessagingService

import android.app.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDeepLinkBuilder
import com.adreal.birdmessenger.BroadcastReceiver.Receiver
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Constants.Constants.Users
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.Retrofit.SendChatObject
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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


@RequiresApi(Build.VERSION_CODES.O)
class FcmMessagingService : FirebaseMessagingService() {

    private val firestore = Firebase.firestore
    var needsAgain = 1

    private val notificationManager by lazy{
        getSystemService(
            NotificationManager::class.java
        )
    }

    private val builder by lazy {
        Notification.Builder(this, CHANNEL_ID)
    }

    companion object {
        const val CHANNEL_ID = "chat_notification"
        const val CHANNEL_NAME = "Chats"
        const val CHANNEL_DESCRIPTION = "this is FCM chat channel"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        SharedPreferences.init(this)

        Log.d("Message Received", "success")

        when (remoteMessage.data["category"]) {
            "newUser" -> {
                val id = remoteMessage.data["id"].toString()
                val time = remoteMessage.data["time"].toString().toLong()
                addNewUserToChat(id, time)
            }

            "chat" -> {
                var senderToken = ""
                var senderName = ""
                val id = remoteMessage.data["id"].toString().toLong()
                val senderId = remoteMessage.data["senderId"].toString()
                val sendTime = remoteMessage.data["sendTime"].toString().toLong()
                val receiverId = remoteMessage.data["receiverId"].toString()
                val msg = remoteMessage.data["msg"].toString()
                val receivedTime = System.currentTimeMillis()

                val chatData = ChatModel(
                    id,
                    senderId,
                    sendTime,
                    receiverId,
                    receivedTime,
                    msg,
                    1,
                    0
                )

                CoroutineScope(Dispatchers.IO).launch {
                    Database.getDatabase(applicationContext).Dao().addChatData(chatData)
                    senderToken = Database.getDatabase(applicationContext).Dao().getToken(senderId)
                    senderName = Database.getDatabase(applicationContext).Dao().getUserName(senderId)
                    prepareChatNotification(senderId, "", senderName, senderToken)

                    val state = SharedPreferences.read(senderId, "n")

                    val jsonObject = JSONObject()
                    val dataJson = JSONObject()
                    jsonObject.put("id", id)
                    dataJson.put("data", jsonObject)
                        .put("to", senderToken)

                    if (state == "y") {
                        updateLastMessage(msg, senderId, receivedTime)
                        jsonObject.put("messageStatus", 3)
                        jsonObject.put("category", "seen")
                        sendData(dataJson.toString(), chatData, 3)
                    } else {
                        incrementUnreadMessages(senderId, msg, receivedTime)
                        jsonObject.put("messageStatus", 2)
                        jsonObject.put("category", "delivered")
                        sendData(dataJson.toString(), chatData, 2)
                    }
                }
            }

            "delivered" -> {
                updateMessageStatus(
                    remoteMessage.data["messageStatus"].toString().toInt(),
                    remoteMessage.data["id"].toString().toLong()
                )
            }

            "seen" -> {
                updateMessageStatus(
                    remoteMessage.data["messageStatus"].toString().toInt(),
                    remoteMessage.data["id"].toString().toLong()
                )
            }

            "img" -> {

            }

            "video" -> {

            }

            "doc" -> {

            }

            "location" -> {

            }
        }

        super.onMessageReceived(remoteMessage)
    }

    private fun updateLastMessage(msg: String, senderId: String, receivedTime : Long) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(applicationContext).Dao().updateLastMessage(msg, receivedTime, senderId)
        }
    }

    private fun incrementUnreadMessages(senderId : String, msg : String, time : Long){
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(applicationContext).Dao().incrementUnreadMessages(senderId, msg, time)
        }
    }

    private fun updateMessageStatus(status: Int, id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(application).Dao().updateMessageStatus(status, id)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun addNewUserToChat(messageId: String, time: Long) {
        firestore.collection(Users).document(messageId).get().addOnSuccessListener {
            val data = UserModel(
                messageId,
                it.get("name").toString(),
                it.get("token").toString(),
                it.get("image").toString(),
                "*you are added by ${it.get("name").toString()}*",
                0,
                time
            )

            addNewUser(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun prepareChatNotification(
        senderId: String,
        subText: String,
        senderName: String,
        senderToken: String
    ) {
        if (SharedPreferences.read("${senderId}_notification_id", -1) == -1) {
            val count = SharedPreferences.read("count", 0)
            SharedPreferences.write("${senderId}_notification_id", count)
            SharedPreferences.write("count", count + 1)
        }

        val data = Database.getDatabase(applicationContext).Dao().readMessagesForNotification(senderId).asReversed()

        val imageString = Database.getDatabase(applicationContext).Dao().readImageStringForUser(senderId)
        val imageBytes = Base64.decode(imageString, 0)
        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val person = Person.Builder()
            .setIcon(Icon.createWithBitmap(getCircleBitmap(imageBitmap)))
            .setName(senderName)
            .build()

        val style = Notification.MessagingStyle(person)

        for (i in data) {
            style.addMessage(i.msg.toString(), i.sendTime!!, person)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification(
                SharedPreferences.read("${senderId}_notification_id", 0),
                style,
                subText,
                senderId,
                senderName,
                senderToken
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun sendData(
        data: String,
        chatModel: ChatModel,
        status: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {

            val json = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = data.toRequestBody(json)
            val chat =
                SendChatObject.sendChatInstance.sendChat("key=${Constants.FCM_API_KEY}", body)

            chat.enqueue(object : Callback<ChatResponse> {
                override fun onResponse(
                    call: Call<ChatResponse>,
                    response: Response<ChatResponse>
                ) {
                    Log.d("message response", response.toString())
                    val details = response.body()
                    if (details != null) {
                        Log.d("details", details.toString())
                        chatModel.messageStatus = status
                        updateMessageStatus(status, chatModel.messageId)
                    }
                }

                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    Log.d("msg sending failed", t.message.toString())
                }
            })
        }
    }

    private fun addNewUser(data: UserModel) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(applicationContext).Dao().addNewUser(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(
        notificationId: Int,
        style: Notification.MessagingStyle?,
        subtext: String,
        senderId: String,
        senderName: String,
        senderToken: String
    ) {
        if(needsAgain == 1){
            val cancelIntent = Intent(this, Receiver::class.java)
            cancelIntent.putExtra("notificationId", notificationId)

            val pendingCancelIntent = PendingIntent.getBroadcast(
                this, notificationId + 2, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val deleteIntent = Intent(this, Receiver::class.java)
            deleteIntent.putExtra("delete", "y")

            val deletePendingIntent = PendingIntent.getActivity(
                this,
                notificationId + 3,
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val args = Bundle()
            args.putString("receiverId", senderId)
            args.putString("receiverName", senderName)
            args.putString("receiverToken", senderToken)
            args.putBoolean("fromNotification", true)

            val pendingIntent = NavDeepLinkBuilder(this)
                .setGraph(R.navigation.main_navigation)
                .setDestination(R.id.chatFragment)
                .setArguments(args)
                .createPendingIntent()

//            val notificationManager = getSystemService(
//                NotificationManager::class.java
//            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = CHANNEL_DESCRIPTION
                notificationManager.createNotificationChannel(channel)
            }

//            val builder = Notification.Builder(this, CHANNEL_ID)
            builder.setSmallIcon(R.drawable.app_icon)
            builder.style = style
            builder.setContentIntent(pendingIntent)
            builder.setDeleteIntent(deletePendingIntent)
            builder.addAction(R.drawable.cancel, "Cancel", pendingCancelIntent)
            builder.setAutoCancel(true)
            builder.setShowWhen(true)
        }

        with(NotificationManagerCompat.from(this)) {
            notificationManager.notify(notificationId, builder.build())
        }
    }

    private fun getCircleBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = Color.RED
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        bitmap.recycle()
        return output
    }
}