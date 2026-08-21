package com.crispr.ai.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crispr.ai.llm.DownloadStatus
import com.crispr.ai.llm.ModelDownloader
import com.crispr.ai.theme.*

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Permission handling
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
        if (!isGranted) {
            viewModel.clearError()
        }
    }

    // Show error as snackbar
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar
            TopBar(
                isModelLoaded = uiState.isModelLoaded,
                onClear = { viewModel.clearSession() }
            )

            // Main content - scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Show model download card if model is not loaded yet
                if (!uiState.isModelLoaded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ModelDownloadCard(
                        downloadStatus = uiState.downloadStatus,
                        onStartDownload = { customUrl ->
                            viewModel.startModelDownload(customUrl)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Microphone button area
                MicrophoneButton(
                    isListening = uiState.isListening,
                    rmsLevel = uiState.rmsLevel,
                    onPressStart = {
                        if (hasAudioPermission) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onPressEnd = {
                        viewModel.stopListening()
                    }
                )

                // Status text below mic
                StatusText(
                    isListening = uiState.isListening,
                    isAnalyzing = uiState.isAnalyzing,
                    partialText = uiState.partialText,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Your Speech section
                SpeechSection(
                    title = "Your Speech",
                    icon = Icons.Default.RecordVoiceOver,
                    color = SpeechSectionColor,
                    text = uiState.transcribedText,
                    partialText = if (uiState.isListening) uiState.partialText else "",
                    placeholder = "Hold the microphone button and speak..."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // AI Corrections section
                CorrectionsSection(
                    title = "AI Corrections",
                    icon = Icons.Default.AutoAwesome,
                    color = CorrectionSectionColor,
                    text = uiState.aiCorrections,
                    isAnalyzing = uiState.isAnalyzing,
                    placeholder = "AI analysis will appear here..."
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TopBar(
    isModelLoaded: Boolean,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "SpeakSmart",
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurfaceDark,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Your AI English Tutor",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariantDark,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Model status indicator
            Icon(
                imageVector = if (isModelLoaded) Icons.Outlined.CloudQueue else Icons.Outlined.CloudOff,
                contentDescription = if (isModelLoaded) "Qwen 2.5 LLM loaded" else "Using built-in rule analysis",
                tint = if (isModelLoaded) SuccessGreen else OnSurfaceVariantDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear session",
                    tint = OnSurfaceVariantDark,
                )
            }
        }
    }
}

@Composable
private fun ModelDownloadCard(
    downloadStatus: DownloadStatus,
    onStartDownload: (String?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryDark.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = PrimaryDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Download Qwen 2.5 (1.5B) Model",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceDark,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Enable full offline privacy-first AI LLM on phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantDark,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (downloadStatus) {
                is DownloadStatus.Idle -> {
                    Button(
                        onClick = { onStartDownload(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Model In-App (~1.1 GB)", fontWeight = FontWeight.SemiBold)
                    }
                }
                is DownloadStatus.Downloading -> {
                    val progressPercent = (downloadStatus.progress * 100).toInt()
                    val downloadedMB = downloadStatus.downloadedBytes / (1024 * 1024)
                    val totalMB = if (downloadStatus.totalBytes > 0) downloadStatus.totalBytes / (1024 * 1024) else 0

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading model...",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryDark,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (totalMB > 0) "$downloadedMB MB / $totalMB MB ($progressPercent%)" else "$downloadedMB MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadStatus.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = PrimaryDark,
                            trackColor = PrimaryContainer
                        )
                    }
                }
                is DownloadStatus.Completed -> {
                    Text(
                        text = "✅ Qwen 2.5 Model Downloaded & Ready!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                is DownloadStatus.Error -> {
                    Text(
                        text = "Download Error: ${downloadStatus.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onStartDownload(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Retry Download")
                    }
                }
            }
        }
    }
}

@Composable
private fun MicrophoneButton(
    isListening: Boolean,
    rmsLevel: Float,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")

    // Pulsing glow animation when listening
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isListening) 1.1f else 1f,
        animationSpec = tween(200),
        label = "button_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isListening) Color.White else PrimaryDark,
        animationSpec = tween(300),
        label = "icon_color"
    )

    val buttonSize = 120.dp
    val glowSize = 180.dp

    Box(
        modifier = Modifier
            .size(glowSize)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow rings when listening
        if (isListening) {
            Canvas(
                modifier = Modifier.size(glowSize)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = size.minDimension / 2

                // Animated glow ring 1
                drawCircle(
                    color = MicButtonPressedGlow,
                    radius = baseRadius * pulseScale,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Animated glow ring 2
                drawCircle(
                    color = MicButtonPressedGlow.copy(alpha = 0.3f),
                    radius = baseRadius * pulseScale * 1.15f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // RMS-based ring
                val rmsRadius = baseRadius * (0.7f + rmsLevel * 0.5f)
                drawCircle(
                    color = MicButtonPressedStart.copy(alpha = rmsLevel * 0.5f),
                    radius = rmsRadius,
                    center = center,
                    style = Stroke(width = (4 + rmsLevel * 4).dp.toPx())
                )
            }
        }

        // The main button
        Box(
            modifier = Modifier
                .size(buttonSize * buttonScale)
                .shadow(
                    elevation = if (isListening) 24.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isListening) MicButtonPressedStart else MicButtonStart,
                    spotColor = if (isListening) MicButtonPressedStart else MicButtonStart,
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isListening) {
                            listOf(MicButtonPressedStart, MicButtonPressedEnd)
                        } else {
                            listOf(MicButtonStart, MicButtonEnd)
                        }
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Hold to speak",
                modifier = Modifier.size(48.dp),
                tint = iconColor,
            )
        }
    }
}

@Composable
private fun StatusText(
    isListening: Boolean,
    isAnalyzing: Boolean,
    partialText: String,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotsAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dots_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        when {
            isListening -> {
                Text(
                    text = "Listening...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MicButtonPressedStart.copy(alpha = dotsAlpha),
                    fontWeight = FontWeight.SemiBold,
                )
                if (partialText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$partialText\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
            isAnalyzing -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CorrectionSectionColor,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyzing your speech with AI...",
                        style = MaterialTheme.typography.titleMedium,
                        color = CorrectionSectionColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            else -> {
                Text(
                    text = "Hold to Speak",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariantDark,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Press and hold the button, then speak clearly",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMutedDark,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SpeechSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    text: String,
    partialText: String,
    placeholder: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            val displayText = when {
                text.isNotBlank() -> text
                partialText.isNotBlank() -> partialText
                else -> ""
            }

            AnimatedVisibility(
                visible = displayText.isNotBlank(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (text.isNotBlank()) OnSurfaceDark else OnSurfaceVariantDark.copy(alpha = 0.7f),
                    lineHeight = 26.sp,
                )
            }

            if (displayText.isBlank()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMutedDark,
                )
            }
        }
    }
}

@Composable
private fun CorrectionsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    text: String,
    isAnalyzing: Boolean,
    placeholder: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            when {
                isAnalyzing -> {
                    Row(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = color,
                            strokeWidth = 2.5.dp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Analyzing your speech...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariantDark,
                        )
                    }
                }
                text.isNotBlank() -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 },
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceDark,
                            lineHeight = 26.sp,
                        )
                    }
                }
                else -> {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMutedDark,
                    )
                }
            }
        }
    }
}
