package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.TransportType
import com.example.ui.theme.BluetoothColor
import com.example.ui.theme.HotspotColor
import com.example.ui.theme.LiquidBlobAmber
import com.example.ui.theme.LiquidBlobBlue
import com.example.ui.theme.LiquidBlobCyan
import com.example.ui.theme.LiquidBlobPink
import com.example.ui.theme.LiquidBlobPurple
import com.example.ui.theme.MobileDataColor
import com.example.ui.theme.WifiDirectColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated iOS "Liquid in Glass" background with undulating, glowing liquid orbs
 * drifting under a frosted acrylic glass backdrop.
 */
@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val transition = rememberInfiniteTransition(label = "liquid_transition")

    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t1"
    )

    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t2"
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val baseBg = if (isDark) Color(0xFF090D16) else Color(0xFFF0F4FA)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        // Floating Fluid Liquid Blobs Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas

            // Blob 1: Electric Blue / Cyan
            val x1 = w * 0.3f + cos(t1.toDouble()).toFloat() * (w * 0.22f)
            val y1 = h * 0.25f + sin(t1.toDouble()).toFloat() * (h * 0.15f)
            val r1 = ((w * 0.55f) * pulseScale).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LiquidBlobBlue.copy(alpha = if (isDark) 0.45f else 0.35f),
                        LiquidBlobCyan.copy(alpha = if (isDark) 0.25f else 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(x1, y1),
                    radius = r1
                ),
                radius = r1,
                center = Offset(x1, y1)
            )

            // Blob 2: Vibrant Purple / Magenta
            val x2 = w * 0.75f + sin(t2.toDouble()).toFloat() * (w * 0.20f)
            val y2 = h * 0.65f + cos(t2.toDouble()).toFloat() * (h * 0.20f)
            val r2 = ((w * 0.60f) * pulseScale).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LiquidBlobPurple.copy(alpha = if (isDark) 0.42f else 0.30f),
                        LiquidBlobPink.copy(alpha = if (isDark) 0.22f else 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(x2, y2),
                    radius = r2
                ),
                radius = r2,
                center = Offset(x2, y2)
            )

            // Blob 3: Amber / Coral Accent
            val x3 = w * 0.5f + cos((t1 * 1.5f).toDouble()).toFloat() * (w * 0.25f)
            val y3 = h * 0.88f + sin((t2 * 1.2f).toDouble()).toFloat() * (h * 0.10f)
            val r3 = (w * 0.45f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LiquidBlobAmber.copy(alpha = if (isDark) 0.28f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(x3, y3),
                    radius = r3
                ),
                radius = r3,
                center = Offset(x3, y3)
            )
        }

        // Frosted Glass Acrylic Filter Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDark) {
                        Color(0xFF0D1322).copy(alpha = 0.65f)
                    } else {
                        Color(0xFFFFFFFF).copy(alpha = 0.55f)
                    }
                )
        )

        // Content
        content()
    }
}

/**
 * Modifier that applies iOS-like frosted liquid glass morphism with specular rim highlight.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = true,
    alpha: Float = if (isDark) 0.25f else 0.75f,
    borderAlpha: Float = if (isDark) 0.35f else 0.55f,
    elevation: Dp = 8.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0x33007AFF),
        spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0x22000000)
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF263248).copy(alpha = alpha + 0.12f),
                    Color(0xFF141C2B).copy(alpha = alpha)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = alpha),
                    Color(0xFFF1F5F9).copy(alpha = alpha - 0.1f)
                )
            }
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = borderAlpha * 0.3f),
                if (isDark) Color.White.copy(alpha = 0.05f) else Color(0x22000000)
            )
        ),
        shape = shape
    )
    .drawBehind {
        // Specular top highlight sheen
        val highlightHeight = 1.5.dp.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = if (isDark) 0.4f else 0.6f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(size.width * 0.15f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.7f, highlightHeight)
        )
    }

/**
 * Reusable Liquid Glass Card with iOS squircle shape and specular refraction.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.3f)),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .liquidGlass(shape = shape, isDark = isDark, elevation = elevation)
            .then(clickModifier),
        content = content
    )
}

/**
 * Transport Badge with iOS liquid pill styling.
 */
@Composable
fun LiquidTransportBadge(
    transport: TransportType,
    modifier: Modifier = Modifier
) {
    val (color, name) = when (transport) {
        TransportType.BLUETOOTH -> Pair(BluetoothColor, "Bluetooth")
        TransportType.WIFI_DIRECT -> Pair(WifiDirectColor, "Wi-Fi Direct")
        TransportType.HOTSPOT -> Pair(HotspotColor, "Hotspot LAN")
        TransportType.MOBILE_DATA -> Pair(MobileDataColor, "5G Long-Range")
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
    }
}
