package com.adreal.birdmessenger.Model.FCMResponse

data class ChatResponse(
    val canonical_ids: Int,
    val failure: Int,
    val multicast_id: Long,
    val results: List<Result>,
    val success: Int
)