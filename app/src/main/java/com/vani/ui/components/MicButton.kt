package com.vani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vani.viewmodel.MicButtonState

@Composable
fun MicButton(
    state: MicButtonState,
    isConnected: Boolean,
    onTap: () -> Unit,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor = when {
        !isConnected -> MaterialTheme.colorScheme.outline
        state == MicButtonState.LISTENING -> Color(0xFF4CAF50) // Green
        else -> MaterialTheme.colorScheme.primary
    }
    
    val contentColor = when {
        !isConnected -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.White
    }
    
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isConnected) {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    },
                    onTap = {
                        if (isConnected) {
                            onTap()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = when (state) {
                MicButtonState.IDLE -> "Start listening"
                MicButtonState.LISTENING -> "Stop listening"
            },
            modifier = Modifier.size(32.dp),
            tint = contentColor
        )
    }
}