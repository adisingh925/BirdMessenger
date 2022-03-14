package com.adreal.birdmessenger.Model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "message")
data class ChatModel(
    @NotNull @PrimaryKey @ColumnInfo(name = "messageId") val messageId : Long,
    @ColumnInfo(name = "senderId") val senderId : String?,
    @ColumnInfo(name = "senderToken") val senderToken : String?,
    @ColumnInfo(name = "sendTime") val sendTime : Long?,
    @ColumnInfo(name = "senderName") val senderName : String?,
    @ColumnInfo(name = "receiverId") val receiverId : String?,
    @ColumnInfo(name = "receiverToken") val receiverToken : String?,
    @ColumnInfo(name = "receiveTime") var receiveTime : Long?,
    @ColumnInfo(name = "msg") val msg : String?,
    @ColumnInfo(name = "messageStatus") var messageStatus : Int?,       //0 - sending, 1 - sent, 2 - delivered, 3 - seen
    @ColumnInfo(name = "mediaType") var mediaType : Int?,               //0 - text, 1 - image, 2 - audio, 3 - video, 4 - contact, 5 - location, 6 - document
    @ColumnInfo(name = "mediaThumbnail") var mediaThumbnail : String?,
    //@ColumnInfo(name = "messageHash") var messageHash : String?,      data limit problem
    @ColumnInfo(name = "mediaUrl") var mediaUrl : String?,              //first server then local
    @ColumnInfo(name = "mediaExtension") var mediaExtension : String?,
    @ColumnInfo(name = "mediaSize") var mediaSize : Long?,              //in kb
    @ColumnInfo(name = "mediaName") var mediaName : String?,
    @ColumnInfo(name = "mediaDuration") var mediaDuration : Long?,      //only when audio or video
    @ColumnInfo(name = "latitude") var latitude : Long?,
    @ColumnInfo(name = "longitude") var longitude : Long?,
)