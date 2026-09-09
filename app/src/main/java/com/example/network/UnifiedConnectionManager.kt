package com.example.network

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UnifiedConnectionManager(
    context: Context,
    private val scope: CoroutineScope
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Pair<WireMessage, TransportType>>()
    val incomingMessages: SharedFlow<Pair<WireMessage, TransportType>> = _incomingMessages.asSharedFlow()

    val bluetoothManager: BluetoothChatManager = BluetoothChatManager(
        context = context,
        scope = scope,
        onMessageReceived = { wireMsg ->
            scope.launch {
                _incomingMessages.emit(Pair(wireMsg, TransportType.BLUETOOTH))
            }
        },
        onConnectionStateChanged = { state ->
            handleStateChange(TransportType.BLUETOOTH, state)
        }
    )

    val wifiDirectManager: WifiDirectChatManager = WifiDirectChatManager(
        context = context,
        scope = scope,
        onMessageReceived = { wireMsg ->
            scope.launch {
                _incomingMessages.emit(Pair(wireMsg, TransportType.WIFI_DIRECT))
            }
        },
        onConnectionStateChanged = { state ->
            handleStateChange(TransportType.WIFI_DIRECT, state)
        }
    )

    val hotspotManager: HotspotChatManager = HotspotChatManager(
        context = context,
        scope = scope,
        onMessageReceived = { wireMsg ->
            scope.launch {
                _incomingMessages.emit(Pair(wireMsg, TransportType.HOTSPOT))
            }
        },
        onConnectionStateChanged = { state ->
            handleStateChange(TransportType.HOTSPOT, state)
        }
    )

    val mobileDataManager: MobileDataChatManager = MobileDataChatManager(
        context = context,
        scope = scope,
        onMessageReceived = { wireMsg ->
            scope.launch {
                _incomingMessages.emit(Pair(wireMsg, TransportType.MOBILE_DATA))
            }
        },
        onConnectionStateChanged = { state ->
            handleStateChange(TransportType.MOBILE_DATA, state)
        }
    )

    private fun handleStateChange(transport: TransportType, newState: ConnectionState) {
        // Only override state if it's an active transition or if currently disconnected/matching
        val current = _connectionState.value
        when (newState) {
            is ConnectionState.Connected,
            is ConnectionState.Connecting,
            is ConnectionState.Listening -> {
                _connectionState.value = newState
            }
            is ConnectionState.Disconnected -> {
                if (current is ConnectionState.Connected && current.transport == transport) {
                    _connectionState.value = ConnectionState.Disconnected
                } else if (current is ConnectionState.Listening && current.transport == transport) {
                    _connectionState.value = ConnectionState.Disconnected
                } else if (current is ConnectionState.Connecting && current.transport == transport) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
            is ConnectionState.Error -> {
                _connectionState.value = newState
            }
        }
    }

    suspend fun sendMessage(text: String, senderName: String): Boolean {
        val state = _connectionState.value
        if (state !is ConnectionState.Connected) return false

        val wireMessage = WireMessage(
            type = WireMessage.TYPE_CHAT,
            sender = senderName,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        return when (state.transport) {
            TransportType.BLUETOOTH -> bluetoothManager.sendMessage(wireMessage)
            TransportType.WIFI_DIRECT -> wifiDirectManager.sendMessage(wireMessage)
            TransportType.HOTSPOT -> hotspotManager.sendMessage(wireMessage)
            TransportType.MOBILE_DATA -> mobileDataManager.sendMessage(wireMessage)
        }
    }

    suspend fun sendBuzz(senderName: String): Boolean {
        val state = _connectionState.value
        if (state !is ConnectionState.Connected) return false

        val wireMessage = WireMessage(
            type = WireMessage.TYPE_BUZZ,
            sender = senderName,
            text = "BUZZ!",
            timestamp = System.currentTimeMillis()
        )

        return when (state.transport) {
            TransportType.BLUETOOTH -> bluetoothManager.sendMessage(wireMessage)
            TransportType.WIFI_DIRECT -> wifiDirectManager.sendMessage(wireMessage)
            TransportType.HOTSPOT -> hotspotManager.sendMessage(wireMessage)
            TransportType.MOBILE_DATA -> mobileDataManager.sendMessage(wireMessage)
        }
    }

    fun disconnect() {
        val current = _connectionState.value
        if (current is ConnectionState.Connected) {
            when (current.transport) {
                TransportType.BLUETOOTH -> bluetoothManager.disconnect()
                TransportType.WIFI_DIRECT -> wifiDirectManager.disconnect()
                TransportType.HOTSPOT -> hotspotManager.disconnect()
                TransportType.MOBILE_DATA -> mobileDataManager.disconnect()
            }
        } else {
            bluetoothManager.disconnect()
            wifiDirectManager.disconnect()
            hotspotManager.disconnect()
            mobileDataManager.disconnect()
        }
        _connectionState.value = ConnectionState.Disconnected
    }

    fun cleanup() {
        bluetoothManager.cleanup()
        wifiDirectManager.cleanup()
        hotspotManager.cleanup()
        mobileDataManager.cleanup()
    }
}
