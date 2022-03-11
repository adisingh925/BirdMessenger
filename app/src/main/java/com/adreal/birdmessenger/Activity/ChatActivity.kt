package com.adreal.birdmessenger.Activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adreal.birdmessenger.Adapter.ChatAdapter
import com.adreal.birdmessenger.Connectivity.ConnectionLiveData
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.ViewModel.OfflineViewModel
import com.adreal.birdmessenger.ViewModel.OnlineViewModel
import com.adreal.birdmessenger.databinding.ActivityChatBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatActivity : AppCompatActivity(), ChatAdapter.OnItemSeenListener {

    private lateinit var binding: ActivityChatBinding

    private lateinit var adapter: ChatAdapter

    private lateinit var recyclerView: RecyclerView

    private lateinit var onlineViewModel: OnlineViewModel

    private lateinit var offlineViewModel: OfflineViewModel

    private lateinit var connectionLiveData: ConnectionLiveData

    private lateinit var senderToken : String

    private lateinit var senderName : String

    private lateinit var receiverName : String

    private lateinit var receiverToken : String

    private lateinit var receiverImage : String

    lateinit var receiverId : String

    lateinit var sharedPreferencesForOpen: SharedPreferences

    lateinit var edit : SharedPreferences.Editor

    private var fromNotification = false

    var listSizeCount = 0

    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_BirdMessenger)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onlineViewModel = ViewModelProvider(this).get(OnlineViewModel::class.java)

        offlineViewModel = ViewModelProvider(this).get(OfflineViewModel::class.java)

        connectionLiveData = ConnectionLiveData(this)

        initSharedPreferences()

        initRecycler()

        receiverName = intent.getStringExtra("receiverName").toString()
        receiverToken = intent.getStringExtra("receiverToken").toString()
        receiverId = intent.getStringExtra("receiverId").toString()
        //receiverImage = intent.getStringExtra("receiverImage").toString()
        fromNotification = intent.getBooleanExtra("fromNotification",false)

        offlineViewModel.initUserImage(receiverId)

        offlineViewModel.updateUserCardData(receiverId)

        binding.toolbar.title = receiverName

        binding.edittext.addTextChangedListener {
            if(binding.edittext.text.toString() != "")
            {
                binding.camera.isVisible = false
                binding.attachment.isVisible = false
                binding.fab.setImageResource(R.drawable.send)
            }
            else
            {
                binding.camera.isVisible = true
                binding.attachment.isVisible = true
                binding.fab.setImageResource(R.drawable.mic)
            }
        }

        binding.fab.setOnClickListener()
        {
            if(binding.edittext.text.isNotEmpty())
            {
                val jsonObject = JSONObject()
                val dataJson = JSONObject()
                val priority = JSONObject()
                val message = JSONObject()

                val id = System.currentTimeMillis()
                val msg = binding.edittext.text.toString()
                binding.edittext.setText("")
                val data = ChatModel(
                    id,
                    auth.uid,
                    senderToken,
                    id,
                    senderName,
                    receiverId,
                    receiverToken,
                    id,
                    msg,
                    0,
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

                jsonObject.put("id",id)
                jsonObject.put("senderId",auth.uid)
                jsonObject.put("senderToken",senderToken)
                jsonObject.put("sendTime",id)
                jsonObject.put("senderName",senderName)
                jsonObject.put("receiverId",receiverId)
                jsonObject.put("receiverToken",receiverToken)
                jsonObject.put("receiveTime",id)
                jsonObject.put("msg",msg)
                jsonObject.put("messageStatus","sending")
                jsonObject.put("category","chat")

                dataJson.put("data",jsonObject)
                dataJson.put("android",priority)
                dataJson.put("to",receiverToken)

                priority.put("priority","high")

                message.put("message",dataJson)

                onlineViewModel.sendData(dataJson.toString(),jsonObject)
                offlineViewModel.addChatData(data)
            }
        }

        onlineViewModel.liveData.observe(this)
        {
            Log.d("Live data value",it)
            binding.toolbar.subtitle = it
        }

        offlineViewModel.readAllMessages(auth.uid!!,receiverId).observe(this)
        {
            if(it.size>listSizeCount)
            {
                listSizeCount = it.size
                adapter.setdata(it)
                recyclerView.scrollToPosition(it.size - 1)
            }
            else
            {
                adapter.setdata(it)
            }
        }

        offlineViewModel.imageLiveData.observe(this)
        {
            receiverImage = it
            initToolbarImage()
        }

        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if(bottom<oldBottom)
            {
                if(listSizeCount >= 1)
                recyclerView.smoothScrollToPosition(listSizeCount-1)
            }
        }

        connectionLiveData.observe(this)
        { isNetworkAvailable ->
            if(isNetworkAvailable == false)
            {
                offlineViewModel.setupWorkManager()
            }
        }
    }

    private fun initToolbarImage()
    {
        binding.toolbar.logo = BitmapDrawable(getCircleBitmap(base64ToBitmap(receiverImage)))
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d("new intent is passed","")
        super.onNewIntent(intent)
    }

    private fun base64ToBitmap(data: String): Bitmap {
        val imageBytes = Base64.decode(data, 0)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun initSharedPreferences()
    {
        val sharedPreferences = this.getSharedPreferences("myData",Context.MODE_PRIVATE)
        senderToken = sharedPreferences.getString("token","null").toString()
        senderName = sharedPreferences.getString("name","null").toString()

        sharedPreferencesForOpen = this.getSharedPreferences("isOpen",Context.MODE_PRIVATE)
        edit = sharedPreferencesForOpen.edit()
        Log.d("activity","initialized")
    }

    private fun initRecycler()
    {
        adapter = ChatAdapter(this,this)
        recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onItemSeen(data: ChatModel) {
        data.messageStatus = 3
        val jsonObject = JSONObject()
        val dataJson = JSONObject()
        jsonObject.put("id",data.messageId)
            .put("messageStatus",3)
            .put("category","seen")
        dataJson.put("data",jsonObject)
            .put("to",data.senderToken)
        onlineViewModel.sendData(dataJson.toString(),jsonObject)
    }

    override fun onPause() {
        edit.putString(receiverId,"no")
        edit.apply()
        Log.d("activity","paused")
        onlineViewModel.setStatus("offline")
        super.onPause()
    }

    override fun onBackPressed() {
        if(fromNotification)
        {
            val intent = Intent(this, PeopleActivity::class.java)
            startActivity(intent)
        }
        else
        {
            finish()
        }
        super.onBackPressed()
    }

    override fun onStop() {
        edit.putString(receiverId,"no")
        edit.apply()
        Log.d("activity","stopped")
        onlineViewModel.setStatus("offline")
        super.onStop()
    }

    override fun onResume() {
        edit.putString(receiverId,"yes")
        edit.apply()
        Log.d("activity","resumed")
        onlineViewModel.setStatus("online")
        super.onResume()
    }

    override fun onRestart() {
        edit.putString(receiverId,"yes")
        edit.apply()
        Log.d("activity","restarted")
        onlineViewModel.setStatus("online")
        super.onRestart()
    }

    override fun onStart() {
        edit.putString(receiverId,"yes")
        edit.apply()
        Log.d("activity","started $receiverId")
        if(fromNotification)
        {
            val manager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancelAll()
        }
        onlineViewModel.setStatus("online")
        onlineViewModel.getStatus(receiverId)
        super.onStart()
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