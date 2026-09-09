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
    val transportType: String, // "BLUETOOTH", "WIFI_DIRECT", "HOTSPOT", "MOBILE_DATA"
    val peerName: String,
    val isBuzz: Boolean = false,
    val isFile: Boolean = false,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val fileMimeType: String? = null,
    val filePath: String? = null,
    val fileUrl: String? = null
)
