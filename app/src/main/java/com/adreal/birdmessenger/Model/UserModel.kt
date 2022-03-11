package com.adreal.birdmessenger.Model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "user")
data class UserModel(
    @NotNull @PrimaryKey @ColumnInfo(name = "Id") val Id : Long,
    @ColumnInfo(name = "userName") val userName : String?,
    @ColumnInfo(name = "userToken") val userToken : String?,
    @ColumnInfo(name = "userId") val userId : String?,
    @ColumnInfo(name = "imageByteArray") val imageByteArray : String?,
    @ColumnInfo(name = "lastMessage") var lastMessage : String?,
    @ColumnInfo(name = "unreadMessages") var unreadMessages : Int?,
    @ColumnInfo(name = "lastMessageTimeStamp") var lastMessageTimeStamp : Long?
)