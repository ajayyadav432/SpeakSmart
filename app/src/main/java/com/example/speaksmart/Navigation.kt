package com.example.speaksmart

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
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
import com.example.speaksmart.theme.OnSurfaceVariantDark
import com.example.speaksmart.theme.PrimaryDark
import com.example.speaksmart.theme.SurfaceElevatedDark
import com.example.speaksmart.ui.chat.ChatScreen
import com.example.speaksmart.ui.main.MainScreen

enum class AppTab {
    VOICE_TUTOR,
    AI_CHAT
}

@Composable
fun MainNavigation() {
    var currentTab by remember { mutableStateOf(AppTab.VOICE_TUTOR) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceElevatedDark,
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.VOICE_TUTOR,
                    onClick = { currentTab = AppTab.VOICE_TUTOR },
                    icon = { Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Tutor") },
                    label = { Text("Voice Tutor", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        unselectedIconColor = OnSurfaceVariantDark,
                        unselectedTextColor = OnSurfaceVariantDark,
                        indicatorColor = PrimaryDark.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.AI_CHAT,
                    onClick = { currentTab = AppTab.AI_CHAT },
                    icon = { Icon(imageVector = Icons.Default.Forum, contentDescription = "AI Chat") },
                    label = { Text("AI Chat", fontWeight = FontWeight.SemiBold) },
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
            AppTab.VOICE_TUTOR -> {
                MainScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .safeDrawingPadding()
                )
            }
            AppTab.AI_CHAT -> {
                ChatScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .safeDrawingPadding()
                )
            }
        }
    }
}
