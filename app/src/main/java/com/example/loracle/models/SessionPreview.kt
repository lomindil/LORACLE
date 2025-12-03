package com.example.loracle.models

data class SessionPreview(
    val sessionId: String,
    val title: String,
    val lastMessage: String,
    val lastTimestamp: Long = System.currentTimeMillis()
)
