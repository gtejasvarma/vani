package com.vani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vani.data.model.TranscriptLine
import com.vani.ui.components.TopAppBar
import com.vani.viewmodel.MainViewModel
import com.vani.viewmodel.MicButtonState

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            com.vani.ui.components.TopAppBar(
                isConnected = uiState.isConnected,
                isListening = uiState.isListening,
                modifier = Modifier.padding(top = 24.dp)
            )
        },
        bottomBar = {
            BottomAppBar(
                onClearClick = viewModel::clearConversation,
                onMicClick = viewModel::onMicButtonTap,
                micState = uiState.micButtonState
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(uiState.transcriptLines) { line ->
                TranscriptLineItem(line = line)
            }
        }
    }
}

@Composable
private fun BottomAppBar(
    onClearClick: () -> Unit,
    onMicClick: () -> Unit,
    micState: MicButtonState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear Button
        OutlinedButton(
            onClick = onClearClick,
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear conversation",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear", fontSize = 16.sp)
        }

        // Mic Button
        MicButton(
            state = micState,
            onTap = onMicClick
        )

        // Placeholder for balance
        Spacer(modifier = Modifier.width(88.dp))
    }
}


@Composable
private fun TranscriptLineItem(
    line: TranscriptLine,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = line.text,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            lineHeight = 40.sp
        )
    }
}

@Composable
private fun MicButton(
    state: MicButtonState,
    onTap: () -> Unit
) {
    val icon = when (state) {
        MicButtonState.IDLE -> Icons.Default.PlayArrow
        MicButtonState.LISTENING -> Icons.Default.Close
    }
    
    val colors = when (state) {
        MicButtonState.IDLE -> IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        MicButtonState.LISTENING -> IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    }
    
    IconButton(
        onClick = onTap,
        modifier = Modifier.size(72.dp),
        colors = colors
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Record",
            modifier = Modifier.size(36.dp)
        )
    }
}