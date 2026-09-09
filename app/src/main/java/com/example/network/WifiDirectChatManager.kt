package com.example.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

private const val TAG = "WifiDirectChatManager"

class WifiDirectChatManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMessageReceived: (WireMessage) -> Unit,
    private val onConnectionStateChanged: (ConnectionState) -> Unit
) {
    companion object {
        const val P2P_PORT = 8888
    }

    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _isWifiP2pEnabled = MutableStateFlow(false)
    val isWifiP2pEnabled: StateFlow<Boolean> = _isWifiP2pEnabled.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredDevice>> = _discoveredPeers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _thisDevice = MutableStateFlow<WifiP2pDevice?>(null)
    val thisDevice: StateFlow<WifiP2pDevice?> = _thisDevice.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var socketJob: Job? = null
    private var activeWorker: SocketChannelWorker? = null

    private var isReceiverRegistered = false

    private val p2pReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    _isWifiP2pEnabled.value = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.let { mgr ->
                        channel?.let { ch ->
                            try {
                                mgr.requestPeers(ch) { peers: WifiP2pDeviceList? ->
                                    val list = peers?.deviceList?.map { dev ->
                                        val statusText = when (dev.status) {
                                            WifiP2pDevice.AVAILABLE -> "Available"
                                            WifiP2pDevice.INVITED -> "Invited"
                                            WifiP2pDevice.CONNECTED -> "Connected"
                                            WifiP2pDevice.FAILED -> "Failed"
                                            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
                                            else -> "Unknown"
                                        }
                                        DiscoveredDevice(
                                            id = dev.deviceAddress,
                                            name = dev.deviceName.ifBlank { "Wi-Fi Direct Peer" },
                                            address = dev.deviceAddress,
                                            transport = TransportType.WIFI_DIRECT,
                                            statusDescription = statusText
                                        )
                                    } ?: emptyList()
                                    _discoveredPeers.value = list
                                }
                            } catch (e: SecurityException) {
                                Log.e(TAG, "Missing permission in requestPeers: ${e.message}")
                            }
                        }
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                    } else {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }

                    if (networkInfo?.isConnected == true) {
                        channel?.let { ch ->
                            manager?.requestConnectionInfo(ch) { info ->
                                _connectionInfo.value = info
                                handleP2pConnection(info)
                            }
                        }
                    } else {
                        _connectionInfo.value = null
                        disconnectSocket()
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    _thisDevice.value = device
                }
            }
        }
    }

    init {
        manager?.let { mgr ->
            channel = mgr.initialize(context, context.mainLooper, null)
        }
    }

    fun isSupported(): Boolean = manager != null && channel != null

    fun register() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            context.registerReceiver(p2pReceiver, intentFilter)
            isReceiverRegistered = true
        }
    }

    @SuppressLint("MissingPermission")
    fun startPeerDiscovery() {
        val mgr = manager ?: return
        val ch = channel ?: return
        register()
        try {
            _isDiscovering.value = true
            mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    _isDiscovering.value = true
                }

                override fun onFailure(reasonCode: Int) {
                    _isDiscovering.value = false
                    Log.e(TAG, "discoverPeers failed: $reasonCode")
                }
            })
        } catch (e: SecurityException) {
            _isDiscovering.value = false
            Log.e(TAG, "SecurityException starting peer discovery: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopPeerDiscovery() {
        val mgr = manager ?: return
        val ch = channel ?: return
        try {
            mgr.stopPeerDiscovery(ch, null)
        } catch (_: SecurityException) {}
        _isDiscovering.value = false
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(deviceAddress: String, peerName: String) {
        val mgr = manager ?: return
        val ch = channel ?: return
        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
        }

        onConnectionStateChanged(ConnectionState.Connecting(TransportType.WIFI_DIRECT, peerName))
        try {
            mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Connect initiated to $peerName")
                }

                override fun onFailure(reason: Int) {
                    onConnectionStateChanged(ConnectionState.Error(TransportType.WIFI_DIRECT, "Connection failed ($reason)"))
                }
            })
        } catch (e: SecurityException) {
            onConnectionStateChanged(ConnectionState.Error(TransportType.WIFI_DIRECT, "Permission missing for Wi-Fi Direct: ${e.message}"))
        }
    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        val mgr = manager ?: return
        val ch = channel ?: return
        register()
        try {
            mgr.createGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    onConnectionStateChanged(ConnectionState.Listening(TransportType.WIFI_DIRECT, "Wi-Fi Direct Group created (Host)"))
                }

                override fun onFailure(reason: Int) {
                    onConnectionStateChanged(ConnectionState.Error(TransportType.WIFI_DIRECT, "Failed to create group: $reason"))
                }
            })
        } catch (e: SecurityException) {
            onConnectionStateChanged(ConnectionState.Error(TransportType.WIFI_DIRECT, "Permission missing: ${e.message}"))
        }
    }

    private fun handleP2pConnection(info: WifiP2pInfo) {
        if (!info.groupFormed) return

        disconnectSocket()
        socketJob = scope.launch(Dispatchers.IO) {
            try {
                if (info.isGroupOwner) {
                    // Group Owner: Run ServerSocket
                    serverSocket = ServerSocket(P2P_PORT).apply {
                        reuseAddress = true
                    }
                    withContext(Dispatchers.Main) {
                        onConnectionStateChanged(
                            ConnectionState.Listening(TransportType.WIFI_DIRECT, "Host ready. Waiting for peer...")
                        )
                    }
                    val clientSocket = serverSocket?.accept() ?: return@launch
                    val peerIp = clientSocket.inetAddress?.hostAddress ?: "Peer"
                    withContext(Dispatchers.Main) {
                        onConnectionStateChanged(
                            ConnectionState.Connected(TransportType.WIFI_DIRECT, "Wi-Fi Direct Client", peerIp)
                        )
                    }
                    setupWorker(clientSocket)
                } else {
                    // Client: Connect to Group Owner Address
                    val hostAddress = info.groupOwnerAddress?.hostAddress
                    if (hostAddress.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            onConnectionStateChanged(ConnectionState.Error(TransportType.WIFI_DIRECT, "Group Owner IP not available"))
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        onConnectionStateChanged(ConnectionState.Connecting(TransportType.WIFI_DIRECT, "Group Owner ($hostAddress)"))
                    }

                    // Retry connect a few times since group owner server socket may take 500ms to open
                    var socket: Socket? = null
                    var attempts = 0
                    while (attempts < 10 && socket == null) {
                        try {
                            val s = Socket()
                            s.connect(InetSocketAddress(hostAddress, P2P_PORT), 2500)
                            socket = s
                        } catch (e: IOException) {
                            attempts++
                            kotlinx.coroutines.delay(800)
                        }
                    }

                    if (socket != null) {
                        withContext(Dispatchers.Main) {
                            onConnectionStateChanged(
                                ConnectionState.Connected(TransportType.WIFI_DIRECT, "Wi-Fi Direct Host", hostAddress)
                            )
                        }
                        setupWorker(socket)
                    } else {
                        withContext(Dispatchers.Main) {
                            onConnectionStateChanged(ConnectionState.Error(TransportType.WIFI_DIRECT, "Could not reach Group Owner at $hostAddress"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "P2P Socket communication error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(ConnectionState.Error(TransportType.WIFI_DIRECT, "Socket error: ${e.message}"))
                }
            }
        }
    }

    private fun setupWorker(socket: Socket) {
        activeWorker?.close()
        val worker = SocketChannelWorker(
            inputStream = socket.getInputStream(),
            outputStream = socket.getOutputStream(),
            underlyingCloseable = socket,
            scope = scope,
            onMessage = onMessageReceived,
            onDisconnected = {
                onConnectionStateChanged(ConnectionState.Disconnected)
            }
        )
        activeWorker = worker
        worker.start()
    }

    suspend fun sendMessage(message: WireMessage): Boolean {
        return activeWorker?.sendMessage(message) ?: false
    }

    private fun disconnectSocket() {
        socketJob?.cancel()
        activeWorker?.close()
        activeWorker = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        disconnectSocket()
        val mgr = manager ?: return
        val ch = channel ?: return
        try {
            mgr.removeGroup(ch, null)
        } catch (_: Exception) {}
        onConnectionStateChanged(ConnectionState.Disconnected)
    }

    fun cleanup() {
        disconnect()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(p2pReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }
}
