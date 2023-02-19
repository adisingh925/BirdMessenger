package com.adreal.birdmessenger.Adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.adreal.birdmessenger.Encryption.Encryption
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.noties.markwon.Markwon
import io.noties.markwon.movement.MovementMethodPlugin
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

    private var messageList: MutableList<ChatModel> = ArrayList()
    lateinit var installationId: String

    private val markWon by lazy {
        context.let { Markwon.builder(context).usePlugin(MovementMethodPlugin.none()).build() }
    }

    interface OnItemSeenListener {
        fun onItemSeen(data: ChatModel)
    }

    private val VIEW_TYPE_ONE = 1

    private val VIEW_TYPE_TWO = 2

    private inner class ViewHolder1(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var senderTextView: TextView = itemView.findViewById(R.id.senderMessage)
        val senderSeen: ImageView = itemView.findViewById(R.id.senderStatus)
        val senderTime: TextView = itemView.findViewById(R.id.senderTime)
        val layout = itemView.findViewById<ConstraintLayout>(R.id.constraintLayout)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(position: Int) {
            val time = getDate(messageList[position].sendTime.toString().toLong(), "hh:mm aa")
            markWon.setMarkdown(
                senderTextView, Encryption().decryptUsingSymmetricEncryption(
                    Base64.getDecoder().decode(messageList[position].msg),
                    Base64.getDecoder().decode(messageList[position].iv),
                    messageList[position].receiverId.toString()
                )
            )
            senderTime.text = time
        }
    }

    private inner class ViewHolder2(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val receiverTextView: TextView = itemView.findViewById(R.id.receiverMessage)
        val receiverTime: TextView = itemView.findViewById(R.id.receiverTime)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(position: Int) {
            val time = getDate(messageList[position].receiveTime.toString().toLong(), "hh:mm aa")
            markWon.setMarkdown(
                receiverTextView,
                Encryption().decryptUsingSymmetricEncryption(
                    Base64.getDecoder().decode(messageList[position].msg),
                    Base64.getDecoder().decode(messageList[position].iv),
                    messageList[position].senderId.toString()
                )
            )
            receiverTime.text = time
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            if (messageList[position].senderId == installationId) {
                when (messageList[position].mediaType) {
                    0 -> {
                        (holder as ViewHolder1).bind(position)
                        when (messageList[position].messageStatus) {
                            3 -> {
                                Glide.with(context).load(R.drawable.seen).circleCrop().transition(
                                    DrawableTransitionOptions.withCrossFade()
                                )
                                    .into(holder.senderSeen)
                            }
                            0 -> {
                                Glide.with(context).load(R.drawable.sending).circleCrop()
                                    .transition(
                                        DrawableTransitionOptions.withCrossFade()
                                    )
                                    .into(holder.senderSeen)
                            }
                            1 -> {
                                Glide.with(context).load(R.drawable.sent).circleCrop().transition(
                                    DrawableTransitionOptions.withCrossFade()
                                )
                                    .into(holder.senderSeen)
                            }
                            2 -> {
                                Glide.with(context).load(R.drawable.delivered).circleCrop()
                                    .transition(
                                        DrawableTransitionOptions.withCrossFade()
                                    )
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
    }

    fun setData(data: List<ChatModel>, id: String) {

        if (!this::installationId.isInitialized) {
            installationId = id
        }

        if (messageList.isEmpty()) {
            messageList.addAll(data)
            notifyItemRangeChanged(0, data.size)
        } else {
            if (data.isNotEmpty()) {
                if (messageList.size != data.size) {
                    messageList.add(data.last())
                    notifyItemInserted(messageList.size - 1)
                }

                for (i in messageList.lastIndex downTo 0) {
                    if (messageList[i] != data[i]) {
                        messageList[i] = data[i]
                        notifyItemChanged(i, "hello")
                    }
                }
            } else {
                notifyItemRangeRemoved(0, messageList.size)
                messageList.clear()
            }
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (messageList[position].senderId == installationId) {
            when (messageList[position].mediaType) {
                0 -> {
                    (holder as ViewHolder1).bind(position)
                    when (messageList[position].messageStatus) {
                        3 -> {
                            Glide.with(context).load(R.drawable.seen).circleCrop().transition(
                                DrawableTransitionOptions.withCrossFade()
                            )
                                .into(holder.senderSeen)
                        }
                        0 -> {
                            Glide.with(context).load(R.drawable.sending).circleCrop().transition(
                                DrawableTransitionOptions.withCrossFade()
                            )
                                .into(holder.senderSeen)
                        }
                        1 -> {
                            Glide.with(context).load(R.drawable.sent).circleCrop().transition(
                                DrawableTransitionOptions.withCrossFade()
                            )
                                .into(holder.senderSeen)
                        }
                        2 -> {
                            Glide.with(context).load(R.drawable.delivered).circleCrop().transition(
                                DrawableTransitionOptions.withCrossFade()
                            )
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