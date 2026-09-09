package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.NearbyUser
import com.example.network.TransportType
import com.example.ui.MainViewModel
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidTransportBadge
import com.example.ui.components.NearbyRadarView
import com.example.ui.theme.BluetoothColor
import com.example.ui.theme.HotspotColor
import com.example.ui.theme.LiquidBlobBlue
import com.example.ui.theme.LiquidBlobCyan
import com.example.ui.theme.MobileDataColor
import com.example.ui.theme.WifiDirectColor

@Composable
fun NearbyScreen(
    viewModel: MainViewModel,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val myUsername by viewModel.userNickname.collectAsStateWithLifecycle()
    val detectedUsers by viewModel.detectedNearbyUsers.collectAsStateWithLifecycle()
    val isScanning by viewModel.isRadarScanning.collectAsStateWithLifecycle()
    val isMobileDataMode by viewModel.isMobileDataMode.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf<TransportType?>(null) }

    val filteredUsers = remember(detectedUsers, selectedFilter) {
        if (selectedFilter == null) detectedUsers
        else detectedUsers.filter { it.transport == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("nearby_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Glass Profile Banner
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // User Avatar Orb
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(LiquidBlobBlue, LiquidBlobCyan)
                                    )
                                )
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = myUsername.removePrefix("@").take(1).uppercase(),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = myUsername,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = onOpenProfile,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit username",
                                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isMobileDataMode) "Broadcasting Nearby & 5G" else "Broadcasting on Offline P2P",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Quick Random Username Roller
                    IconButton(
                        onClick = {
                            viewModel.setNickname(viewModel.generateRandomUsername())
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Random Handle",
                            tint = if (isDark) Color.White else Color(0xFF007AFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Animated Liquid Radar
        item {
            NearbyRadarView(
                myUsername = myUsername,
                detectedUsers = detectedUsers,
                isScanning = isScanning,
                onUserSelected = { user ->
                    viewModel.connectToUser(user)
                }
            )
        }

        // Radar Controls & Filter Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detected Nearby (${filteredUsers.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    ElevatedButton(
                        onClick = { viewModel.toggleRadarScan() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isScanning) Color(0xFFEF4444).copy(alpha = 0.85f) else Color(0xFF007AFF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_toggle_radar")
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isScanning) "Pause" else "Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Transport filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFF007AFF).copy(alpha = 0.2f)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == TransportType.BLUETOOTH,
                        onClick = {
                            selectedFilter = if (selectedFilter == TransportType.BLUETOOTH) null else TransportType.BLUETOOTH
                        },
                        label = { Text("Bluetooth", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = BluetoothColor, modifier = Modifier.size(14.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == TransportType.WIFI_DIRECT,
                        onClick = {
                            selectedFilter = if (selectedFilter == TransportType.WIFI_DIRECT) null else TransportType.WIFI_DIRECT
                        },
                        label = { Text("Wi-Fi Direct", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = WifiDirectColor, modifier = Modifier.size(14.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == TransportType.MOBILE_DATA,
                        onClick = {
                            selectedFilter = if (selectedFilter == TransportType.MOBILE_DATA) null else TransportType.MOBILE_DATA
                        },
                        label = { Text("5G", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = MobileDataColor, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        }

        // List of detected nearby users
        items(filteredUsers, key = { it.username }) { user ->
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { viewModel.connectToUser(user) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val transportColor = when (user.transport) {
                            TransportType.BLUETOOTH -> BluetoothColor
                            TransportType.WIFI_DIRECT -> WifiDirectColor
                            TransportType.HOTSPOT -> HotspotColor
                            TransportType.MOBILE_DATA -> MobileDataColor
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(transportColor, transportColor.copy(alpha = 0.6f))
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LiquidTransportBadge(transport = user.transport)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${user.proximityDescription} • ${user.statusText}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                            )
                        }
                    }

                    ElevatedButton(
                        onClick = { viewModel.connectToUser(user) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = when (user.transport) {
                                TransportType.BLUETOOTH -> BluetoothColor
                                TransportType.WIFI_DIRECT -> WifiDirectColor
                                TransportType.HOTSPOT -> HotspotColor
                                TransportType.MOBILE_DATA -> MobileDataColor
                            },
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
