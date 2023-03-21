package com.adreal.birdmessenger.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("CallData")
data class VideoCallModel(
    @PrimaryKey val id : String,
    val serverUrl : String,
    val sdpMid : String,
    val sdpMLineIndex : String,
    val sdpCandidate : String,
    val type : String
)