package com.crispr.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crispr.ai.data.UserProfileStore
import com.crispr.ai.theme.*
import com.crispr.ai.ui.chat.AgentsChatScreen
import com.crispr.ai.ui.chat.QuickChatScreen
import com.crispr.ai.ui.profile.ProfileScreen

enum class AppTab { QUICK_CHAT, AGENTS, PROFILE }

@Composable
fun MainNavigation() {
    var currentTab by remember { mutableStateOf(AppTab.QUICK_CHAT) }
    val context = LocalContext.current

    // Show name setup dialog on very first launch
    var showFirstLaunch by remember { mutableStateOf(!UserProfileStore.isSetupDone(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceNavDark,
                tonalElevation = 0.dp,
                modifier = Modifier.shadow(elevation = 16.dp)
            ) {
                // Quick Chat tab
                NavigationBarItem(
                    selected = currentTab == AppTab.QUICK_CHAT,
                    onClick = { currentTab = AppTab.QUICK_CHAT },
                    icon = {
                        NavIcon(
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Quick Chat", modifier = Modifier.size(22.dp)) },
                            selected = currentTab == AppTab.QUICK_CHAT
                        )
                    },
                    label = {
                        Text(
                            "Quick Chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    colors = navItemColors()
                )
                // AI Agents tab
                NavigationBarItem(
                    selected = currentTab == AppTab.AGENTS,
                    onClick = { currentTab = AppTab.AGENTS },
                    icon = {
                        NavIcon(
                            icon = { Icon(Icons.Default.Apps, contentDescription = "AI Agents", modifier = Modifier.size(22.dp)) },
                            selected = currentTab == AppTab.AGENTS
                        )
                    },
                    label = {
                        Text(
                            "AI Agents",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    colors = navItemColors()
                )
                // Profile tab
                NavigationBarItem(
                    selected = currentTab == AppTab.PROFILE,
                    onClick = { currentTab = AppTab.PROFILE },
                    icon = {
                        NavIcon(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(22.dp)) },
                            selected = currentTab == AppTab.PROFILE
                        )
                    },
                    label = {
                        Text(
                            "Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    colors = navItemColors()
                )
            }
        }
    ) { paddingValues ->
        when (currentTab) {
            AppTab.QUICK_CHAT -> QuickChatScreen(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
            AppTab.AGENTS -> AgentsChatScreen(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
            AppTab.PROFILE -> ProfileScreen(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                onBack = { currentTab = AppTab.QUICK_CHAT }
            )
        }
    }
}

@Composable
private fun NavIcon(
    icon: @Composable () -> Unit,
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .size(if (selected) 46.dp else 38.dp)
            .clip(CircleShape)
            .background(
                if (selected)
                    Brush.radialGradient(listOf(PrimaryDark.copy(0.28f), PrimaryDark.copy(0.06f)))
                else
                    Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor   = PrimaryDark,
    selectedTextColor   = PrimaryDark,
    unselectedIconColor = OnSurfaceVariantDark,
    unselectedTextColor = OnSurfaceVariantDark,
    indicatorColor      = Color.Transparent
)
