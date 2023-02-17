package com.adreal.birdmessenger.Dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.UserModel

@Dao
interface Dao {

    /**All functions for chat and user data related operations*/

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addNewUser(data : UserModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addChatData(data : ChatModel)

    @Update
    fun updateChatData(data : ChatModel)

    @Update
    fun updateUserData(data : UserModel)

    @Query("select * from message where senderId = :senderId and receiverId = :receiverId or senderId = :receiverId and receiverId = :senderId order by sendTime asc")
    fun readAllMessages(senderId: String,receiverId : String) : LiveData<List<ChatModel>>

    @Query("select * from user order by lastMessageTimeStamp desc")
    fun readAllUsers() : LiveData<List<UserModel>>

    @Query("update user set lastMessage = :msg, lastMessageTimeStamp = :time where Id = :id")
    fun updateLastMessage(msg : String, time : Long, id : String)

    @Query("update user set unreadMessages = 0 where Id = :id")
    fun resetUnseenMessageCount(id : String)

//    @Query("update user set lastMessageTimestamp = :time where Id = :id")
//    fun updateLastMessageTimestamp(time : Long, id : String)

    @Query("select * from message")
    fun readAllMessagesForWorker() : List<ChatModel>

    @Query("select * from message where senderId = :senderId and messageStatus = 1 or messageStatus = 2 order by sendTime desc limit 4")
    fun readMessagesForNotification(senderId : String) : List<ChatModel>

    @Query("select imageByteArray from user where Id = :senderId")
    fun readImageStringForUser(senderId : String) : String

    @Query("select * from user where Id = :userId")
    fun readUserForUpdate(userId : String) : UserModel

    @Query("update user set unreadMessages = unreadMessages + 1, lastMessage = :msg, lastMessageTimeStamp = :time where Id = :id")
    fun incrementUnreadMessages(id : String, msg : String, time : Long)

    @Query("select count(messageStatus) from message where senderId = :senderId and messageStatus = 2")
    fun readNumberOfDeliveredMessagesForUser(senderId: String) : Int

    @Query("update message set messageStatus = :status where messageId = :id")
    fun updateMessageStatus(status : Int,id : Long)

    @Query("update user set unreadMessages = null where Id = :uid")
    fun updateUserCardData(uid : String)

    @Query("select imageByteArray from user where Id = :uid")
    fun getImageData(uid : String) : String

    @Query("delete from message where senderId = :senderId and receiverId = :receiverId or senderId = :receiverId and receiverId = :senderId")
    fun deleteAllChatsBetweenUsers(senderId : String, receiverId : String)

    @Query("select userToken from user where Id = :uid")
    fun getToken(uid : String) : String

    @Query("select userName from user where Id = :uid")
    fun getUserName(uid : String) : String
}