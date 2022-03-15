package com.adreal.birdmessenger.FcmMessagingService

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.adreal.birdmessenger.Activity.ChatActivity
import com.adreal.birdmessenger.BroadcastReceiver.Receiver
import com.adreal.birdmessenger.Constants.Constants
import com.adreal.birdmessenger.Constants.Constants.COMMON_FCM_TOPIC
import com.adreal.birdmessenger.Constants.Constants.Users
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.*


class FcmMessagingService : FirebaseMessagingService() {

    private val firestore = Firebase.firestore

    private val auth = Firebase.auth

    lateinit var imageString : String

    lateinit var chatData : ChatModel

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.d("Message Received","success")

        when(remoteMessage.data["category"])
        {
            "newUser" ->{

                val id = remoteMessage.data["id"].toString().toLong()
                val senderName = remoteMessage.data["senderName"].toString()
                val senderToken = remoteMessage.data["senderToken"].toString()
                val senderId = remoteMessage.data["senderId"].toString()

                addNewUserToChat(id,senderName,senderToken,senderId)
            }

            "returnRequest" ->{

                val id = remoteMessage.data["id"].toString().toLong()
                val senderName = remoteMessage.data["senderName"].toString()
                val senderToken = remoteMessage.data["senderToken"].toString()
                val senderId = remoteMessage.data["senderId"].toString()

                handleReturnRequest(id,senderName,senderToken,senderId)
            }

            "chat" ->{

                val id = remoteMessage.data["id"].toString().toLong()
                val senderId = remoteMessage.data["senderId"].toString()
                val senderToken = remoteMessage.data["senderToken"].toString()
                val sendTime = remoteMessage.data["sendTime"].toString().toLong()
                val senderName = remoteMessage.data["senderName"].toString()
                val receiverId = remoteMessage.data["receiverId"].toString()
                val receiverToken = remoteMessage.data["receiverToken"].toString()
                val msg = remoteMessage.data["msg"].toString()

                val currentTime = System.currentTimeMillis()

                chatData = ChatModel(id,
                    senderId,
                    senderToken,
                    sendTime,
                    senderName,
                    receiverId,
                    receiverToken,
                    currentTime,
                    msg,
                    1,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )

                val sharedPreferences = this.getSharedPreferences("isOpen",Context.MODE_PRIVATE)
                val state = sharedPreferences.getString(senderId,"no")
                Log.d("state","$state $senderId")
                if(state == "yes")
                {
                    val jsonObject = JSONObject()
                    val dataJson = JSONObject()
                    jsonObject.put("id",id)
                        .put("messageStatus",3)
                        .put("category","seen")
                        .put("mediaType",0)
                    dataJson.put("data",jsonObject)
                        .put("to",senderToken)

                    sendData(dataJson.toString(),jsonObject)
                }
                else
                {
                    val jsonObject = JSONObject()
                    val dataJson = JSONObject()
                    jsonObject.put("id",id)
                        .put("messageStatus",2)
                        .put("category","delivered")
                        .put("mediaType",0)
                    dataJson.put("data",jsonObject)
                        .put("to",senderToken)

                    sendData(dataJson.toString(),jsonObject)
                }
            }

            "delivered" ->{
                updateMessageStatus(remoteMessage.data["messageStatus"].toString().toInt(),remoteMessage.data["id"].toString().toLong())
            }

            "seen" ->{
                updateMessageStatus(remoteMessage.data["messageStatus"].toString().toInt(),remoteMessage.data["id"].toString().toLong())
            }

            "img" ->{

            }

            "video" ->{

            }

            "doc" ->{
                val id = remoteMessage.data["id"].toString().toLong()
                val senderId = remoteMessage.data["senderId"].toString()
                val senderToken = remoteMessage.data["senderToken"].toString()
                val sendTime = remoteMessage.data["sendTime"].toString().toLong()
                val senderName = remoteMessage.data["senderName"].toString()
                val receiverId = remoteMessage.data["receiverId"].toString()
                val receiverToken = remoteMessage.data["receiverToken"].toString()
                val mediaType = remoteMessage.data["mediaType"].toString().toInt()
                val mediaExtension = remoteMessage.data["mediaExtension"].toString()
                val mediaSize = remoteMessage.data["mediaSize"].toString().toLong()
                val mediaName = remoteMessage.data["mediaName"].toString()
                val mediaUrl = remoteMessage.data["mediaUrl"].toString()

                val currentTime = System.currentTimeMillis()

                chatData = ChatModel(id,
                    senderId,
                    senderToken,
                    sendTime,
                    senderName,
                    receiverId,
                    receiverToken,
                    currentTime,
                    null,
                    1,
                    mediaType,
                    null,
                    mediaUrl,
                    mediaExtension,
                    mediaSize,
                    mediaName,
                    null,
                    null,
                    null
                )

                val sharedPreferences = this.getSharedPreferences("isOpen",Context.MODE_PRIVATE)
                val state = sharedPreferences.getString(senderId,"no")
                Log.d("state","$state $senderId")
                if(state == "yes")
                {
                    val jsonObject = JSONObject()
                    val dataJson = JSONObject()
                    jsonObject.put("id",id)
                        .put("messageStatus",3)
                        .put("category","seen")
                        .put("mediaType",6)
                    dataJson.put("data",jsonObject)
                        .put("to",senderToken)

                    sendData(dataJson.toString(),jsonObject)
                }
                else
                {
                    val jsonObject = JSONObject()
                    val dataJson = JSONObject()
                    jsonObject.put("id",id)
                        .put("messageStatus",2)
                        .put("category","delivered")
                        .put("mediaType",6)
                    dataJson.put("data",jsonObject)
                        .put("to",senderToken)

                    sendData(dataJson.toString(),jsonObject)
                }
            }

            "location" ->{

            }
        }

