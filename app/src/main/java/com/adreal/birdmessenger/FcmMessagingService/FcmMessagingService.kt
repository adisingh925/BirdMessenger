package com.adreal.birdmessenger.FcmMessagingService

import android.app.*
import android.content.Context
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
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.FCMResponse.ChatResponse
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.Model.encryptedModel
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.Retrofit.SendChatObject
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
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
    var data : MutableList<ChatModel> = ArrayList()

    companion object {
        const val CHANNEL_ID = "chat_notification"
        const val CHANNEL_NAME = "Chats"
        const val CHANNEL_DESCRIPTION = "this is FCM chat channel"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val NOTIFICATION_ID = "notificationId"
        const val DELETE = "delete"
        const val ID = "senderId"
        const val MUTE = "mute"
        const val REMOTE_INPUT = "remoteInput"
        const val READ = "read"
        const val REPLY = "Reply"
        const val MESSAGES = "messages"
        const val TOKEN = "token"
        const val SENDER_NAME = "name"
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
                if (remoteMessage.data.isNotEmpty()) {

                    val encryptedData = remoteMessage.data["ED"]
                    val hash = remoteMessage.data["HASH"]
                    val senderId = remoteMessage.data["SI"]
                    val initializationVector = remoteMessage.data["IV"]

                    if (!encryptedData.isNullOrBlank() && !hash.isNullOrBlank() && !senderId.isNullOrBlank() && !initializationVector.isNullOrBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (Encryption().compareMessageAndHMAC(encryptedData, hash, senderId)) {

                                val receivedTime = System.currentTimeMillis()

                                val decryptedData = Encryption().decryptUsingSymmetricEncryption(
                                    java.util.Base64.getDecoder().decode(encryptedData),
                                    java.util.Base64.getDecoder().decode(initializationVector),
                                    senderId
                                )

                                val data = Gson().fromJson(decryptedData, encryptedModel::class.java)

                                val chatData = ChatModel(
                                    data.id.toLong(),
                                    senderId,
                                    data.id.toLong(),
                                    data.receiverId,
                                    receivedTime,
                                    data.msg,
                                    1,
                                    0
                                )

                                val senderData = Database.getDatabase(applicationContext).Dao().getTokenAndUserName(senderId)

                                Database.getDatabase(applicationContext).Dao().addChatData(chatData)

                                if(SharedPreferences.read("MUTE-$senderId","n") == "y"){
                                    val notificationManager = getSystemService(NotificationManager::class.java)
                                    prepareChatNotification(
                                        senderId,
                                        senderData.userName,
                                        senderData.userToken,
                                        applicationContext,
                                        notificationManager
                                    )
                                }

                                val state = SharedPreferences.read(senderId, "n")

                                if (state == "y") {
                                    updateLastMessage(chatData.msg, senderId, receivedTime)
                                    createJson(3,"seen",chatData.messageId, senderData.userToken, applicationContext)
                                } else {
                                    incrementUnreadMessages(senderId, chatData.msg, receivedTime)
                                    createJson(2,"delivered",chatData.messageId,senderData.userToken, applicationContext)
                                }
                            }
                        }
                    }
                }
            }

            "delivered", "seen" -> {
                updateMessageStatus(
                    remoteMessage.data["messageStatus"].toString().toInt(),
                    remoteMessage.data["id"].toString().toLong(),
                    applicationContext
                )
            }
        }

        super.onMessageReceived(remoteMessage)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun createJson(status : Int, category : String, messageId : Long, token : String, context: Context){
        CoroutineScope(Dispatchers.IO).launch {
            val jsonObject = JSONObject()
            val dataJson = JSONObject()
            jsonObject.put("id", messageId)
            dataJson.put("data", jsonObject).put("to", token)
            jsonObject.put("messageStatus", status)
            jsonObject.put("category", category)
            sendData(dataJson.toString(), messageId, status, context)
        }
    }

    private fun updateLastMessage(msg: String, senderId: String, receivedTime: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(applicationContext).Dao()
                .updateLastMessage(msg, receivedTime, senderId)
        }
    }

    private fun incrementUnreadMessages(senderId: String, msg: String, time: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(applicationContext).Dao()
                .incrementUnreadMessages(senderId, msg, time)
        }
    }

    private fun updateMessageStatus(status: Int, id: Long, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Database.getDatabase(context).Dao().updateMessageStatus(status, id)
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

            Encryption().generateECDHSecret(it.get("ECDHPublic").toString(), messageId)

            addNewUser(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun prepareChatNotification(
        senderId: String,
        senderName: String,
        senderToken: String,
        context: Context,
        notificationManager: NotificationManager
    ) {
        if (SharedPreferences.read("${senderId}_notification_id", -1) == -1) {
            val count = SharedPreferences.read("count", 0)
            SharedPreferences.write("${senderId}_notification_id", count)
            SharedPreferences.write("count", count + 1)
        }

        val data = Database.getDatabase(context).Dao().readMessagesForNotification(senderId, SharedPreferences.read("installationId","").toString())

        Log.d("data",data.toString())

        val style = Notification.MessagingStyle("hello")

        for (i in data.asReversed()) {
            if(i.senderId == senderId){
                val person = Person.Builder()
                    .setIcon(Icon.createWithBitmap(getCircleBitmap(Database.getDatabase(context).Dao().readImageStringForUser(senderId))))
                    .setName(senderName)
                    .build()

                style.addMessage(i.msg, i.sendTime, person)
            }else{
                val person = Person.Builder()
                    .setIcon(Icon.createWithBitmap(getCircleBitmap(SharedPreferences.read("image","").toString())))
                    .setName("You")
                    .build()

                style.addMessage(i.msg, i.sendTime, person)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification(
                SharedPreferences.read("${senderId}_notification_id", 0),
                style,
                senderId,
                senderName,
                senderToken,
                data,
                context,
                notificationManager
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun sendData(
        data: String,
        messageId : Long,
        status: Int,
        context: Context
    ) {
        CoroutineScope(Dispatchers.IO).launch {

            val json = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = data.toRequestBody(json)
            val chat = SendChatObject.sendChatInstance.sendChat("key=${Constants.FCM_API_KEY}", body)

            chat.enqueue(object : Callback<ChatResponse> {
                override fun onResponse(
                    call: Call<ChatResponse>,
                    response: Response<ChatResponse>
                ) {
                    Log.d("message response", response.toString())
                    val details = response.body()
                    if (details != null) {
                        Log.d("details", details.toString())
                        updateMessageStatus(status, messageId, context)
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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun createNotification(
        notificationId: Int,
        style: Notification.MessagingStyle?,
        senderId: String,
        senderName: String,
        senderToken: String,
        data : List<ChatModel>,
        context: Context,
        notificationManager: NotificationManager
    ) {
        val subArray = LongArray(4)
        for (i in data.indices) {
            if(data[i].senderId == senderId){
                subArray[i] = data[i].messageId
            }else{
                subArray[i] = 0
            }
        }

        //read intent
        val readIntent = Intent(context, Receiver::class.java)
        readIntent.action = READ
        readIntent.putExtra(ID, senderId)
        readIntent.putExtra(NOTIFICATION_ID,notificationId)
        readIntent.putExtra(MESSAGES,subArray)
        readIntent.putExtra(TOKEN,senderToken)
        val pendingReadIntent = PendingIntent.getBroadcast(
            context, notificationId * 2, readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //delete intent
        val deleteIntent = Intent(context, Receiver::class.java)
        deleteIntent.action = DELETE
        deleteIntent.putExtra(DELETE, "y")
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 3,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //remote input
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).build()
        val resultIntent = Intent(context, Receiver::class.java)
        resultIntent.action = REMOTE_INPUT
        resultIntent.putExtra(ID,senderId)
        resultIntent.putExtra(TOKEN,senderToken)
        resultIntent.putExtra(SENDER_NAME,senderName)

        val resultPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 4,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = Notification.Action.Builder(
            android.R.drawable.ic_input_add,
            REPLY, resultPendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        //mute action
        val muteIntent = Intent(context,Receiver::class.java)
        muteIntent.action = MUTE
        muteIntent.putExtra(ID,senderId)
        muteIntent.putExtra(NOTIFICATION_ID,notificationId)
        val mutePendingIntent = PendingIntent.getBroadcast(context, notificationId * 5, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        //notification click handling
        val args = Bundle()
        args.putString("receiverId", senderId)
        args.putString("receiverName", senderName)
        args.putString("receiverToken", senderToken)
        args.putBoolean("fromNotification", true)

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.chatFragment)
            .setArguments(args)
            .createPendingIntent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = CHANNEL_DESCRIPTION
            notificationManager.createNotificationChannel(channel)
        }

        val builder = Notification.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.drawable.chat)
        builder.style = style
        builder.setContentIntent(pendingIntent)
        builder.setDeleteIntent(deletePendingIntent)
        builder.addAction(R.drawable.cancel, "Mark as Read", pendingReadIntent)
        builder.addAction(replyAction)
        builder.addAction(R.drawable.cancel,"Mute",mutePendingIntent)
        builder.setAutoCancel(true)
        builder.setShowWhen(true)

        with(NotificationManagerCompat.from(context)) {
            notificationManager.notify(notificationId, builder.build())
        }
    }

    private fun getCircleBitmap(imageString : String): Bitmap? {
        val imageBytes = Base64.decode(imageString, 0)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

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