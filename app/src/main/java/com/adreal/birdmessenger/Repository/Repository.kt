package com.adreal.birdmessenger.Repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import com.adreal.birdmessenger.Dao.Dao
import com.adreal.birdmessenger.Database.Database
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.UserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Repository(private val dao: Dao) {

    val readAllUsers = dao.readAllUsers()

    fun readAllMessages(senderId: String, receiverId: String): DataSource.Factory<Int, ChatModel> {
        return dao.readAllMessages(senderId, receiverId)
    }

    fun addChatData(data: ChatModel) {
        dao.addChatData(data)
    }

    fun addNewUser(data: UserModel) {
        dao.addNewUser(data)
    }

    fun updateChatData(data: ChatModel) {
        dao.updateChatData(data)
    }
}