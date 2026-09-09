package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val isFromMe: Boolean,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val transportType: String, // "BLUETOOTH", "WIFI_DIRECT", "HOTSPOT"
    val peerName: String,
    val isBuzz: Boolean = false
)
