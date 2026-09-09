package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.NearbyUser
import com.example.network.TransportType
import com.example.ui.theme.BluetoothColor
import com.example.ui.theme.HotspotColor
import com.example.ui.theme.LiquidBlobBlue
import com.example.ui.theme.LiquidBlobCyan
import com.example.ui.theme.LiquidBlobPink
import com.example.ui.theme.MobileDataColor
import com.example.ui.theme.WifiDirectColor
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun NearbyRadarView(
    myUsername: String,
    detectedUsers: List<NearbyUser>,
    isScanning: Boolean,
    onUserSelected: (NearbyUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var selectedUser by remember { mutableStateOf<NearbyUser?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")

    // Continuous sweep angle
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    // Concentric expanding pulse wave
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nearby Radar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = if (isScanning) "Scanning via Bluetooth, Wi-Fi & 5G..." else "${detectedUsers.size} users detected in range",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isScanning) LiquidBlobCyan.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isScanning) LiquidBlobCyan.copy(alpha = 0.6f)
                        else Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isScanning) "SCANNING" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = if (isScanning) LiquidBlobCyan else if (isDark) Color.White else Color(0xFF334155)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Liquid Radar Canvas Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val radarSizePx = minOf(maxWidth.value, maxHeight.value)
            val centerOffset = Offset(
                x = maxWidth.value * 0.5f,
                y = maxHeight.value * 0.5f
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxRadius = minOf(cx, cy) * 0.92f
                if (maxRadius <= 1f) return@Canvas

                // Concentric glass rings
                val rings = listOf(0.3f, 0.6f, 0.9f)
                rings.forEach { ratio ->
                    val ringRadius = maxRadius * ratio
                    drawCircle(
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFF007AFF).copy(alpha = 0.12f),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Crosshairs
                drawLine(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    start = Offset(cx - maxRadius, cy),
                    end = Offset(cx + maxRadius, cy),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    start = Offset(cx, cy - maxRadius),
                    end = Offset(cx, cy + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )

                // Expanding pulse wave
                if (isScanning) {
                    val pulseRadius = maxRadius * pulseProgress
                    val pulseAlpha = (1f - pulseProgress) * 0.45f
                    if (pulseRadius > 1f && pulseAlpha > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    LiquidBlobBlue.copy(alpha = pulseAlpha),
                                    LiquidBlobCyan.copy(alpha = pulseAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = pulseRadius
                            ),
                            radius = pulseRadius,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = LiquidBlobCyan.copy(alpha = pulseAlpha),
                            radius = pulseRadius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Sweeping radar beam line
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val endX = cx + (maxRadius * cos(sweepRad)).toFloat()
                    val endY = cy + (maxRadius * sin(sweepRad)).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LiquidBlobBlue.copy(alpha = 0.9f),
                                LiquidBlobCyan.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            start = Offset(cx, cy),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(cx, cy),
                        end = Offset(endX, endY),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Center Node: My Device
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                LiquidBlobBlue,
                                Color(0xFF0044BB)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .shadow(8.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Me",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.White
                    )
                }
            }

            // Detected User Nodes positioned radially
            val radiusPx = (radarSizePx * 0.5f) * 0.78f
            detectedUsers.forEachIndexed { index, user ->
                // Calculate position using angle and distance
                val angleDeg = if (user.radarAngle != 0f) user.radarAngle else (index * 72f + 35f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dist = user.radarDistance.coerceIn(0.28f, 0.88f)
                val offsetX = (radiusPx * dist * cos(angleRad)).toFloat()
                val offsetY = (radiusPx * dist * sin(angleRad)).toFloat()

                val transportColor = when (user.transport) {
                    TransportType.BLUETOOTH -> BluetoothColor
                    TransportType.WIFI_DIRECT -> WifiDirectColor
                    TransportType.HOTSPOT -> HotspotColor
                    TransportType.MOBILE_DATA -> MobileDataColor
                }

                val isSelected = selectedUser?.username == user.username

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .size(if (isSelected) 56.dp else 46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    transportColor.copy(alpha = 0.9f),
                                    transportColor.copy(alpha = 0.5f)
                                )
                            )
                        )
                        .border(
                            width = if (isSelected) 2.5.dp else 1.5.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .clickable {
                            selectedUser = user
                            onUserSelected(user)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = user.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Icon(
                            imageVector = when (user.transport) {
                                TransportType.BLUETOOTH -> Icons.Default.Bluetooth
                                TransportType.WIFI_DIRECT -> Icons.Default.Wifi
                                TransportType.HOTSPOT -> Icons.Default.WifiTethering
                                TransportType.MOBILE_DATA -> Icons.Default.SignalCellularAlt
                            },
                            contentDescription = user.transport.displayName,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        // Details Popup Card when a user is selected
        selectedUser?.let { user ->
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.username,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LiquidTransportBadge(transport = user.transport)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${user.proximityDescription} • Signal ${user.signalStrength}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                        )
                    }

                    ElevatedButton(
                        onClick = { onUserSelected(user) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = when (user.transport) {
                                TransportType.BLUETOOTH -> BluetoothColor
                                TransportType.WIFI_DIRECT -> WifiDirectColor
                                TransportType.HOTSPOT -> HotspotColor
                                TransportType.MOBILE_DATA -> MobileDataColor
                            },
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
