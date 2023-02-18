package com.adreal.birdmessenger.Adapter

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adreal.birdmessenger.Activity.ChatActivity
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.databinding.PeopleLayoutBinding
import com.bumptech.glide.Glide
import io.noties.markwon.Markwon
import io.noties.markwon.movement.MovementMethodPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PeopleAdapter(
    private val context: Context,
    private val onItemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<PeopleAdapter.myViewHolder>() {

    private lateinit var binding: PeopleLayoutBinding

    private var peopleList : MutableList<UserModel> = ArrayList()

    private val markWon by lazy {
        context.let { Markwon.builder(context).usePlugin(MovementMethodPlugin.none()).build() }
    }

    interface OnItemClickListener {
        fun onItemClick(data: UserModel, type: Int)
    }

    class myViewHolder(binding: PeopleLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.imageView
        val text = binding.textView
        val lastMessage = binding.lastMessage
        val unseenMessages = binding.unseenMessages
        val timeStamp = binding.timeStamp
        val parent = binding.peopleLayoutParent
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        binding = PeopleLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return myViewHolder(binding)
    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int) {
        Log.d("working","working")
        if (peopleList[position].imageByteArray.toString() == "null") {
            Glide.with(context)
                .load(
                    base64ToBitmap(
                        com.adreal.birdmessenger.SharedPreferences.SharedPreferences.read(
                            "image",
                            ""
                        )
                    )
                )
                .circleCrop().into(holder.image)
        } else {
            Glide.with(context)
                .load(base64ToBitmap(peopleList[position].imageByteArray))
                .circleCrop().into(holder.image)
        }

        holder.text.text = peopleList[position].userName

        markWon.setMarkdown(holder.lastMessage, peopleList[position].lastMessage.toString())

        holder.unseenMessages.text = peopleList[position].unreadMessages.toString()

        if (peopleList[position].lastMessageTimeStamp == null) {
            holder.timeStamp.text = ""
        } else {
            holder.timeStamp.text = getDate(peopleList[position].lastMessageTimeStamp.toString().toLong(), "hh:mm aa")
        }

        if (peopleList[position].unreadMessages == 0) {
            holder.unseenMessages.text = ""
        } else {
            holder.unseenMessages.text = peopleList[position].unreadMessages.toString()
        }

        holder.image.setOnClickListener()
        {
            onItemClickListener.onItemClick(peopleList[position], 0)
        }

        holder.itemView.setOnClickListener()
        {
            onItemClickListener.onItemClick(peopleList[position], 1)
        }
    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int, payloads: MutableList<Any>) {
        if(payloads.isEmpty()){
            super.onBindViewHolder(holder, position, payloads)
        }else{
            Log.d("payload", payloads.toString())
            holder.lastMessage.text = peopleList[position].lastMessage

            holder.timeStamp.text = getDate(peopleList[position].lastMessageTimeStamp.toString().toLong(), "hh:mm aa")

            if (peopleList[position].unreadMessages == 0) {
                holder.unseenMessages.text = ""
            } else {
                holder.unseenMessages.text = peopleList[position].unreadMessages.toString()
            }
        }
    }

    override fun getItemCount(): Int {
        return peopleList.size
    }

    fun setData(data: List<UserModel>) {
        if(peopleList.isEmpty()){
            Log.d("list", "initialized")
            peopleList = data as MutableList<UserModel>
            notifyItemRangeChanged(0,data.size)
        }else{
            if (data.size == peopleList.size) {
                if (data[0].Id != peopleList[0].Id) {
                    val iterator = peopleList.iterator()
                    while (iterator.hasNext()) {
                        val person = iterator.next()
                        if (person.Id == data[0].Id) {
                            val index = peopleList.indexOf(person)
                            iterator.remove()
                            peopleList.add(0, data[0])
                            notifyItemMoved(index, 0)
                            peopleList[0] = data[0]
                            notifyItemChanged(0)
                            break
                        }
                    }
                } else {
                    peopleList[0] = data[0]
                    notifyItemChanged(0, "hello")
                }
            }
        }
    }

    private fun base64ToBitmap(data: String?): Bitmap {
        val imageBytes = Base64.decode(data, 0)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }
}