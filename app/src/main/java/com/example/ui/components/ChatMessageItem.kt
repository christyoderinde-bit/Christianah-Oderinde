package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessageEntity
import com.example.network.TransportType
import com.example.ui.theme.BluetoothColor
import com.example.ui.theme.HotspotColor
import com.example.ui.theme.MobileDataColor
import com.example.ui.theme.WifiDirectColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    modifier: Modifier = Modifier
) {
    val isMe = message.isFromMe
    val isBuzz = message.isBuzz
    val isDark = isSystemInDarkTheme()

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val transport = try {
        TransportType.valueOf(message.transportType)
    } catch (_: Exception) {
        TransportType.HOTSPOT
    }

    val transportIcon = when (transport) {
        TransportType.BLUETOOTH -> Icons.Filled.Bluetooth
        TransportType.WIFI_DIRECT -> Icons.Filled.Wifi
        TransportType.HOTSPOT -> Icons.Filled.WifiTethering
        TransportType.MOBILE_DATA -> Icons.Filled.SignalCellularAlt
    }

    val transportColor = when (transport) {
        TransportType.BLUETOOTH -> BluetoothColor
        TransportType.WIFI_DIRECT -> WifiDirectColor
        TransportType.HOTSPOT -> HotspotColor
        TransportType.MOBILE_DATA -> MobileDataColor
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    val bubbleBrush = when {
        isBuzz -> Brush.linearGradient(
            listOf(
                Color(0xFFFFB300).copy(alpha = 0.35f),
                Color(0xFFFFA000).copy(alpha = 0.20f)
            )
        )
        isMe -> Brush.linearGradient(
            listOf(
                Color(0xFF007AFF).copy(alpha = 0.88f),
                Color(0xFF5856D6).copy(alpha = 0.85f)
            )
        )
        else -> Brush.linearGradient(
            if (isDark) {
                listOf(
                    Color(0xFF1E293B).copy(alpha = 0.55f),
                    Color(0xFF0F172A).copy(alpha = 0.45f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.82f),
                    Color(0xFFF1F5F9).copy(alpha = 0.72f)
                )
            }
        )
    }

    val borderBrush = when {
        isBuzz -> Brush.linearGradient(
            listOf(Color(0xFFFFD54F), Color(0xFFFFB300).copy(alpha = 0.4f))
        )
        isMe -> Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.70f),
                Color.White.copy(alpha = 0.25f),
                Color.Transparent
            )
        )
        else -> Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) 0.45f else 0.85f),
                Color.White.copy(alpha = if (isDark) 0.15f else 0.40f)
            )
        )
    }

    val textColor = when {
        isBuzz -> if (isDark) Color(0xFFFFE082) else Color(0xFFB45309)
        isMe -> Color.White
        else -> if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .testTag("chat_message_item_${message.id}"),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 90.dp, max = 310.dp)
                .shadow(
                    elevation = if (isMe) 5.dp else 3.dp,
                    shape = bubbleShape,
                    ambientColor = if (isMe) Color(0x40007AFF) else Color(0x20000000)
                )
                .clip(bubbleShape)
                .background(bubbleBrush)
                .border(BorderStroke(1.dp, borderBrush), bubbleShape)
                .drawBehind {
                    // Subtle specular top highlight
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isMe) 0.45f else 0.25f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(size.width * 0.15f, 0f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.7f, 1.2.dp.toPx())
                    )
                }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Column {
                // Sender label & transport badge for incoming messages
                if (!isMe) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF)
                            )
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = transportIcon,
                                contentDescription = transport.displayName,
                                tint = transportColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = transport.displayName,
                                fontSize = 9.sp,
                                color = transportColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (isBuzz) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Buzz",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                    }
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }

                // Timestamp row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMe) {
                        Icon(
                            imageVector = transportIcon,
                            contentDescription = transport.displayName,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.75f) else textColor.copy(alpha = 0.60f)
                    )
                }
            }
        }
    }
}

