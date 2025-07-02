package com.vani.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConnectivityIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = if (isConnected) "Connected" else "No Internet",
            modifier = Modifier.size(16.dp),
            tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
        
        Text(
            text = if (isConnected) "Connected" else "No Internet",
            fontSize = 14.sp,
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}