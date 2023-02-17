package com.adreal.birdmessenger.Adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.R
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*


class ChatAdapter(
    private val context: Context,
    private val onItemSeenListener: OnItemSeenListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messageList = emptyList<ChatModel>()
    lateinit var installationId: String

    interface OnItemSeenListener {
        fun onItemSeen(data: ChatModel)
    }

    private val VIEW_TYPE_ONE = 1

    private val VIEW_TYPE_TWO = 2

    private inner class ViewHolder1(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var senderTextView: TextView = itemView.findViewById(R.id.senderMessage)
        val senderSeen: ImageView = itemView.findViewById(R.id.senderStatus)
        val senderTime: TextView = itemView.findViewById(R.id.senderTime)

        fun bind(position: Int) {
            val time = getDate(messageList[position].sendTime.toString().toLong(), "hh:mm aa")
            senderTextView.text = messageList[position].msg
            senderTime.text = time
        }
    }

    private inner class ViewHolder2(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val receiverTextView: TextView = itemView.findViewById(R.id.receiverMessage)
        val receiverTime: TextView = itemView.findViewById(R.id.receiverTime)

        fun bind(position: Int) {
            val time = getDate(messageList[position].receiveTime.toString().toLong(), "hh:mm aa")
            receiverTextView.text = messageList[position].msg
            receiverTime.text = time
        }
    }

    fun setdata(data: List<ChatModel>, id: String) {
        this.messageList = data
        notifyDataSetChanged()

        if (!this::installationId.isInitialized) {
            installationId = id
        }
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        val formatter = SimpleDateFormat(dateFormat)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {
            1 -> return ViewHolder1(
                LayoutInflater.from(context).inflate(R.layout.sender_layout, parent, false)
            )
            2 -> return ViewHolder2(
                LayoutInflater.from(context).inflate(R.layout.receiver_layout, parent, false)
            )
        }

        return ViewHolder2(
            LayoutInflater.from(context).inflate(R.layout.receiver_layout, parent, false)
        )
    }

    override fun getItemViewType(position: Int): Int {
        if (messageList[position].senderId == installationId) {
            when (messageList[position].mediaType) {
                0 -> return VIEW_TYPE_ONE
                else -> VIEW_TYPE_TWO
            }
        } else {
            when (messageList[position].mediaType) {
                0 -> return VIEW_TYPE_TWO
                else -> VIEW_TYPE_TWO
            }
        }
        return VIEW_TYPE_ONE
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (messageList[position].senderId == installationId) {
            when (messageList[position].mediaType) {
                0 -> {
                    (holder as ViewHolder1).bind(position)
                    when (messageList[position].messageStatus) {
                        3 -> {
                            Glide.with(context).load(R.drawable.seen).circleCrop()
                                .into(holder.senderSeen)
                        }
                        0 -> {
                            Glide.with(context).load(R.drawable.sending).circleCrop()
                                .into(holder.senderSeen)
                        }
                        1 -> {
                            Glide.with(context).load(R.drawable.sent).circleCrop()
                                .into(holder.senderSeen)
                        }
                        2 -> {
                            Glide.with(context).load(R.drawable.delivered).circleCrop()
                                .into(holder.senderSeen)
                        }
                    }
                }
            }
        } else if (messageList[position].receiverId == installationId) {
            when (messageList[position].mediaType) {
                0 -> {
                    (holder as ViewHolder2).bind(position)
                    if (messageList[position].receiverId == installationId && messageList[position].messageStatus == 2) {
                        onItemSeenListener.onItemSeen(messageList[position])
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    private fun openLink(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

    fun humanReadableByteCountSI(bytes: Long): String? {
        var bytes = bytes
        if (-1000 < bytes && bytes < 1000) {
            return "$bytes B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        while (bytes <= -999950 || bytes >= 999950) {
            bytes /= 1000
            ci.next()
        }
        return java.lang.String.format("%.1f %cB", bytes / 1000.0, ci.current())
    }
}