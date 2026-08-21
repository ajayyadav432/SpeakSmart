package com.crispr.ai.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.crispr.ai.data.UserProfileStore
import com.crispr.ai.theme.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(UserProfileStore.getDisplayName(context)) }
    var avatarUri by remember { mutableStateOf(UserProfileStore.getAvatarUri(context)) }
    var totalChats by remember { mutableStateOf(UserProfileStore.getTotalChats(context)) }
    var selectedTheme by remember { mutableStateOf(UserProfileStore.getTheme(context)) }
    var selectedLanguage by remember { mutableStateOf(UserProfileStore.getLanguage(context)) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUri = it.toString()
            UserProfileStore.setAvatarUri(context, it.toString())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurfaceDark)
                }
                Text(
                    text = "My Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurfaceDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Avatar + Name hero block ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SurfaceVariantDark, SurfaceDark)
                        )
                    )
                    .padding(vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(PrimaryDark.copy(alpha = 0.15f))
                            .border(2.5.dp, PrimaryDark, CircleShape)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = Uri.parse(avatarUri),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = PrimaryDark,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        // Camera badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF001412), modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    Text(
                        text = if (displayName.isNotBlank()) displayName else "Tap to set your name",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (displayName.isNotBlank()) OnSurfaceDark else OnSurfaceVariantDark,
                        modifier = Modifier.clickable { showNameDialog = true },
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Privacy badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(PrimaryDark.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "100% Private · No Cloud", fontSize = 12.sp, color = PrimaryDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Stats Row ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(modifier = Modifier.weight(1f), number = totalChats.toString(), label = "AI Chats", color = PrimaryDark)
                StatCard(modifier = Modifier.weight(1f), number = "7", label = "Agents", color = AgentPurple)
                StatCard(modifier = Modifier.weight(1f), number = "0", label = "Data Sent", color = AgentGreen)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Settings Section ──────────────────────────
            SectionHeader(title = "Settings")

            SettingRow(
                icon = Icons.Default.ColorLens,
                iconColor = AgentPurple,
                title = "App Theme",
                subtitle = selectedTheme,
                onClick = {
                    selectedTheme = if (selectedTheme == "Dark (Forest)") "Dark (Midnight)" else "Dark (Forest)"
                    UserProfileStore.setTheme(context, selectedTheme)
                }
            )

            SettingRow(
                icon = Icons.Default.Language,
                iconColor = AgentBlue,
                title = "Language",
                subtitle = selectedLanguage,
                onClick = {
                    selectedLanguage = if (selectedLanguage == "English") "Hindi" else "English"
                    UserProfileStore.setLanguage(context, selectedLanguage)
                }
            )

            SettingRow(
                icon = Icons.Default.Edit,
                iconColor = PrimaryDark,
                title = "Edit Name",
                subtitle = if (displayName.isNotBlank()) displayName else "Not set",
                onClick = { showNameDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Privacy & Data Section ────────────────────
            SectionHeader(title = "Privacy & Data")

            SettingRow(
                icon = Icons.Default.Shield,
                iconColor = AgentGreen,
                title = "Privacy Policy",
                subtitle = "How Crispr AI protects your data",
                onClick = { showPrivacyDialog = true }
            )

            SettingRow(
                icon = Icons.Default.Storage,
                iconColor = AgentAmber,
                title = "Data Storage",
                subtitle = "All data stored on your device only",
                onClick = {}
            )

            SettingRow(
                icon = Icons.Default.DeleteForever,
                iconColor = ErrorRed,
                title = "Clear All Data",
                subtitle = "Erase chat history and profile",
                onClick = {
                    UserProfileStore.setDisplayName(context, "")
                    UserProfileStore.setAvatarUri(context, "")
                    displayName = ""
                    avatarUri = ""
                    totalChats = 0
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App version
            Text(
                text = "Crispr AI v1.0  •  Made with ❤️ for Privacy",
                fontSize = 12.sp,
                color = OnSurfaceMutedDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── Name Dialog ──────────────────────────────────────
    if (showNameDialog) {
        var draftName by remember { mutableStateOf(displayName) }
        Dialog(onDismissRequest = { showNameDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceElevatedDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text("What's your name?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OnSurfaceDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        placeholder = { Text("Enter your name", color = OnSurfaceMutedDark) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDark,
                            unfocusedBorderColor = OnSurfaceMutedDark,
                            focusedContainerColor = SurfaceCardDark,
                            unfocusedContainerColor = SurfaceCardDark,
                            focusedTextColor = OnSurfaceDark,
                            unfocusedTextColor = OnSurfaceDark
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (draftName.isNotBlank()) {
                                displayName = draftName.trim()
                                UserProfileStore.setDisplayName(context, displayName)
                                UserProfileStore.markSetupDone(context)
                            }
                            showNameDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                    ) {
                        Text("Save", color = Color(0xFF001412), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }

    // ── Privacy Dialog ────────────────────────────────────
    if (showPrivacyDialog) {
        Dialog(onDismissRequest = { showPrivacyDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceElevatedDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    Text("🔒 Our Privacy Promise", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    val items = listOf(
                        "✅ All AI processing happens on your phone — zero internet used.",
                        "✅ Your conversations are never saved to disk or any cloud.",
                        "✅ No analytics, no trackers, no third-party SDKs.",
                        "✅ Your name and avatar are stored only in your phone's local storage.",
                        "✅ Uninstalling the app permanently deletes all data.",
                        "✅ We have no server. There is nothing to hack."
                    )
                    items.forEach { item ->
                        Text(item, fontSize = 13.sp, color = OnSurfaceVariantDark, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 5.dp), lineHeight = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showPrivacyDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                    ) {
                        Text("Got it!", color = Color(0xFF001412), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, number: String, label: String, color: Color) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .border(1.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = number, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 12.sp, color = OnSurfaceVariantDark, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = OnSurfaceMutedDark,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurfaceDark)
            Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariantDark, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceMutedDark, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = SurfaceVariantDark, thickness = 0.8.dp)
}

// End of ProfileScreen
