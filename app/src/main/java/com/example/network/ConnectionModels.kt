package com.example.network

import org.json.JSONObject
import java.util.UUID

enum class TransportType(val displayName: String) {
    BLUETOOTH("Bluetooth"),
    WIFI_DIRECT("Wi-Fi Direct"),
    HOTSPOT("Hotspot LAN"),
    MOBILE_DATA("4G / Internet")
}

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    data class Listening(val transport: TransportType, val details: String) : ConnectionState
    data class Connecting(val transport: TransportType, val peerName: String) : ConnectionState
    data class Connected(
        val transport: TransportType,
        val peerName: String,
        val peerAddress: String
    ) : ConnectionState
    data class Error(val transport: TransportType?, val message: String) : ConnectionState
}

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val address: String,
    val transport: TransportType,
    val isPaired: Boolean = false,
    val statusDescription: String = ""
)

data class WireMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: String = TYPE_CHAT,
    val sender: String,
    val text: String,
    val target: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fileName: String = "",
    val fileSize: Long = 0L,
    val fileMimeType: String = "",
    val fileData: String = "",
    val fileUrl: String = ""
) {
    companion object {
        const val TYPE_CHAT = "CHAT"
        const val TYPE_BUZZ = "BUZZ"
        const val TYPE_FILE = "FILE"
        const val TYPE_HANDSHAKE = "HANDSHAKE"
        const val TYPE_ACK = "ACK"
        const val TYPE_PRESENCE = "PRESENCE"

        fun fromJson(jsonStr: String): WireMessage? {
            return try {
                val obj = JSONObject(jsonStr)
                WireMessage(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    type = obj.optString("type", TYPE_CHAT),
                    sender = obj.optString("sender", "Unknown"),
                    text = obj.optString("text", ""),
                    target = obj.optString("target", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    fileName = obj.optString("fileName", ""),
                    fileSize = obj.optLong("fileSize", 0L),
                    fileMimeType = obj.optString("fileMimeType", ""),
                    fileData = obj.optString("fileData", ""),
                    fileUrl = obj.optString("fileUrl", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("type", type)
        obj.put("sender", sender)
        obj.put("text", text)
        obj.put("target", target)
        obj.put("timestamp", timestamp)
        if (fileName.isNotEmpty()) obj.put("fileName", fileName)
        if (fileSize > 0) obj.put("fileSize", fileSize)
        if (fileMimeType.isNotEmpty()) obj.put("fileMimeType", fileMimeType)
        if (fileData.isNotEmpty()) obj.put("fileData", fileData)
        if (fileUrl.isNotEmpty()) obj.put("fileUrl", fileUrl)
        return obj.toString()
    }
}

data class NearbyUser(
    val username: String,
    val displayName: String,
    val transport: TransportType,
    val address: String,
    val proximityDescription: String,
    val signalStrength: Int = 85, // 0..100
    val isOnline: Boolean = true,
    val statusText: String = "Active now",
    val radarAngle: Float = 0f, // 0..360
    val radarDistance: Float = 0.5f // 0.2..0.9
)
