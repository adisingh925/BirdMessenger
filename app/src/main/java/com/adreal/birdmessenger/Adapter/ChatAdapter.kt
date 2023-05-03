package com.adreal.birdmessenger.Adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.noties.markwon.Markwon
import io.noties.markwon.movement.MovementMethodPlugin
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val context: Context,
    private val onItemSeenListener: OnItemSeenListener,
    private val installationId : String
) : PagingDataAdapter<ChatModel, ViewHolder>(COMPARATOR) {

    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<ChatModel>() {
            override fun areItemsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
                return oldItem.messageId == newItem.messageId
            }

            override fun areContentsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
                return oldItem == newItem
            }
        }
    }


    private val markWon by lazy {
        context.let { Markwon.builder(context).usePlugin(MovementMethodPlugin.none()).build() }
    }

    interface OnItemSeenListener {
        fun onItemSeen(data: ChatModel)
    }

    private val VIEW_TYPE_ONE = 1

    private val VIEW_TYPE_TWO = 2

    private inner class ViewHolder1(itemView: View) : ViewHolder(itemView) {

        var senderTextView: TextView = itemView.findViewById(R.id.senderMessage)
        val senderSeen: ImageView = itemView.findViewById(R.id.senderStatus)
        val senderTime: TextView = itemView.findViewById(R.id.senderTime)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(position: Int) {
            val time = getDate(getItem(position)?.sendTime.toString().toLong(), "hh:mm aa")
            markWon.setMarkdown(senderTextView, getItem(position)?.msg.toString())
            senderTime.text = time
        }
    }

    private inner class ViewHolder2(itemView: View) : ViewHolder(itemView) {

        val receiverTextView: TextView = itemView.findViewById(R.id.receiverMessage)
        val receiverTime: TextView = itemView.findViewById(R.id.receiverTime)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(position: Int) {
            val time = getDate(getItem(position)?.receiveTime.toString().toLong(), "hh:mm aa")
            markWon.setMarkdown(receiverTextView, getItem(position)?.msg.toString())
            receiverTime.text = time
        }
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        val formatter = SimpleDateFormat(dateFormat)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

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
        if (getItem(position)?.senderId == installationId) {
            when (getItem(position)?.mediaType) {
                0 -> return VIEW_TYPE_ONE
                else -> VIEW_TYPE_TWO
            }
        } else {
            when (getItem(position)?.mediaType) {
                0 -> return VIEW_TYPE_TWO
                else -> VIEW_TYPE_TWO
            }
        }
        return VIEW_TYPE_ONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItem(position)?.senderId == installationId) {
            when (getItem(position)?.mediaType) {
                0 -> {
                    (holder as ViewHolder1).bind(position)
                    when (getItem(position)?.messageStatus) {
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
        } else if (getItem(position)?.receiverId == installationId) {
            when (getItem(position)?.mediaType) {
                0 -> {
                    (holder as ViewHolder2).bind(position)
                    if (getItem(position)?.receiverId == installationId && getItem(position)?.messageStatus == 2) {
                        getItem(position)?.let { onItemSeenListener.onItemSeen(it) }
                    }
                }
            }
        }
    }
}