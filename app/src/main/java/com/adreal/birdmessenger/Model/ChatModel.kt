package com.adreal.birdmessenger.Model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "message")
data class ChatModel(
    @PrimaryKey @ColumnInfo(name = "messageId") val messageId : Long,
    @ColumnInfo(name = "senderId") val senderId : String,
    @ColumnInfo(name = "sendTime") val sendTime : Long,
    @ColumnInfo(name = "receiverId") val receiverId : String,
    @ColumnInfo(name = "receiveTime") var receiveTime : Long?,
    @ColumnInfo(name = "msg") val msg : String,
    @ColumnInfo(name = "messageStatus") var messageStatus : Int,       //0 - sending, 1 - sent, 2 - delivered, 3 - seen 4 - seen pending 5 - delivery pending
    @ColumnInfo(name = "mediaType") var mediaType : Int,               //0 - text, 1 - image, 2 - audio, 3 - video, 4 - contact, 5 - location, 6 - document
)