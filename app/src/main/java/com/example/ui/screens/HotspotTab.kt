package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.ConnectionState
import com.example.network.DiscoveredDevice
import com.example.network.HotspotChatManager
import com.example.ui.MainViewModel
import com.example.ui.theme.HotspotColor

@Composable
fun HotspotTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val hotspotManager = viewModel.connectionManager.hotspotManager

    val localIp by hotspotManager.localIp.collectAsStateWithLifecycle()
    val isServerRunning by hotspotManager.isServerRunning.collectAsStateWithLifecycle()
    val isBeaconActive by hotspotManager.isBeaconActive.collectAsStateWithLifecycle()
    val discoveredPeers by hotspotManager.discoveredPeers.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val userNickname by viewModel.userNickname.collectAsStateWithLifecycle()

    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf(HotspotChatManager.TCP_PORT.toString()) }

    LaunchedEffect(Unit) {
        hotspotManager.refreshLocalIp()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("hotspot_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card with Local IP
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.WifiTethering,
                                contentDescription = null,
                                tint = HotspotColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hotspot & Local Wi-Fi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        IconButton(
                            onClick = { hotspotManager.refreshLocalIp() },
                            modifier = Modifier.testTag("btn_refresh_ip")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh IP", tint = HotspotColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Connect devices when one phone creates a Mobile Hotspot and the other joins it, or both are on the same Wi-Fi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // IP Info Box
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "My Local IP Address",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = localIp ?: "Not Connected (No IP)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (localIp != null) HotspotColor else MaterialTheme.colorScheme.error
                                )
                            }
                            if (localIp != null) {
                                AssistChip(
                                    onClick = { manualIp = localIp ?: "" },
                                    label = { Text("Port 8988", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Server Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Host Chat Server (Port 8988)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (isServerRunning) "Server active • Waiting for clients" else "Enable to allow peers to connect to your IP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isServerRunning,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    hotspotManager.startServer()
                                } else {
                                    hotspotManager.stopServer()
                                }
                            },
                            modifier = Modifier.testTag("switch_hotspot_server")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Beacon Auto Discovery Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Discovery (UDP Beacon)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (isBeaconActive) "Broadcasting & scanning for nearby devices" else "Find nearby phones on the same network automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isBeaconActive,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    hotspotManager.startBeaconDiscovery(userNickname)
                                } else {
                                    hotspotManager.stopBeaconDiscovery()
                                }
                            },
                            modifier = Modifier.testTag("switch_hotspot_beacon")
                        )
                    }
                }
            }
        }

        // Auto Discovered Peers section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Auto-Discovered Devices (${discoveredPeers.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                if (discoveredPeers.isNotEmpty()) {
                    TextButton(onClick = { hotspotManager.clearDiscoveredPeers() }) {
                        Text("Clear", fontSize = 11.sp)
                    }
                }
            }
        }

        if (discoveredPeers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No devices detected automatically yet",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure 'Auto-Discovery' is ON on both phones, or use the direct IP connect below.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(discoveredPeers, key = { it.id }) { peer ->
                HotspotPeerCard(
                    peer = peer,
                    connectionState = connectionState,
                    onConnect = {
                        hotspotManager.connectToHost(peer.address, HotspotChatManager.TCP_PORT, peer.name)
                    }
                )
            }
        }

        // Direct IP Connection Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Direct Connect by IP Address",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect directly by entering the IP of the phone hosting the chat server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Hotspot preset chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { manualIp = "192.168.43.1" },
                            label = { Text("Hotspot Host (192.168.43.1)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Filled.CellTower, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Peer IP Address") },
                            placeholder = { Text("e.g. 192.168.43.1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("input_manual_ip")
                        )

                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_manual_port")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val port = manualPort.toIntOrNull() ?: HotspotChatManager.TCP_PORT
                            hotspotManager.connectToHost(manualIp, port, "Hotspot Peer ($manualIp)")
                        },
                        enabled = manualIp.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_connect_manual_ip")
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect to IP")
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotPeerCard(
    peer: DiscoveredDevice,
    connectionState: ConnectionState,
    onConnect: () -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected &&
            connectionState.peerAddress == peer.address
    val isConnecting = connectionState is ConnectionState.Connecting &&
            connectionState.peerName.contains(peer.name)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hotspot_peer_card_${peer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiTethering,
                    contentDescription = null,
                    tint = HotspotColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = peer.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${peer.address} • Discovered via Beacon",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        fontSize = 11.sp
                    )
                }
            }

            if (isConnected) {
                Text(
                    text = "Connected",
                    color = HotspotColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                FilledTonalButton(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_connect_hotspot_${peer.id}")
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect", fontSize = 12.sp)
                }
            }
        }
    }
}
