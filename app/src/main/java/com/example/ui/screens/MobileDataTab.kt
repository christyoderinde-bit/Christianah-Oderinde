package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.NearbyUser
import com.example.network.TransportType
import com.example.ui.MainViewModel
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.MobileDataColor

@Composable
fun MobileDataTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isMobileDataActive by viewModel.isMobileDataMode.collectAsStateWithLifecycle()
    val myUsername by viewModel.userNickname.collectAsStateWithLifecycle()
    val isConnected by viewModel.connectionManager.mobileDataManager.isConnected.collectAsStateWithLifecycle()
    val isCellular = remember { viewModel.connectionManager.mobileDataManager.isUsingCellular() }

    val allDetectedUsers by viewModel.detectedNearbyUsers.collectAsStateWithLifecycle()
    val mobileDataUsers = remember(allDetectedUsers) {
        allDetectedUsers.filter { it.transport == TransportType.MOBILE_DATA }
    }

    var targetUsernameInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("mobile_data_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card: Long Range Cellular Mode
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MobileDataColor, Color(0xFFFF9500))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularAlt,
                                    contentDescription = "Cellular",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Mobile Data / 5G Relay",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (isCellular) "Cellular network detected" else "Internet relay active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                                )
                            }
                        }

                        Switch(
                            checked = isMobileDataActive,
                            onCheckedChange = { viewModel.toggleMobileDataMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MobileDataColor
                            ),
                            modifier = Modifier.testTag("switch_mobile_data_mode")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isConnected) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .border(
                                1.dp,
                                if (isConnected) Color(0xFF10B981).copy(alpha = 0.4f)
                                else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.CellTower,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF10B981) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConnected) "Relay Online: Global Mesh Active" else "Enable toggle to connect over cellular",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (isConnected) Color(0xFF10B981) else if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // Direct Connect by Username Section
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Direct Chat by @Username",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect with any user anywhere across the world via their username.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetUsernameInput,
                            onValueChange = { targetUsernameInput = it },
                            placeholder = { Text("@username", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (targetUsernameInput.isNotBlank()) {
                                        val cleanHandle = if (targetUsernameInput.startsWith("@")) targetUsernameInput else "@$targetUsernameInput"
                                        val user = NearbyUser(
                                            username = cleanHandle,
                                            displayName = cleanHandle.removePrefix("@"),
                                            transport = TransportType.MOBILE_DATA,
                                            address = "cell:$cleanHandle",
                                            proximityDescription = "Long-Range (Mobile Data)",
                                            signalStrength = 95,
                                            isOnline = true,
                                            statusText = "Connected via 5G"
                                        )
                                        viewModel.connectToUser(user)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_target_username")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ElevatedButton(
                            onClick = {
                                if (targetUsernameInput.isNotBlank()) {
                                    val cleanHandle = if (targetUsernameInput.startsWith("@")) targetUsernameInput else "@$targetUsernameInput"
                                    val user = NearbyUser(
                                        username = cleanHandle,
                                        displayName = cleanHandle.removePrefix("@"),
                                        transport = TransportType.MOBILE_DATA,
                                        address = "cell:$cleanHandle",
                                        proximityDescription = "Long-Range (Mobile Data)",
                                        signalStrength = 95,
                                        isOnline = true,
                                        statusText = "Connected via 5G"
                                    )
                                    viewModel.connectToUser(user)
                                }
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MobileDataColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("btn_connect_by_username")
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Long Range Users Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active on Mobile Data (${mobileDataUsers.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                IconButton(
                    onClick = {
                        viewModel.connectionManager.mobileDataManager.broadcastPresence()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MobileDataColor
                    )
                }
            }
        }

        // List of long range users
        items(mobileDataUsers, key = { it.username }) { user ->
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth()
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
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(MobileDataColor, Color(0xFFFF2D55))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = user.username,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "${user.statusText} • Signal ${user.signalStrength}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                            )
                        }
                    }

                    ElevatedButton(
                        onClick = { viewModel.connectToUser(user) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MobileDataColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
