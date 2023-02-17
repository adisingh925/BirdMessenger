package com.adreal.birdmessenger.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.adreal.birdmessenger.Adapter.ChatAdapter
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.adreal.birdmessenger.ViewModel.ChatViewModel
import com.adreal.birdmessenger.ViewModel.OfflineViewModel
import com.adreal.birdmessenger.databinding.FragmentChatBinding
import com.vanniktech.emoji.EmojiPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.properties.Delegates


class ChatFragment : Fragment(), ChatAdapter.OnItemSeenListener {

    private val binding by lazy {
        FragmentChatBinding.inflate(layoutInflater)
    }

    private val offlineViewModel by lazy {
        ViewModelProvider(this)[OfflineViewModel::class.java]
    }

    private val adapter by lazy {
        context?.let { ChatAdapter(it, this) }
    }

    private val recyclerView by lazy {
        binding.recyclerView
    }

    private val chatViewModel by lazy {
        ViewModelProvider(this)[ChatViewModel::class.java]
    }

    lateinit var receiverId: String
    lateinit var receiverToken: String
    lateinit var receiverName: String
    private var listSizeCount = 0
    lateinit var senderId: String
    var fromNotification  = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        initValues()
        initRecycler()
        chatViewModel.getStatus(receiverId)
        chatViewModel.updateUnseenMessageCount(receiverId)

        binding.toolbar.inflateMenu(R.menu.delete)

        val popup = EmojiPopup.Builder.fromRootView(binding.root).build(binding.edittext)

        binding.emoji.setOnClickListener()
        {
            if (popup.isShowing) {
                binding.emoji.setImageResource(R.drawable.emoji_notfilled)
            } else {
                binding.emoji.setImageResource(R.drawable.keyboard)
            }
            popup.toggle()
        }

        binding.edittext.addTextChangedListener {
            if (binding.edittext.text.toString() != "") {
                binding.camera.isVisible = false
                binding.attachment.isVisible = false
                binding.fab.setImageResource(R.drawable.send)
            } else {
                binding.camera.isVisible = true
                binding.attachment.isVisible = true
                binding.fab.setImageResource(R.drawable.mic)
            }
        }

        offlineViewModel.readAllMessages(senderId, receiverId).observe(viewLifecycleOwner)
        {
            if (it.size > listSizeCount) {
                listSizeCount = it.size
                adapter?.setdata(it,senderId)
                recyclerView.scrollToPosition(it.size - 1)
            } else {
                adapter?.setdata(it,senderId)
            }
        }

        recyclerView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (bottom < oldBottom) {
                if (listSizeCount >= 1)
                    recyclerView.smoothScrollToPosition(listSizeCount - 1)
            }
        }

        chatViewModel.liveData.observe(viewLifecycleOwner)
        {
            if (it == "1") {
                binding.toolbar.subtitle = "Online"
            } else {
                binding.toolbar.subtitle = ""
            }
        }

        binding.fab.setOnClickListener {
            if (!binding.edittext.text.isNullOrBlank()) {
                val time = System.currentTimeMillis()
                val msg = binding.edittext.text.toString().trim()
                binding.edittext.setText("")

                val chatData = ChatModel(
                    time,
                    SharedPreferences.read("installationId", ""),
                    time,
                    receiverId,
                    null,
                    msg,
                    0,
                    0
                )

                chatViewModel.sendMsg(
                    chatData, receiverToken
                )

                chatViewModel.storeMsg(chatData)
            }
        }

        return binding.root
    }

    private fun initRecycler() {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun initValues() {
        receiverId = arguments?.getString("receiverId", "").toString()
        receiverName = arguments?.getString("receiverName", "").toString()
        receiverToken = arguments?.getString("receiverToken", "").toString()
        senderId = SharedPreferences.read("installationId", "").toString()
        fromNotification = arguments?.getBoolean("fromNotification",false) == true
        binding.toolbar.title = receiverName
    }

    override fun onItemSeen(data: ChatModel) {
        if(data.senderId == receiverId){
            CoroutineScope(Dispatchers.IO).launch {
                val jsonObject = JSONObject()
                val dataJson = JSONObject()
                jsonObject.put("id",data.messageId)
                    .put("messageStatus",3)
                    .put("category","seen")
                dataJson.put("data",jsonObject)
                    .put("to",receiverToken)
                chatViewModel.send(dataJson.toString(),data)
            }
        }
    }

    override fun onStart() {
        SharedPreferences.write(receiverId,"y")
        super.onStart()
    }

    override fun onStop() {
        SharedPreferences.write(receiverId,"n")
        super.onStop()
    }
}