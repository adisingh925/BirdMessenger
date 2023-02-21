package com.adreal.birdmessenger.Model

data class encryptedModel(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val msg: String,
    val msgStatus: Int,
    val mediaType : Int,
)