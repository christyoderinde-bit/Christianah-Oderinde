package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import java.util.UUID

private const val TAG = "BluetoothChatManager"

class BluetoothChatManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMessageReceived: (WireMessage) -> Unit,
    private val onConnectionStateChanged: (ConnectionState) -> Unit
) {
    companion object {
        val CHAT_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        const val SERVICE_NAME = "NearbyChat_BT"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isServerListening = MutableStateFlow(false)
    val isServerListening: StateFlow<Boolean> = _isServerListening.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var activeWorker: SocketChannelWorker? = null

    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { dev ->
                        val name = try { dev.name ?: "Unknown Device" } catch (_: SecurityException) { "Unknown Device" }
                        val address = dev.address ?: ""
                        val isBonded = try { dev.bondState == BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false }
                        val current = _discoveredDevices.value.toMutableList()
                        if (current.none { it.address == address }) {
                            current.add(
                                DiscoveredDevice(
                                    id = address,
                                    name = name,
                                    address = address,
                                    transport = TransportType.BLUETOOTH,
                                    isPaired = isBonded,
                                    statusDescription = if (isBonded) "Paired" else "Discovered"
                                )
                            )
                            _discoveredDevices.value = current
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    fun isSupported(): Boolean = adapter != null

    fun isEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (adapter == null || !adapter.isEnabled) return
        try {
            val bonded = adapter.bondedDevices ?: emptySet()
            val list = bonded.map { dev ->
                val name = dev.name ?: "Unnamed Device"
                val address = dev.address ?: ""
                DiscoveredDevice(
                    id = address,
                    name = name,
                    address = address,
                    transport = TransportType.BLUETOOTH,
                    isPaired = true,
                    statusDescription = "Paired Device"
                )
            }
            _discoveredDevices.value = list
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading bonded devices: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (adapter == null || !adapter.isEnabled) return
        refreshPairedDevices()
        registerReceiver()
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            val started = adapter.startDiscovery()
            _isScanning.value = started
        } catch (e: SecurityException) {
            Log.e(TAG, "Scan permission error: ${e.message}")
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (adapter == null) return
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
        } catch (_: SecurityException) {}
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        if (adapter == null || !adapter.isEnabled) {
            onConnectionStateChanged(ConnectionState.Error(TransportType.BLUETOOTH, "Bluetooth is disabled"))
            return
        }
        stopServer()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, CHAT_UUID)
                _isServerListening.value = true
                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(ConnectionState.Listening(TransportType.BLUETOOTH, "Listening for Bluetooth connections..."))
                }
                while (_isServerListening.value) {
                    val socket: BluetoothSocket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: IOException) {
                        break
                    }
                    val remoteDevice = socket.remoteDevice
                    val remoteName = try { remoteDevice?.name ?: remoteDevice?.address ?: "Peer" } catch (_: SecurityException) { "Peer" }
                    val remoteAddress = remoteDevice?.address ?: ""
                    
                    withContext(Dispatchers.Main) {
                        onConnectionStateChanged(ConnectionState.Connected(TransportType.BLUETOOTH, remoteName, remoteAddress))
                    }
                    
                    // Connected to incoming client, handle stream
                    handleConnectedSocket(socket, remoteName, remoteAddress)
                    break // Stop accepting more for single 1-to-1 session
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isServerListening.value = false
                    onConnectionStateChanged(ConnectionState.Error(TransportType.BLUETOOTH, "Server failed: ${e.message}"))
                }
            }
        }
    }

    fun stopServer() {
        _isServerListening.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String, deviceName: String) {
        if (adapter == null || !adapter.isEnabled) {
            onConnectionStateChanged(ConnectionState.Error(TransportType.BLUETOOTH, "Bluetooth is disabled"))
            return
        }
        stopScan()
        disconnect()

        clientJob = scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                onConnectionStateChanged(ConnectionState.Connecting(TransportType.BLUETOOTH, deviceName))
            }
            try {
                val device = adapter.getRemoteDevice(deviceAddress)
                val socket = device.createRfcommSocketToServiceRecord(CHAT_UUID)
                adapter.cancelDiscovery()
                socket.connect()

                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(ConnectionState.Connected(TransportType.BLUETOOTH, deviceName, deviceAddress))
                }
                handleConnectedSocket(socket, deviceName, deviceAddress)
            } catch (e: Exception) {
                Log.e(TAG, "Client connect error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(ConnectionState.Error(TransportType.BLUETOOTH, "Failed to connect: ${e.message}"))
                }
            }
        }
    }

    private fun handleConnectedSocket(socket: BluetoothSocket, peerName: String, peerAddress: String) {
        activeWorker?.close()
        val worker = SocketChannelWorker(
            inputStream = socket.inputStream,
            outputStream = socket.outputStream,
            underlyingCloseable = socket,
            scope = scope,
            onMessage = onMessageReceived,
            onDisconnected = { reason ->
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

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(receiver, filter)
            isReceiverRegistered = true
        }
    }

    fun cleanup() {
        stopScan()
        disconnect()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }
}
