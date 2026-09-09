package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.ConnectionState
import com.example.network.DiscoveredDevice
import com.example.ui.MainViewModel
import com.example.ui.theme.WifiDirectColor

@Composable
fun WifiDirectTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val p2pManager = viewModel.connectionManager.wifiDirectManager

    val isP2pEnabled by p2pManager.isWifiP2pEnabled.collectAsStateWithLifecycle()
    val isDiscovering by p2pManager.isDiscovering.collectAsStateWithLifecycle()
    val peers by p2pManager.discoveredPeers.collectAsStateWithLifecycle()
    val thisDevice by p2pManager.thisDevice.collectAsStateWithLifecycle()
    val connectionInfo by p2pManager.connectionInfo.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        p2pManager.register()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("wifi_direct_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Control Card
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
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = WifiDirectColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wi-Fi Direct (P2P)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = if (isP2pEnabled) "Active" else "Checking/Standby",
                            color = if (isP2pEnabled) WifiDirectColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    thisDevice?.let { dev ->
                        Text(
                            text = "My P2P Device: ${dev.deviceName.ifBlank { "Android Device" }} (${dev.deviceAddress})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "High-speed peer-to-peer connection without an external internet or Wi-Fi router.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons: Discover & Create Group
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isDiscovering) {
                                    p2pManager.stopPeerDiscovery()
                                } else {
                                    p2pManager.startPeerDiscovery()
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("btn_p2p_discover"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isDiscovering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discovering...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Filled.WifiFind, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discover Peers", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { p2pManager.createGroup() },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("btn_p2p_create_group"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Act as Host", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Instructions Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "💡 How to Connect with Wi-Fi Direct:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = WifiDirectColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Ensure Wi-Fi is ON on both devices.\n2. Tap 'Discover Peers' on both phones.\n3. Tap 'Connect' on the found device and accept the prompt.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Peers Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Discovered Wi-Fi Direct Peers (${peers.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                FilledTonalButton(
                    onClick = { p2pManager.startPeerDiscovery() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("btn_p2p_refresh")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search", fontSize = 11.sp)
                }
            }
        }

        if (peers.isEmpty()) {
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Wi-Fi Direct peers found yet",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Make sure both devices have Wi-Fi turned ON and have tapped 'Discover Peers'.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(peers, key = { it.id }) { peer ->
                WifiDirectPeerCard(
                    peer = peer,
                    connectionState = connectionState,
                    onConnect = {
                        p2pManager.connectToPeer(peer.address, peer.name)
                    }
                )
            }
        }
    }
}

@Composable
private fun WifiDirectPeerCard(
    peer: DiscoveredDevice,
    connectionState: ConnectionState,
    onConnect: () -> Unit
) {
    val isConnected = connectionState is ConnectionState.Connected &&
            (connectionState.peerAddress == peer.address || connectionState.peerName.contains(peer.name))
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
            .testTag("p2p_peer_card_${peer.id}")
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
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = WifiDirectColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = peer.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "${peer.address} • ${peer.statusDescription}",
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
                    color = WifiDirectColor,
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
                    modifier = Modifier.testTag("btn_connect_p2p_${peer.id}")
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect", fontSize = 12.sp)
                }
            }
        }
    }
}
