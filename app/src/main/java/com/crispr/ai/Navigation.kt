package com.crispr.ai

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.crispr.ai.theme.OnSurfaceVariantDark
import com.crispr.ai.theme.PrimaryDark
import com.crispr.ai.theme.SurfaceElevatedDark
import com.crispr.ai.ui.chat.AgentsChatScreen
import com.crispr.ai.ui.chat.QuickChatScreen

enum class AppTab {
    QUICK_CHAT,
    AGENTS
}

@Composable
fun MainNavigation() {
    var currentTab by remember { mutableStateOf(AppTab.QUICK_CHAT) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = SurfaceElevatedDark) {
                NavigationBarItem(
                    selected = currentTab == AppTab.QUICK_CHAT,
                    onClick = { currentTab = AppTab.QUICK_CHAT },
                    icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = "Quick Chat") },
                    label = { Text("Quick Chat", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        unselectedIconColor = OnSurfaceVariantDark,
                        unselectedTextColor = OnSurfaceVariantDark,
                        indicatorColor = PrimaryDark.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.AGENTS,
                    onClick = { currentTab = AppTab.AGENTS },
                    icon = { Icon(imageVector = Icons.Default.Apps, contentDescription = "AI Agents") },
                    label = { Text("AI Agents", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        unselectedIconColor = OnSurfaceVariantDark,
                        unselectedTextColor = OnSurfaceVariantDark,
                        indicatorColor = PrimaryDark.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { paddingValues ->
        when (currentTab) {
            AppTab.QUICK_CHAT -> {
                QuickChatScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .safeDrawingPadding()
                )
            }
            AppTab.AGENTS -> {
                AgentsChatScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .safeDrawingPadding()
                )
            }
        }
    }
}
