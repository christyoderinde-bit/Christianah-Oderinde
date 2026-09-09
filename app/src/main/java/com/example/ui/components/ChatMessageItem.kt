package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ChatMessageEntity
import com.example.network.TransportType
import com.example.ui.theme.BluetoothColor
import com.example.ui.theme.HotspotColor
import com.example.ui.theme.MobileDataColor
import com.example.ui.theme.WifiDirectColor
import com.example.util.FileHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onOpenFile: (ChatMessageEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMe = message.isFromMe
    val isBuzz = message.isBuzz
    val isFile = message.isFile
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
                } else if (isFile) {
                    // File / Image Attachment layout
                    val isImage = (message.fileMimeType?.startsWith("image/") == true) ||
                            (message.fileName?.matches(Regex("(?i).*\\.(png|jpe?g|webp|gif)$")) == true)

                    if (isImage) {
                        val imageModel = message.filePath?.let { File(it) } ?: message.fileUrl
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .clickable { onOpenFile(message) }
                        ) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = message.fileName ?: "Photo attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // File info row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMe) Color.White.copy(alpha = 0.16f) else Color(0x15007AFF))
                            .border(1.dp, Color.White.copy(alpha = if (isMe) 0.30f else 0.18f), RoundedCornerShape(12.dp))
                            .clickable { onOpenFile(message) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMe) Color.White.copy(alpha = 0.22f) else Color(0xFF007AFF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveFileIcon(message.fileName, message.fileMimeType),
                                contentDescription = null,
                                tint = if (isMe) Color.White else Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.fileName ?: "Attachment",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = FileHelper.formatFileSize(message.fileSize),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isMe) Color.White.copy(alpha = 0.8f) else textColor.copy(alpha = 0.65f),
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (message.filePath != null) Icons.Default.OpenInNew else Icons.Default.Download,
                            contentDescription = "Open file",
                            tint = if (isMe) Color.White.copy(alpha = 0.85f) else Color(0xFF007AFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
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

private fun resolveFileIcon(fileName: String?, mimeType: String?): ImageVector {
    val name = fileName?.lowercase() ?: ""
    val mime = mimeType?.lowercase() ?: ""

    return when {
        mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif") -> Icons.Filled.Image
        mime == "application/pdf" || name.endsWith(".pdf") -> Icons.Filled.PictureAsPdf
        mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") -> Icons.Filled.AudioFile
        mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv") -> Icons.Filled.VideoFile
        name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") || name.endsWith(".tar") -> Icons.Filled.FolderZip
        mime.startsWith("text/") || name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx") -> Icons.Filled.Description
        else -> Icons.Filled.InsertDriveFile
    }
}

