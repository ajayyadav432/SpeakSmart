package com.example.speaksmart.ui.chat

// AgentsChatScreen.kt — wraps the existing ChatScreen (which handles the persona-selector + per-agent chat)
// This simply re-exports ChatScreen under a more semantic name for Navigation.kt

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AgentsChatScreen(
    modifier: Modifier = Modifier,
) {
    ChatScreen(modifier = modifier)
}
