package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.PermissionsBanner
import com.example.ui.theme.BluetoothColor
import com.example.ui.theme.HotspotColor
import com.example.ui.theme.MobileDataColor
import com.example.ui.theme.WifiDirectColor

@Composable
fun NetworkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSubTab by viewModel.activeNetworkTab.collectAsStateWithLifecycle()

    val tabs = listOf(
        Triple("Bluetooth", Icons.Filled.Bluetooth, BluetoothColor),
        Triple("Wi-Fi Direct", Icons.Filled.Wifi, WifiDirectColor),
        Triple("Hotspot LAN", Icons.Filled.WifiTethering, HotspotColor),
        Triple("Mobile Data (5G)", Icons.Filled.SignalCellularAlt, MobileDataColor)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("network_screen")
    ) {
        // Permissions banner if any permission is missing
        PermissionsBanner()

        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                if (selectedSubTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = tabs[selectedSubTab].third
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (title, icon, color) ->
                val selected = selectedSubTab == index
                Tab(
                    selected = selected,
                    onClick = { viewModel.activeNetworkTab.value = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("tab_network_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> BluetoothTab(viewModel = viewModel)
            1 -> WifiDirectTab(viewModel = viewModel)
            2 -> HotspotTab(viewModel = viewModel)
            3 -> MobileDataTab(viewModel = viewModel)
        }
    }
}
