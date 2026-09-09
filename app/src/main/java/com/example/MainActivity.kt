package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.LiquidBackground
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.NearbyScreen
import com.example.ui.screens.NetworkScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MobileDataColor
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                val activeTab by mainViewModel.activeMainTab.collectAsStateWithLifecycle()
                val userNickname by mainViewModel.userNickname.collectAsStateWithLifecycle()
                val isMobileDataMode by mainViewModel.isMobileDataMode.collectAsStateWithLifecycle()
                val isDark = isSystemInDarkTheme()

                var showSettings by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Animated Liquid Background
                    LiquidBackground(modifier = Modifier.fillMaxSize())

                    Scaffold(
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(id = R.string.app_name),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.5).sp
                                            ),
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                },
                                actions = {
                                    // User Handle Glass Pill
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isDark) Color.White.copy(alpha = 0.12f)
                                                else Color.White.copy(alpha = 0.70f)
                                            )
                                            .border(
                                                1.dp,
                                                Color.White.copy(alpha = if (isDark) 0.3f else 0.8f),
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AlternateEmail,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = userNickname.removePrefix("@"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )

                                        if (isMobileDataMode) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MobileDataColor)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { showSettings = true },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isDark) Color.White.copy(alpha = 0.10f)
                                                else Color.White.copy(alpha = 0.60f)
                                            )
                                            .testTag("btn_top_settings")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Settings",
                                            tint = if (isDark) Color.White else Color(0xFF0F172A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        },
                        bottomBar = {
                            // Frosted Liquid Glass Floating Bottom Bar
                            Box(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(28.dp),
                                        ambientColor = Color(0x30007AFF)
                                    )
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        if (isDark) Color(0xD01E293B) else Color(0xE0FFFFFF)
                                    )
                                    .border(
                                        1.5.dp,
                                        Color.White.copy(alpha = if (isDark) 0.35f else 0.85f),
                                        RoundedCornerShape(28.dp)
                                    )
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.height(68.dp)
                                ) {
                                    NavigationBarItem(
                                        selected = activeTab == 0,
                                        onClick = { mainViewModel.activeMainTab.value = 0 },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == 0) Icons.Filled.Radar else Icons.Outlined.Radar,
                                                contentDescription = "Nearby Radar",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Radar",
                                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = if (isDark) Color(0xFF007AFF).copy(alpha = 0.35f) else Color(0xFF007AFF).copy(alpha = 0.2f),
                                            selectedIconColor = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                                            selectedTextColor = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_radar")
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == 1,
                                        onClick = { mainViewModel.activeMainTab.value = 1 },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == 1) Icons.Filled.Chat else Icons.Outlined.Chat,
                                                contentDescription = "Chat",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Chat",
                                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = if (isDark) Color(0xFF007AFF).copy(alpha = 0.35f) else Color(0xFF007AFF).copy(alpha = 0.2f),
                                            selectedIconColor = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                                            selectedTextColor = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_chat")
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == 2,
                                        onClick = { mainViewModel.activeMainTab.value = 2 },
                                        icon = {
                                            Icon(
                                                imageVector = if (activeTab == 2) Icons.Filled.Hub else Icons.Outlined.Hub,
                                                contentDescription = "Transports",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Transports",
                                                fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = if (isDark) Color(0xFF007AFF).copy(alpha = 0.35f) else Color(0xFF007AFF).copy(alpha = 0.2f),
                                            selectedIconColor = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF),
                                            selectedTextColor = if (isDark) Color(0xFF64D2FF) else Color(0xFF007AFF)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_network")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeTab) {
                                0 -> NearbyScreen(
                                    viewModel = mainViewModel,
                                    onOpenProfile = { showSettings = true }
                                )
                                1 -> ChatScreen(
                                    viewModel = mainViewModel
                                )
                                2 -> NetworkScreen(
                                    viewModel = mainViewModel
                                )
                            }
                        }
                    }

                    if (showSettings) {
                        SettingsDialog(
                            viewModel = mainViewModel,
                            onDismiss = { showSettings = false }
                        )
                    }
                }
            }
        }
    }
}