        super.onMessageReceived(remoteMessage)
    }

    private fun updateMessageStatus(status : Int, id : Long) {
        Database.getDatabase(application).Dao().updateMessageStatus(status,id)
    }

    private fun handleReturnRequest(messageId : Long,senderName : String, senderToken : String, senderId : String) {
        firestore.collection(Users).document(senderId).get().addOnSuccessListener {
            imageString = it.get("image").toString()
            val data = UserModel(
                messageId,
                senderName,
                senderToken,
                senderId,
                imageString,
                null,
                null,
                null
            )
            addNewUser(data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun addNewUserToChat(messageId : Long,senderName : String, senderToken : String, senderId : String) {
        firestore.collection(Users).document(senderId).get().addOnSuccessListener {
            imageString = it.get("image").toString()
            val data = UserModel(
                messageId,
                senderName,
                senderToken,
                senderId,
                imageString,
                null,
                null,
                null
            )
            addNewUser(data)
            val sharedPreferences = this.getSharedPreferences("myData",Context.MODE_PRIVATE)
            val name = sharedPreferences.getString("name","null")
            val token = sharedPreferences.getString("token","null")
            val id = auth.uid

            val jsonObject = JSONObject()
            val dataJson = JSONObject()
            jsonObject.put("id",messageId)
                .put("senderName",name)
                .put("senderToken",token)
                .put("senderId",id)
                .put("category","returnRequest")
            dataJson.put("data",jsonObject)
                .put("to",senderToken)

            sendData(dataJson.toString(),jsonObject)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun prepareChatNotification(senderId: String, subText : String) {
        val sharedPreferences = getSharedPreferences(senderId, MODE_PRIVATE)
        val chatNotificationId = sharedPreferences.getInt("chat_notification_id", 0)
        if (chatNotificationId == 0) {
            val myEdit = sharedPreferences.edit()
            val randomValue = (Calendar.getInstance().timeInMillis % 1000000000).toInt()
            myEdit.putInt("chat_notification_id", randomValue)
            myEdit.apply()
        }

        var data = Database.getDatabase(applicationContext).Dao().readMessagesForNotification(senderId)

        data = data.asReversed()

        Log.d("notification data",data.toString())

        val imageString = Database.getDatabase(applicationContext).Dao().readImageStringForUser(senderId)
        val imageBytes = Base64.decode(imageString, 0)
        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val person = Person.Builder()
            .setIcon(Icon.createWithBitmap(getCircleBitmap(imageBitmap)))
            .setName(data[0].senderName)
            .build()

        val style = Notification.MessagingStyle(person)

        for(i in data)
        {
            if(i.mediaType == 0)
            {
                style.addMessage(i.msg.toString(),i.sendTime!!,person)
            }
            else if(i.mediaType == 6)
            {
                style.addMessage(i.mediaName.toString(),i.sendTime!!,person)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification("chat_notification","Chats","this is fcm chat channel",sharedPreferences.getInt("chat_notification_id", 0),style,subText)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun sendData(
        data : String,
        json : JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val JSON = MediaType.parse("application/json; charset=utf-8")
            val body = RequestBody.create(JSON,data)
            val request = Request.Builder()
                .url(Constants.FCM_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader(
                    "Authorization",
                    "key=${Constants.FCM_API_KEY}"
                )
                .build()
            try {
                val response = client.newCall(request).execute()
                Log.d("FCM Response", response.toString())
                if (response.isSuccessful) {
                    if(json.get("category") == "delivered" && json.getString("mediaType") == "0")
                    {
                        chatData.messageStatus = 2
                        addChatData(chatData)
                        val currentUser = Database.getDatabase(applicationContext).Dao().readUserForUpdate(chatData.senderId.toString())
                        if(chatData.msg == null)
                        {
                            currentUser.lastMessage = ""
                        }
                        else
                        {
                            currentUser.lastMessage = chatData.msg
                        }
                        currentUser.lastMessageTimeStamp = chatData.sendTime
                        val unreadMessages = Database.getDatabase(applicationContext).Dao().readNumberOfDeliveredMessagesForUser(chatData.senderId.toString())
                        currentUser.unreadMessages = unreadMessages
                        Database.getDatabase(applicationContext).Dao().updateUserData(currentUser)
                        prepareChatNotification(chatData.senderId.toString(),unreadMessages.toString())
                    }
                    else if(json.get("category") == "seen" && json.getString("mediaType") == "0")
                    {
                        chatData.messageStatus = 3
                        addChatData(chatData)
                        val currentUser = Database.getDatabase(applicationContext).Dao().readUserForUpdate(chatData.senderId.toString())
                        if(chatData.msg == null)
                        {
                            currentUser.lastMessage = ""
                        }
                        else
                        {
                            currentUser.lastMessage = chatData.msg
                        }
                        currentUser.lastMessageTimeStamp = chatData.sendTime
                        Database.getDatabase(applicationContext).Dao().updateUserData(currentUser)
                    }
                    else if(json.get("category") == "seen" && json.getString("mediaType") == "6")
                    {
                        chatData.messageStatus = 3
                        addChatData(chatData)
                        val currentUser = Database.getDatabase(applicationContext).Dao().readUserForUpdate(chatData.senderId.toString())
                        if(chatData.mediaName == null)
                        {
                            currentUser.lastMessage = ""
                        }
                        else
                        {
                            currentUser.lastMessage = chatData.mediaName
                        }
                        currentUser.lastMessageTimeStamp = chatData.sendTime
                        Database.getDatabase(applicationContext).Dao().updateUserData(currentUser)
                    }
                    else if(json.get("category") == "delivered" && json.getString("mediaType") == "6")
                    {
                        chatData.messageStatus = 2
                        addChatData(chatData)
                        val currentUser = Database.getDatabase(applicationContext).Dao().readUserForUpdate(chatData.senderId.toString())
                        if(chatData.mediaName == null)
                        {
                            currentUser.lastMessage = ""
                        }
                        else
                        {
                            currentUser.lastMessage = chatData.mediaName
                        }
                        currentUser.lastMessageTimeStamp = chatData.sendTime
                        val unreadMessages = Database.getDatabase(applicationContext).Dao().readNumberOfDeliveredMessagesForUser(chatData.senderId.toString())
                        currentUser.unreadMessages = unreadMessages
                        Database.getDatabase(applicationContext).Dao().updateUserData(currentUser)
                        prepareChatNotification(chatData.senderId.toString(),unreadMessages.toString())
                    }
                }
                else
                {
                    if(json.get("category") == "delivered" && json.getString("mediaType") == "0")
                    {
                        addChatData(chatData)
                    }
                    else if(json.get("category") == "seen" && json.getString("mediaType") == "0")
                    {
                        addChatData(chatData)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    private fun addNewUser(data : UserModel) {
        CoroutineScope(Dispatchers.IO).launch{
            Database.getDatabase(applicationContext).Dao().addNewUser(data)
        }
    }

    private fun addChatData(data : ChatModel) {
        Database.getDatabase(applicationContext).Dao().addChatData(data)
    }

    private fun updateChatData(data : ChatModel) {
        Database.getDatabase(applicationContext).Dao().updateChatData(data)
    }

    override fun onNewToken(token : String) {
        if (auth.uid != null) {
            uploadToken(token)
        }
        else{
            auth.signInAnonymously().addOnCompleteListener {
                if(it.isSuccessful)
                {
                    Log.d("Authentication","success")
                    uploadToken(token)
                    subscribeToTopic()
                    subscribeToCommonTopic()
                    addTokenToSharedPreferences(token)
                }
                else{
                    Log.d("Authentication","Failed")
                }
            }
        }
        super.onNewToken(token)
    }

    private fun addTokenToSharedPreferences(token : String) {
        val sharedPreferences = this.getSharedPreferences("myData",Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putString("token",token)
        edit.apply()
    }

    private fun subscribeToCommonTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(COMMON_FCM_TOPIC).addOnSuccessListener {
            Log.d("FCM subscribe","success")
        }
    }

    private fun subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(auth.uid!!).addOnSuccessListener {
            Log.d("FCM subscribe","success")
        }
    }

    private fun uploadToken(token: String)
    {
        auth.uid?.let { it ->
            firestore.collection(Users).document(it).get().addOnSuccessListener {
                if(it.exists())
                {
                    firestore.collection(Users).document(auth.uid!!).update("token",token).addOnSuccessListener {
                        Log.d("FCM token","updated")
                    }
                }
                else
                {
                    val data = hashMapOf("token" to token)
                    firestore.collection(Users).document(auth.uid!!).set(data).addOnSuccessListener {
                        Log.d("FCM token","uploaded")
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(channelId : String, channelName : String, channelDescription : String, notificationId : Int, style : Notification.MessagingStyle?, subtext : String) {
        val intent = Intent(this, ChatActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        intent.putExtra("receiverId",chatData.senderId)
        intent.putExtra("receiverName",chatData.senderName)
        intent.putExtra("receiverToken",chatData.senderToken)
        intent.putExtra("fromNotification",true)

        val cancelIntent = Intent(this,Receiver::class.java)

        cancelIntent.putExtra("notificationId",notificationId)

        val deleteIntent = Intent(this,Receiver::class.java)

        intent.putExtra("delete","yes")

        val deletePendingIntent = PendingIntent.getActivity(this,notificationId+3,deleteIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pendingIntent = PendingIntent.getActivity(this, notificationId+1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pendingCancelIntent = PendingIntent.getBroadcast(this, notificationId+2, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = channelDescription
            notificationManager.createNotificationChannel(channel)
        }

        val builder = Notification.Builder(this, channelId)
        builder.setSmallIcon(R.drawable.app_icon)
        builder.style = style
        builder.setContentIntent(pendingIntent)
        builder.setDeleteIntent(deletePendingIntent)
        builder.addAction(R.drawable.cancel,"Cancel",pendingCancelIntent)
        builder.setAutoCancel(true)
        builder.setShowWhen(true)

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