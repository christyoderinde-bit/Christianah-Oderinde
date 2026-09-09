package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
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
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

private const val TAG = "HotspotChatManager"

class HotspotChatManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMessageReceived: (WireMessage) -> Unit,
    private val onConnectionStateChanged: (ConnectionState) -> Unit
) {
    companion object {
        const val TCP_PORT = 8988
        const val UDP_BEACON_PORT = 8989
        const val BEACON_PREFIX = "NEARBY_CHAT_BEACON"
    }

    private val _localIp = MutableStateFlow<String?>(null)
    val localIp: StateFlow<String?> = _localIp.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _isBeaconActive = MutableStateFlow(false)
    val isBeaconActive: StateFlow<Boolean> = _isBeaconActive.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredDevice>> = _discoveredPeers.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var beaconSenderJob: Job? = null
    private var beaconReceiverJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var activeWorker: SocketChannelWorker? = null

    private var multicastLock: WifiManager.MulticastLock? = null

    init {
        refreshLocalIp()
    }

    fun refreshLocalIp(): String? {
        val ip = getActiveIPv4Address()
        _localIp.value = ip
        return ip
    }

    fun startServer(customPort: Int = TCP_PORT) {
        stopServer()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(customPort).apply {
                    reuseAddress = true
                }
                _isServerRunning.value = true
                val ip = refreshLocalIp() ?: "Unknown IP"
                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(
                        ConnectionState.Listening(
                            TransportType.HOTSPOT,
                            "Server active at $ip:$customPort"
                        )
                    )
                }

                while (_isServerRunning.value) {
                    val clientSocket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: IOException) {
                        break
                    }
                    val peerIp = clientSocket.inetAddress?.hostAddress ?: "Client"
                    withContext(Dispatchers.Main) {
                        onConnectionStateChanged(
                            ConnectionState.Connected(
                                TransportType.HOTSPOT,
                                "Hotspot Peer ($peerIp)",
                                peerIp
                            )
                        )
                    }
                    setupWorker(clientSocket, "Hotspot Peer ($peerIp)", peerIp)
                    break // Single active 1-to-1 session
                }
            } catch (e: Exception) {
                Log.e(TAG, "Hotspot ServerSocket error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isServerRunning.value = false
                    onConnectionStateChanged(
                        ConnectionState.Error(
                            TransportType.HOTSPOT,
                            "Server error: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    fun stopServer() {
        _isServerRunning.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
    }

    fun connectToHost(hostIp: String, port: Int = TCP_PORT, hostName: String = "Host") {
        disconnect()
        val trimmedIp = hostIp.trim()
        if (trimmedIp.isBlank()) {
            onConnectionStateChanged(ConnectionState.Error(TransportType.HOTSPOT, "Invalid IP address"))
            return
        }

        clientJob = scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                onConnectionStateChanged(ConnectionState.Connecting(TransportType.HOTSPOT, "$hostName ($trimmedIp)"))
            }
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(trimmedIp, port), 5000)

                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(
                        ConnectionState.Connected(
                            TransportType.HOTSPOT,
                            hostName,
                            trimmedIp
                        )
                    )
                }
                setupWorker(socket, hostName, trimmedIp)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to $trimmedIp:$port: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(
                        ConnectionState.Error(
                            TransportType.HOTSPOT,
                            "Could not connect to $trimmedIp: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    private fun setupWorker(socket: Socket, peerName: String, peerAddress: String) {
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

    fun disconnect() {
        clientJob?.cancel()
        activeWorker?.close()
        activeWorker = null
        stopServer()
        onConnectionStateChanged(ConnectionState.Disconnected)
    }

    // --- UDP Auto Discovery Beacon ---

    fun startBeaconDiscovery(deviceName: String) {
        stopBeaconDiscovery()
        _isBeaconActive.value = true

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        try {
            multicastLock = wifiManager?.createMulticastLock("NearbyChatMulticast")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
        }

        // Start listening
        beaconReceiverJob = scope.launch(Dispatchers.IO) {
            try {
                udpSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_BEACON_PORT))
                    broadcast = true
                }
                val buffer = ByteArray(1024)

                while (isActive && _isBeaconActive.value) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket?.receive(packet)
                    val senderIp = packet.address.hostAddress ?: continue
                    val myIp = _localIp.value
                    if (senderIp == myIp || senderIp == "127.0.0.1") {
                        continue // Skip self
                    }
                    val data = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    if (data.startsWith(BEACON_PREFIX)) {
                        val parts = data.split(":")
                        val remoteName = if (parts.size >= 2) parts[1] else "Nearby Device"
                        addDiscoveredPeer(senderIp, remoteName)
                    }
                }
            } catch (e: Exception) {
                if (_isBeaconActive.value) {
                    Log.e(TAG, "Beacon receiver error: ${e.message}")
                }
            }
        }

        // Start broadcasting presence every 2.5 seconds
        beaconSenderJob = scope.launch(Dispatchers.IO) {
            val broadcastAddress = getBroadcastAddress() ?: InetAddress.getByName("255.255.255.255")
            val message = "$BEACON_PREFIX:$deviceName:$TCP_PORT"
            val sendData = message.toByteArray(Charsets.UTF_8)

            while (isActive && _isBeaconActive.value) {
                try {
                    val sendSocket = DatagramSocket()
                    sendSocket.broadcast = true
                    val packet = DatagramPacket(sendData, sendData.size, broadcastAddress, UDP_BEACON_PORT)
                    sendSocket.send(packet)
                    sendSocket.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Beacon send error: ${e.message}")
                }
                delay(2500)
            }
        }
    }

    fun stopBeaconDiscovery() {
        _isBeaconActive.value = false
        beaconSenderJob?.cancel()
        beaconReceiverJob?.cancel()
        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    private fun addDiscoveredPeer(ip: String, name: String) {
        val current = _discoveredPeers.value.toMutableList()
        if (current.none { it.address == ip }) {
            current.add(
                DiscoveredDevice(
                    id = ip,
                    name = name,
                    address = ip,
                    transport = TransportType.HOTSPOT,
                    statusDescription = "Discovered on Wi-Fi / Hotspot"
                )
            )
            _discoveredPeers.value = current
        }
    }

    fun clearDiscoveredPeers() {
        _discoveredPeers.value = emptyList()
    }

    private fun getActiveIPv4Address(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First check Wi-Fi or tethering interfaces (wlan, ap, rndis)
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                            if (intf.name.contains("wlan", ignoreCase = true) ||
                                intf.name.contains("ap", ignoreCase = true) ||
                                intf.name.contains("p2p", ignoreCase = true)) {
                                return host
                            }
                        }
                    }
                }
            }
            // Fallback to any valid non-loopback IPv4
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP: ${e.message}")
        }
        return null
    }

    private fun getBroadcastAddress(): InetAddress? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (interfaceAddress in intf.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null) {
                        return broadcast
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting broadcast address: ${e.message}")
        }
        return null
    }

    fun cleanup() {
        stopBeaconDiscovery()
        disconnect()
    }
}
