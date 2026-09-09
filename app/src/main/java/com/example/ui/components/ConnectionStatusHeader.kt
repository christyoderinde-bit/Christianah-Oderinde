package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ConnectionState
import com.example.network.TransportType
import com.example.ui.theme.BluetoothColor
import com.example.ui.theme.HotspotColor
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusDisconnected
import com.example.ui.theme.StatusError
import com.example.ui.theme.WifiDirectColor

@Composable
fun ConnectionStatusHeader(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    val (bgColor, statusColor, titleText, subtitleText, transportIcon) = when (connectionState) {
        is ConnectionState.Connected -> {
            val icon = when (connectionState.transport) {
                TransportType.BLUETOOTH -> Icons.Filled.Bluetooth
                TransportType.WIFI_DIRECT -> Icons.Filled.Wifi
                TransportType.HOTSPOT -> Icons.Filled.WifiTethering
                TransportType.MOBILE_DATA -> androidx.compose.material.icons.Icons.Filled.SignalCellularAlt
            }
            Tuple5(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                StatusConnected,
                "Connected: ${connectionState.peerName}",
                "Via ${connectionState.transport.displayName} • ${connectionState.peerAddress}",
                icon
            )
        }
        is ConnectionState.Connecting -> {
            Tuple5(
                MaterialTheme.colorScheme.surfaceVariant,
                StatusConnecting,
                "Connecting to ${connectionState.peerName}...",
                "Initiating ${connectionState.transport.displayName} handshake",
                Icons.Filled.Wifi
            )
        }
        is ConnectionState.Listening -> {
            Tuple5(
                MaterialTheme.colorScheme.surfaceVariant,
                StatusConnecting,
                "Listening for Peers",
                connectionState.details,
                Icons.Filled.WifiTethering
            )
        }
        is ConnectionState.Error -> {
            Tuple5(
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                StatusError,
                "Connection Issue",
                connectionState.message,
                Icons.Filled.LinkOff
            )
        }
        is ConnectionState.Disconnected -> {
            Tuple5(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                StatusDisconnected,
                "No Active Peer",
                "Ready for Bluetooth, Wi-Fi Direct, or Hotspot",
                Icons.Filled.LinkOff
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_status_header"),
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action buttons
                when (connectionState) {
                    is ConnectionState.Connected -> {
                        FilledTonalButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_header_disconnect")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Disconnect",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }
                    is ConnectionState.Connecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {
                        TextButton(
                            onClick = onNavigateToNetwork,
                            modifier = Modifier.testTag("btn_header_connect")
                        ) {
                            Text("Connect", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
