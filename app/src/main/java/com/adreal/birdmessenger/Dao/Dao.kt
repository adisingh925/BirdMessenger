package com.adreal.birdmessenger.Dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import androidx.room.Dao
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.TokenAndUserName
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.Model.VideoCallModel

@Dao
interface Dao {

    /**All functions for chat and user data related operations*/

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addNewUser(data : UserModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addChatData(data : ChatModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addCallData(data: VideoCallModel)

    @Query("select * from CallData where id = :id")
    fun isOfferReceived(id : String) : VideoCallModel

    @Query("delete from CallData")
    fun deleteCallData()

    @Update
    fun updateChatData(data : ChatModel)

    @Update
    fun updateUserData(data : UserModel)

    @Query("select * from message where senderId = :senderId and receiverId = :receiverId or senderId = :receiverId and receiverId = :senderId order by sendTime desc")
    fun readAllMessages(senderId: String,receiverId : String) : DataSource.Factory<Int, ChatModel>

    @Query("select * from user order by lastMessageTimeStamp desc")
    fun readAllUsers() : LiveData<List<UserModel>>

    @Query("select * from CallData")
    fun getAllCalls() : LiveData<List<VideoCallModel>>

    @Query("update user set lastMessage = :msg, lastMessageTimeStamp = :time where Id = :id")
    fun updateLastMessage(msg: String, time: Long, id: String)

    @Query("update user set unreadMessages = 0 where Id = :id")
    fun resetUnseenMessageCount(id : String)

    @Query("select * from message where messageStatus = 0 or messageStatus = 5 or messageStatus = 4")
    fun readAllMessagesForWorker() : List<ChatModel>

    @Query("select * from message where senderId = :senderId and receiverId = :receiverId or senderId = :receiverId and receiverId = :senderId order by sendTime desc limit 4")
    fun readMessagesForNotification(senderId : String, receiverId : String) : List<ChatModel>

    @Query("select imageByteArray from user where Id = :senderId")
    fun readImageStringForUser(senderId : String) : String

    @Query("select * from user where Id = :userId")
    fun readUserForUpdate(userId : String) : UserModel

    @Query("update user set unreadMessages = unreadMessages + 1, lastMessage = :msg, lastMessageTimeStamp = :time where Id = :id")
    fun incrementUnreadMessages(id : String, msg : String, time : Long)

    @Query("select count(messageStatus) from message where senderId = :senderId and messageStatus = 2")
    fun readNumberOfDeliveredMessagesForUser(senderId: String) : Int

    @Query("update message set messageStatus = :status where rowid = :id")
    fun updateMessageStatus(status : Int,id : Long)

    @Query("update user set unreadMessages = null where Id = :uid")
    fun updateUserCardData(uid : String)

    @Query("select imageByteArray from user where Id = :uid")
    fun getImageData(uid : String) : String

    @Query("delete from message where senderId = :senderId and receiverId = :receiverId or senderId = :receiverId and receiverId = :senderId")
    fun deleteAllChatsBetweenUsers(senderId : String, receiverId : String)

    @Query("select userToken, userName from user where Id = :uid")
    fun getTokenAndUserName(uid : String) : TokenAndUserName

    @Query("select userToken from user where Id = :uid")
    fun getToken(uid : String) : String

    @Query("select userName from user where Id = :uid")
    fun getUserName(uid : String) : String

    @Query("select * from message where rowid = :mid")
    fun getMessage(mid : Long) : ChatModel
}