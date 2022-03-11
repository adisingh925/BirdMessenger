package com.adreal.birdmessenger.Adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adreal.birdmessenger.Activity.ChatActivity
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.databinding.PeopleLayoutBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PeopleAdapter(private val context: Context, private val onItemClickListener : OnItemClickListener) :
    RecyclerView.Adapter<PeopleAdapter.myViewHolder>() {

    private lateinit var binding: PeopleLayoutBinding

    private  var peopleList = emptyList<UserModel>()

    interface OnItemClickListener
    {
        fun onItemClick(data : UserModel)
    }

    class myViewHolder(binding : PeopleLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.imageView
        val text = binding.textView
        val lastMessage = binding.lastMessage
        val unseenMessages = binding.unseenMessages
        val timeStamp = binding.timeStamp
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        binding = PeopleLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return myViewHolder(binding)
    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int) {
        Glide.with(context).load(peopleList[position].imageByteArray?.let { base64ToBitmap(it) }).circleCrop().into(holder.image)
        holder.text.text = peopleList[position].userName
        holder.lastMessage.text = peopleList[position].lastMessage
        holder.unseenMessages.text = peopleList[position].unreadMessages.toString()
        if(peopleList[position].lastMessageTimeStamp.toString() == "null")
        {
            holder.timeStamp.text = ""
        }
        else
        {
            holder.timeStamp.text = getDate(peopleList[position].lastMessageTimeStamp.toString().toLong(), "hh:mm aa")
        }
        if(peopleList[position].unreadMessages.toString() == "null")
        {
            holder.unseenMessages.text = ""
        }
        else
        {
            holder.unseenMessages.text = peopleList[position].unreadMessages.toString()
        }

        holder.image.setOnClickListener()
        {
            onItemClickListener.onItemClick(peopleList[position])
        }

        holder.itemView.setOnClickListener()
        {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("receiverName",peopleList[position].userName)
            intent.putExtra("receiverId",peopleList[position].userId)
            intent.putExtra("receiverToken",peopleList[position].userToken)
            //intent.putExtra("receiverImage",peopleList[position].imageByteArray)

            context.startActivity(intent)

            updateUserData(peopleList[position])
        }
    }

    private fun updateUserData(data : UserModel)
    {
        CoroutineScope(Dispatchers.IO).launch {
            data.unreadMessages = null
            Database.getDatabase(context).Dao().updateUserData(data)
        }
    }

    override fun getItemCount(): Int {
        return peopleList.size
    }

    fun setData(data : List<UserModel>)
    {
        this.peopleList = data
        notifyDataSetChanged()
    }

    private fun base64ToBitmap(data: String): Bitmap {
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