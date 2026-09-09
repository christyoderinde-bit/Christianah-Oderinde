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

    fun connect(username: String, room: String = GLOBAL_TOPIC) {
        currentUsername = username
        currentRoom = room.ifBlank { GLOBAL_TOPIC }

        disconnect()

        onConnectionStateChanged(ConnectionState.Connecting(TransportType.MOBILE_DATA, "Long Range Relay ($currentRoom)"))
        _isMobileDataActive.value = true

        val request = Request.Builder()
            .url("$RELAY_BASE_WS/$currentRoom/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket open to $currentRoom")
                _isConnected.value = true
                val netType = if (isUsingCellular()) "Cellular / 5G" else "Internet Relay"
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
            // ntfy format wraps message in {"event":"message", "message":"...", "topic":"..."}
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
                    proximityDescription = "Long-Range (Mobile Data / 5G)",
                    signalStrength = (75..98).random(),
                    isOnline = true,
                    statusText = wireMsg.text.ifBlank { "Active via Cellular" },
                    radarAngle = (cleanSender.hashCode().rem(360) + 360) % 360f,
                    radarDistance = 0.65f + ((cleanSender.length % 4) * 0.08f)
                )
                _remotePeers[cleanSender] = user
                updateDiscoveredPeers()
            } else {
                // Chat or Buzz message
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
            val presenceMsg = WireMessage(
                type = WireMessage.TYPE_PRESENCE,
                sender = currentUsername,
                text = if (isUsingCellular()) "Connected on 5G / Cellular" else "Online on Cloud Relay",
                timestamp = System.currentTimeMillis()
            )
            publishToRelay(presenceMsg)
        }
    }

    suspend fun sendMessage(wireMsg: WireMessage): Boolean {
        return publishToRelay(wireMsg)
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
