package com.example.speaksmart.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Psychology

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
import com.example.speaksmart.data.ChatMessage
import com.example.speaksmart.data.MessageSender
import com.example.speaksmart.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Permission launcher for voice dictation
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    // Auto scroll list when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceDark,
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SurfaceDark,
                            SurfaceVariantDark,
                            SurfaceDark,
                        )
                    )
                ),
        ) {
            // Chat Top Bar
            ChatTopBar(
                isModelLoaded = uiState.isModelLoaded,
                onClearChat = { viewModel.clearChat() }
            )

            // Suggestion Chips
            SuggestionChipsRow(
                onChipSelected = { suggestion ->
                    viewModel.sendMessage(suggestion)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageItem(message = message)
                }
            }

            // Input Bar
            ChatInputBar(
                inputText = uiState.inputText,
                isGenerating = uiState.isGenerating,
                isListening = uiState.isListening,
                onTextChanged = { viewModel.updateInputText(it) },
                onSendClicked = { viewModel.sendMessage() },
                onMicClicked = {
                    if (uiState.isListening) {
                        viewModel.stopListening()
                    } else {
                        if (hasAudioPermission) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    isModelLoaded: Boolean,
    onClearChat: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryDark.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SpeakSmart AI Chat",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurfaceDark,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isModelLoaded) Icons.Outlined.CloudQueue else Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = if (isModelLoaded) SuccessGreen else CorrectionSectionColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isModelLoaded) "On-Device Gemma 2B LLM" else "Rule AI Tutor Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantDark,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        IconButton(onClick = onClearChat) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear Chat",
                tint = OnSurfaceVariantDark
            )
        }
    }
}

@Composable
private fun SuggestionChipsRow(
    onChipSelected: (String) -> Unit
) {
    val suggestions = listOf(
        "💡 Explain 'affect' vs 'effect'",
        "🎓 Give me a 3-question English quiz",
        "💼 Practice job interview questions",
        "📚 Explain Present Perfect Tense",
        "✨ Give me 3 synonyms for 'excellent'"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { text ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onChipSelected(text.substringAfter(" ")) },
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
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
                    .background(CorrectionSectionColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CorrectionSectionColor,
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
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        brush = if (isUser) {
                            Brush.linearGradient(
                                colors = listOf(PrimaryDark, PrimaryLight)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(SurfaceCardDark, SurfaceElevatedDark)
                            )
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.isPending) {
                    TypingDotsIndicator()
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceDark,
                        lineHeight = 22.sp,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceMutedDark,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun TypingDotsIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = CorrectionSectionColor,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "SpeakSmart AI is thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = CorrectionSectionColor.copy(alpha = alpha),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isGenerating: Boolean,
    isListening: Boolean,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onMicClicked: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceElevatedDark,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice input button
            IconButton(
                onClick = onMicClicked,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isListening) MicButtonPressedStart.copy(alpha = 0.2f) else SurfaceCardDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Dictation",
                    tint = if (isListening) MicButtonPressedStart else PrimaryDark,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text input field
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = if (isListening) "Listening..." else "Ask SpeakSmart AI anything...",
                        color = OnSurfaceMutedDark,
                        fontSize = 14.sp
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
                maxLines = 4,
                singleLine = false
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            IconButton(
                onClick = onSendClicked,
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (inputText.isNotBlank() && !isGenerating) {
                            Brush.linearGradient(listOf(PrimaryDark, PrimaryLight))
                        } else {
                            Brush.linearGradient(listOf(SurfaceCardDark, SurfaceCardDark))
                        }
                    )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = OnSurfaceDark,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) OnSurfaceDark else OnSurfaceMutedDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
