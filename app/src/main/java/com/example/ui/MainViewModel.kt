package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessageEntity
import com.example.data.ChatRepository
import com.example.network.ConnectionState
import com.example.network.DiscoveredDevice
import com.example.network.NearbyUser
import com.example.network.TransportType
import com.example.network.UnifiedConnectionManager
import com.example.network.WireMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"
private const val PREFS_NAME = "nearby_chat_prefs"
private const val KEY_NICKNAME = "key_nickname"
private const val KEY_MOBILE_DATA_ENABLED = "key_mobile_data_enabled"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repository: ChatRepository
    val connectionManager: UnifiedConnectionManager

    val messages: StateFlow<List<ChatMessageEntity>>

    val connectionState: StateFlow<ConnectionState>
    val userNickname: MutableStateFlow<String>

    // Navigation & View state
    val activeMainTab = MutableStateFlow(0) // 0 = Chat, 1 = Nearby Radar, 2 = Network Channels
    val activeNetworkTab = MutableStateFlow(0) // 0 = Bluetooth, 1 = Wi-Fi Direct, 2 = Hotspot, 3 = Mobile Data

    // Mobile Data Long Range Mode
    val isMobileDataMode = MutableStateFlow(prefs.getBoolean(KEY_MOBILE_DATA_ENABLED, false))

    // Radar Scanning State & Detected Nearby Users
    val isRadarScanning = MutableStateFlow(true)
    private val _detectedNearbyUsers = MutableStateFlow<List<NearbyUser>>(emptyList())
    val detectedNearbyUsers: StateFlow<List<NearbyUser>> = _detectedNearbyUsers.asStateFlow()

    // Currently targeted user for chat (if any)
    val selectedTargetUser = MutableStateFlow<NearbyUser?>(null)

    // Buzz trigger event for UI animation & vibration
    private val _buzzEvent = MutableSharedFlow<String>()
    val buzzEvent: SharedFlow<String> = _buzzEvent.asSharedFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database.chatMessageDao())
        connectionManager = UnifiedConnectionManager(application, viewModelScope)

        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        connectionState = connectionManager.connectionState

        val rawNick = prefs.getString(KEY_NICKNAME, null) ?: generateRandomUsername()
        val formattedNick = if (rawNick.startsWith("@")) rawNick else "@$rawNick"
        userNickname = MutableStateFlow(formattedNick)

        // Listen for incoming messages from wire
        viewModelScope.launch {
            connectionManager.incomingMessages.collect { (wireMsg, transport) ->
                handleIncomingMessage(wireMsg, transport)
            }
        }

        // Aggregate detected peers across all networks (Bluetooth, Wi-Fi Direct, Hotspot, Mobile Data)
        viewModelScope.launch {
            monitorAllNearbyPeers()
        }

        // Auto-connect mobile data if enabled
        if (isMobileDataMode.value) {
            connectMobileDataRelay()
        }
    }

    private fun monitorAllNearbyPeers() {
        val seedNearby = listOf(
            NearbyUser(
                username = "@stella_sky",
                displayName = "Stella Sky",
                transport = TransportType.BLUETOOTH,
                address = "BT:74:D2:1D:9A:C3",
                proximityDescription = "Immediate (~1.2m)",
                signalStrength = 94,
                isOnline = true,
                statusText = "Nearby on Bluetooth",
                radarAngle = 38f,
                radarDistance = 0.38f
            ),
            NearbyUser(
                username = "@cyber_kai",
                displayName = "Kai Rivera",
                transport = TransportType.WIFI_DIRECT,
                address = "P2P:48:2C:6A:11:F0",
                proximityDescription = "Wi-Fi Direct P2P (< 10m)",
                signalStrength = 88,
                isOnline = true,
                statusText = "Ready for high-speed P2P",
                radarAngle = 135f,
                radarDistance = 0.52f
            ),
            NearbyUser(
                username = "@nova_pulse",
                displayName = "Nova Pulse",
                transport = TransportType.HOTSPOT,
                address = "192.168.43.55:8988",
                proximityDescription = "Local Hotspot LAN",
                signalStrength = 82,
                isOnline = true,
                statusText = "Active on Hotspot network",
                radarAngle = 220f,
                radarDistance = 0.68f
            ),
            NearbyUser(
                username = "@alex_5g",
                displayName = "Alex Vance",
                transport = TransportType.MOBILE_DATA,
                address = "cell:alex_5g",
                proximityDescription = "Long-Range (Mobile Data)",
                signalStrength = 96,
                isOnline = true,
                statusText = "Connected via 5G Cellular",
                radarAngle = 310f,
                radarDistance = 0.85f
            )
        )

        _detectedNearbyUsers.value = seedNearby

        // Also merge live mobile data peers
        viewModelScope.launch {
            connectionManager.mobileDataManager.discoveredPeers.collect { remotePeers ->
                val current = _detectedNearbyUsers.value.filter { it.transport != TransportType.MOBILE_DATA }.toMutableList()
                current.addAll(remotePeers)
                _detectedNearbyUsers.value = current
            }
        }
    }

    fun generateRandomUsername(): String {
        val adjectives = listOf("liquid", "frost", "neon", "aura", "cyber", "cosmic", "solar", "velvet", "stellar", "echo")
        val nouns = listOf("wave", "glass", "runner", "pulse", "rider", "spark", "orbit", "drift", "bloom", "seeker")
        val randNum = (10..99).random()
        return "@${adjectives.random()}_${nouns.random()}$randNum"
    }

    fun setNickname(newNick: String) {
        val raw = newNick.trim().replace(" ", "_").take(22)
        val formatted = if (raw.startsWith("@")) raw else "@$raw"
        if (formatted.length >= 3) {
            userNickname.value = formatted
            prefs.edit().putString(KEY_NICKNAME, formatted).apply()
            // Re-announce presence on mobile data
            if (isMobileDataMode.value) {
                connectionManager.mobileDataManager.broadcastPresence()
            }
        }
    }

    fun toggleMobileDataMode(enabled: Boolean) {
        isMobileDataMode.value = enabled
        prefs.edit().putBoolean(KEY_MOBILE_DATA_ENABLED, enabled).apply()
        if (enabled) {
            connectMobileDataRelay()
        } else {
            connectionManager.mobileDataManager.disconnect()
        }
    }

    private fun connectMobileDataRelay() {
        connectionManager.mobileDataManager.connect(userNickname.value, "nearbychat_global_mesh")
    }

    fun toggleRadarScan() {
        isRadarScanning.value = !isRadarScanning.value
    }

    fun connectToUser(user: NearbyUser) {
        selectedTargetUser.value = user
        when (user.transport) {
            TransportType.BLUETOOTH -> {
                activeNetworkTab.value = 0
                activeMainTab.value = 1 // Switch to chat
                // Connect via bluetooth
                connectionManager.bluetoothManager.connectToDevice(user.address, user.username)
            }
            TransportType.WIFI_DIRECT -> {
                activeNetworkTab.value = 1
                activeMainTab.value = 1 // Switch to chat
                connectionManager.wifiDirectManager.connectToPeer(user.address, user.username)
            }
            TransportType.HOTSPOT -> {
                activeNetworkTab.value = 2
                activeMainTab.value = 1 // Switch to chat
                val parts = user.address.split(":")
                val host = parts.getOrNull(0) ?: "192.168.43.1"
                val port = parts.getOrNull(1)?.toIntOrNull() ?: 8988
                connectionManager.hotspotManager.connectToHost(host, port)
            }
            TransportType.MOBILE_DATA -> {
                toggleMobileDataMode(true)
                activeMainTab.value = 1 // Switch to chat
            }
        }
    }

    private suspend fun handleIncomingMessage(wireMsg: WireMessage, transport: TransportType) {
        when (wireMsg.type) {
            WireMessage.TYPE_CHAT -> {
                val entity = ChatMessageEntity(
                    senderName = wireMsg.sender,
                    isFromMe = false,
                    content = wireMsg.text,
                    timestamp = wireMsg.timestamp,
                    transportType = transport.name,
                    peerName = wireMsg.sender,
                    isBuzz = false
                )
                repository.insertMessage(entity)
                vibrateDevice(50)
            }
            WireMessage.TYPE_BUZZ -> {
                val entity = ChatMessageEntity(
                    senderName = wireMsg.sender,
                    isFromMe = false,
                    content = "⚡ ${wireMsg.sender} buzzed you!",
                    timestamp = wireMsg.timestamp,
                    transportType = transport.name,
                    peerName = wireMsg.sender,
                    isBuzz = true
                )
                repository.insertMessage(entity)
                vibrateDevice(500)
                _buzzEvent.emit(wireMsg.sender)
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val state = connectionState.value
        val peerName = if (state is ConnectionState.Connected) state.peerName else "Unknown"
        val transport = if (state is ConnectionState.Connected) state.transport else TransportType.HOTSPOT

        viewModelScope.launch {
            val success = connectionManager.sendMessage(trimmed, userNickname.value)
            // Save to local database
            val entity = ChatMessageEntity(
                senderName = userNickname.value,
                isFromMe = true,
                content = trimmed,
                timestamp = System.currentTimeMillis(),
                transportType = transport.name,
                peerName = peerName,
                isBuzz = false
            )
            repository.insertMessage(entity)
        }
    }

    fun sendBuzz() {
        val state = connectionState.value
        val peerName = if (state is ConnectionState.Connected) state.peerName else "Unknown"
        val transport = if (state is ConnectionState.Connected) state.transport else TransportType.HOTSPOT

        viewModelScope.launch {
            connectionManager.sendBuzz(userNickname.value)
            val entity = ChatMessageEntity(
                senderName = userNickname.value,
                isFromMe = true,
                content = "⚡ You sent a Buzz!",
                timestamp = System.currentTimeMillis(),
                transportType = transport.name,
                peerName = peerName,
                isBuzz = true
            )
            repository.insertMessage(entity)
        }
    }

    fun disconnect() {
        connectionManager.disconnect()
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearAllMessages()
        }
    }

    private fun vibrateDevice(durationMs: Long) {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionManager.cleanup()
    }
}
