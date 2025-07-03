package com.vani.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vani.data.model.TranscriptLine
import com.vani.viewmodel.MainViewModel
import com.vani.viewmodel.MicButtonState
import com.vani.ui.theme.AccessibilityColors

@Composable
fun ConversationScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.transcriptLines.size) {
        if (uiState.transcriptLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.transcriptLines.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        
        // Conversation Area
        if (uiState.transcriptLines.isEmpty()) {
            // Empty state - Accessibility focused
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Start conversation",
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Ready to Listen",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap the microphone to start capturing conversation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                )
            }
        } else {
            // Conversation list - maximum space for text (no top bar)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp), // Less top padding without status bar
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = 180.dp // Space for mic button only
                )
            ) {
                items(uiState.transcriptLines) { line ->
                    ConversationCard(line = line)
                }
            }
        }
        
        // Microphone button with integrated connection status
        MicrophoneButton(
            state = uiState.micButtonState,
            isConnected = uiState.isConnected,
            isListening = uiState.isListening,
            onTap = viewModel::onMicButtonTap,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
        
        // Clear Button
        if (uiState.transcriptLines.isNotEmpty()) {
            ClearButton(
                onClick = viewModel::clearConversation,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp)
            )
        }
    }
}



@Composable
private fun ConversationCard(
    line: TranscriptLine,
    modifier: Modifier = Modifier
) {
    // Radical simplicity - just the words with subtle speaker hint
    val speakerIndex = line.text.hashCode() % 3
    val speakerColor = AccessibilityColors.getSpeakerColor(speakerIndex)
    
    // Pure simplicity - just colored text for speaker identification
    Text(
        text = line.text,
        style = MaterialTheme.typography.headlineSmall,
        color = speakerColor, // Text color directly indicates speaker
        fontWeight = FontWeight.Normal,
        lineHeight = 40.sp,
        fontSize = 28.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun MicrophoneButton(
    state: MicButtonState,
    isConnected: Boolean,
    isListening: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListeningState = state == MicButtonState.LISTENING
    
    // Connection-based colors and states
    val buttonColor = when {
        isListening -> AccessibilityColors.AudioActive // Green when actively listening
        isConnected -> AccessibilityColors.Speaker1Primary // Blue when connected and ready
        else -> AccessibilityColors.ErrorRed // Red when disconnected
    }
    
    val borderColor = when {
        isListening -> Color.Transparent // No border when listening (clean focus)
        isConnected -> AccessibilityColors.SuccessGreen // Green border when connected
        else -> AccessibilityColors.ErrorRed // Red border when disconnected
    }
    
    // Pulsing animation only for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        // Connection status border (hidden during listening)
        if (!isListening) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = Color.Transparent,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                borderColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        FloatingActionButton(
            onClick = if (isConnected) onTap else { {} }, // Disabled when disconnected
            modifier = Modifier
                .size(88.dp)
                .scale(pulseScale),
            containerColor = buttonColor,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (isListening) 12.dp else 8.dp
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = when {
                    isListening -> "Stop listening"
                    isConnected -> "Start listening"
                    else -> "No connection"
                },
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun ClearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .wrapContentWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFFFF5722)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = Color(0xFFFF5722)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear conversation",
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Clear",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF5722)
            )
        }
    }
}

