package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val TAG = "MobileDataChatManager"
private const val RELAY_BASE_WS = "wss://ntfy.sh"
private const val RELAY_BASE_HTTP = "https://ntfy.sh"
private const val GLOBAL_TOPIC = "nearbychat_global_mesh"

class MobileDataChatManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMessageReceived: (WireMessage) -> Unit,
    private val onConnectionStateChanged: (ConnectionState) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websocket
        .connectTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUsername: String = ""
    private var currentRoom: String = GLOBAL_TOPIC
    private var heartbeatJob: Job? = null

    private val _isMobileDataActive = MutableStateFlow(false)
    val isMobileDataActive: StateFlow<Boolean> = _isMobileDataActive.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _remotePeers = ConcurrentHashMap<String, NearbyUser>()
    private val _discoveredPeers = MutableStateFlow<List<NearbyUser>>(emptyList())
    val discoveredPeers: StateFlow<List<NearbyUser>> = _discoveredPeers.asStateFlow()

    val okHttpClient: OkHttpClient get() = client

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isUsingCellular(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun getNetworkTypeName(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Offline"
        val active = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(active) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "4G LTE / Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Internet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Active Internet"
            else -> "Offline"
        }
    }

    fun connect(username: String, room: String = GLOBAL_TOPIC) {
        currentUsername = username
        currentRoom = room.ifBlank { GLOBAL_TOPIC }

        disconnect()

        onConnectionStateChanged(ConnectionState.Connecting(TransportType.MOBILE_DATA, "4G / Internet Relay ($currentRoom)"))
        _isMobileDataActive.value = true

        val request = Request.Builder()
            .url("$RELAY_BASE_WS/$currentRoom/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket open to $currentRoom")
                _isConnected.value = true
                val netType = getNetworkTypeName()
                onConnectionStateChanged(
                    ConnectionState.Connected(
                        transport = TransportType.MOBILE_DATA,
                        peerName = "Global Mesh ($netType)",
                        peerAddress = "relay:$currentRoom"
                    )
                )

                // Start presence broadcast
                startHeartbeat()
                // Send immediate presence
                broadcastPresence()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingRaw(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket error: ${t.message}")
                _isConnected.value = false
                onConnectionStateChanged(
                    ConnectionState.Error(
                        TransportType.MOBILE_DATA,
                        "Relay disconnect: ${t.localizedMessage ?: "Network error"}"
                    )
                )
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                onConnectionStateChanged(ConnectionState.Disconnected)
            }
        })
    }

    private fun handleIncomingRaw(jsonString: String) {
        try {
            val root = JSONObject(jsonString)
            // 1. Check if ntfy attachment is present
            val attachment = root.optJSONObject("attachment")
            if (attachment != null) {
                val fileUrl = attachment.optString("url", "")
                val fileName = attachment.optString("name", "attachment")
                val fileSize = attachment.optLong("size", 0L)
                val mimeType = attachment.optString("type", "application/octet-stream")
                val title = root.optString("title", "")
                val sender = if (title.startsWith("File from ")) {
                    title.removePrefix("File from ").trim()
                } else {
                    root.optString("sender", "Peer")
                }

                if (!sender.equals(currentUsername, ignoreCase = true)) {
                    val wireMsg = WireMessage(
                        type = WireMessage.TYPE_FILE,
                        sender = sender,
                        text = "Shared file: $fileName",
                        timestamp = root.optLong("time", System.currentTimeMillis() / 1000) * 1000L,
                        fileName = fileName,
                        fileSize = fileSize,
                        fileMimeType = mimeType,
                        fileUrl = fileUrl
                    )
                    onMessageReceived(wireMsg)
                    return
                }
            }

            // 2. Parse as standard WireMessage JSON
            val rawMsg = root.optString("message", jsonString)
            val wireMsg = WireMessage.fromJson(rawMsg) ?: return

            // Avoid echoing our own messages
            if (wireMsg.sender.equals(currentUsername, ignoreCase = true)) {
                return
            }

            // Check if targeted to another user
            if (wireMsg.target.isNotBlank() && !wireMsg.target.equals(currentUsername, ignoreCase = true)) {
                return
            }

            if (wireMsg.type == WireMessage.TYPE_PRESENCE) {
                // Register remote peer
                val cleanSender = wireMsg.sender
                val user = NearbyUser(
                    username = cleanSender,
                    displayName = cleanSender.removePrefix("@"),
                    transport = TransportType.MOBILE_DATA,
                    address = "cell:$cleanSender",
                    proximityDescription = "4G / Internet Relay",
                    signalStrength = (75..98).random(),
                    isOnline = true,
                    statusText = wireMsg.text.ifBlank { "Active via Internet" },
                    radarAngle = (cleanSender.hashCode().rem(360) + 360) % 360f,
                    radarDistance = 0.65f + ((cleanSender.length % 4) * 0.08f)
                )
                _remotePeers[cleanSender] = user
                updateDiscoveredPeers()
            } else {
                // Chat, File, or Buzz message
                onMessageReceived(wireMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing raw relay message: ${e.message}")
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && _isConnected.value) {
                broadcastPresence()
                delay(25000) // Broadcast presence every 25 seconds
            }
        }
    }

    fun broadcastPresence() {
        if (currentUsername.isBlank()) return
        scope.launch(Dispatchers.IO) {
            val netType = getNetworkTypeName()
            val presenceMsg = WireMessage(
                type = WireMessage.TYPE_PRESENCE,
                sender = currentUsername,
                text = "Online via $netType",
                timestamp = System.currentTimeMillis()
            )
            publishToRelay(presenceMsg)
        }
    }

    suspend fun sendMessage(wireMsg: WireMessage): Boolean {
        return publishToRelay(wireMsg)
    }

    suspend fun uploadAndSendFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        senderName: String
    ): WireMessage? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val mediaType = (try {
                mimeType.toMediaTypeOrNull()
            } catch (_: Exception) {
                null
            }) ?: "application/octet-stream".toMediaType()

            val requestBody = fileBytes.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$RELAY_BASE_HTTP/$currentRoom")
                .put(requestBody)
                .header("Filename", fileName)
                .header("Title", "File from $senderName")
                .header("X-Sender", senderName)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val respObj = if (bodyString.isNotBlank()) JSONObject(bodyString) else JSONObject()
                    val attachment = respObj.optJSONObject("attachment")
                    val fileUrl = attachment?.optString("url") ?: "$RELAY_BASE_HTTP/$currentRoom/file/${respObj.optString("id")}"

                    val wireMsg = WireMessage(
                        type = WireMessage.TYPE_FILE,
                        sender = senderName,
                        text = "Shared file: $fileName",
                        timestamp = System.currentTimeMillis(),
                        fileName = fileName,
                        fileSize = fileBytes.size.toLong(),
                        fileMimeType = mimeType,
                        fileUrl = fileUrl
                    )

                    // Also broadcast the wire message explicitly so all listeners get it reliably
                    publishToRelay(wireMsg)
                    wireMsg
                } else {
                    Log.e(TAG, "File upload failed with code: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading file: ${e.message}", e)
            null
        }
    }

    private fun publishToRelay(wireMsg: WireMessage): Boolean {
        return try {
            val jsonPayload = wireMsg.toJson()
            // If websocket is active, we can also publish via HTTP POST to ensure delivery
            val request = Request.Builder()
                .url("$RELAY_BASE_HTTP/$currentRoom")
                .post(jsonPayload.toRequestBody("text/plain".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "publishToRelay failed: ${e.message}")
            false
        }
    }

    private fun updateDiscoveredPeers() {
        _discoveredPeers.value = _remotePeers.values.toList()
    }

    fun clearDiscoveredPeers() {
        _remotePeers.clear()
        _discoveredPeers.value = emptyList()
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        _isConnected.value = false
        _isMobileDataActive.value = false
        onConnectionStateChanged(ConnectionState.Disconnected)
    }

    fun cleanup() {
        disconnect()
    }
}
