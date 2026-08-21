package com.crispr.ai.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crispr.ai.data.ChatMessage
import com.crispr.ai.data.MessageSender
import com.crispr.ai.theme.MicButtonPressedStart
import com.crispr.ai.theme.OnSurfaceDark
import com.crispr.ai.theme.OnSurfaceMutedDark
import com.crispr.ai.theme.OnSurfaceVariantDark
import com.crispr.ai.theme.PrimaryDark
import com.crispr.ai.theme.PrimaryLight
import com.crispr.ai.theme.SurfaceCardDark
import com.crispr.ai.theme.SurfaceDark
import com.crispr.ai.theme.SurfaceElevatedDark
import com.crispr.ai.theme.SurfaceVariantDark
import com.crispr.ai.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_CHAT_SUGGESTIONS = listOf(
    "✍️ Help me write an email",
    "💡 Brainstorm ideas for me",
    "📋 Summarize a text",
    "🌐 What can you do?",
)

@Composable
fun QuickChatScreen(
    modifier: Modifier = Modifier,
    viewModel: QuickChatViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.startListening()
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceDark
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceDark, SurfaceVariantDark, SurfaceDark)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryDark.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quick Chat",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurfaceDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isModelLoaded) Icons.Outlined.CloudQueue else Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = if (uiState.isModelLoaded) SuccessGreen else PrimaryDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isModelLoaded) "On-Device LLM" else "Smart Mode • 100% Private",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariantDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = OnSurfaceVariantDark)
                    }
                }

                // Suggestion chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(QUICK_CHAT_SUGGESTIONS) { chip ->
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                            color = SurfaceElevatedDark,
                            shape = RoundedCornerShape(20.dp),
                            onClick = { viewModel.sendMessage(chip.substringAfter(" ")) }
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        QuickChatMessageBubble(message)
                    }
                }

                // Input bar
                QuickChatInputBar(
                    inputText = uiState.inputText,
                    isGenerating = uiState.isGenerating,
                    isListening = uiState.isListening,
                    onTextChanged = { viewModel.updateInputText(it) },
                    onSendClicked = { viewModel.sendMessage() },
                    onMicClicked = {
                        if (uiState.isListening) viewModel.stopListening()
                        else if (hasAudioPermission) viewModel.startListening()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickChatMessageBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryDark.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) Brush.linearGradient(listOf(PrimaryDark, PrimaryLight))
                        else Brush.linearGradient(listOf(SurfaceCardDark, SurfaceElevatedDark))
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.isPending) {
                    TypingIndicator(tint = PrimaryDark)
                } else {
                    Text(message.text, color = OnSurfaceDark, fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(formattedTime, color = OnSurfaceMutedDark, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TypingIndicator(tint: Color) {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = tint, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Thinking...", color = tint.copy(alpha = alpha), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    isListening: Boolean,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onMicClicked: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceElevatedDark, tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMicClicked,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isListening) MicButtonPressedStart.copy(0.2f) else SurfaceCardDark)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice Dictation",
                    tint = if (isListening) MicButtonPressedStart else PrimaryDark,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        if (isListening) "Listening..." else "Ask me anything...",
                        color = OnSurfaceMutedDark, fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = PrimaryDark,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDark
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSendClicked,
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isGenerating)
                            Brush.linearGradient(listOf(PrimaryDark, PrimaryLight))
                        else Brush.linearGradient(listOf(SurfaceCardDark, SurfaceCardDark))
                    )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OnSurfaceDark, strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send, contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) OnSurfaceDark else OnSurfaceMutedDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
