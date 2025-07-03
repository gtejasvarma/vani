package com.vani.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.vani.ui.theme.AccessibilityColors
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Professional waveform visualization for accessibility-first audio feedback
 * Provides visual representation of audio activity for hard of hearing users
 */
@Composable
fun WaveformVisualization(
    isListening: Boolean,
    rmsDb: Float = -40f,
    modifier: Modifier = Modifier,
    waveformColor: Color = AccessibilityColors.WaveformActive,
    backgroundColor: Color = AccessibilityColors.WaveformBackground,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    // Animation for active waveform
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_animation"
    )
    
    // Volume-based amplitude calculation
    val amplitude = if (isListening) {
        // Convert RMS dB to amplitude (0.0 to 1.0)
        val normalizedDb = (rmsDb + 60f) / 60f // Normalize -60dB to 0dB range
        (normalizedDb.coerceIn(0f, 1f) * 0.8f + 0.2f) // Minimum 20% amplitude
    } else {
        0.1f // Idle state
    }
    
    Box(
        modifier = modifier
            .height(80.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / barCount * 0.6f
            val spacing = canvasWidth / barCount * 0.4f
            val centerY = canvasHeight / 2
            
            repeat(barCount) { index ->
                val x = index * (barWidth + spacing) + barWidth / 2
                
                // Calculate bar height based on position and animation
                val baseHeight = if (isListening) {
                    val phase = (animationProgress * 2 * Math.PI + index * 0.5f).toFloat()
                    val waveOffset = sin(phase) * 0.3f + 0.7f
                    val randomVariation = if (index % 3 == 0) Random.nextFloat() * 0.4f + 0.6f else 1f
                    canvasHeight * amplitude * waveOffset * randomVariation * 0.8f
                } else {
                    canvasHeight * amplitude * (Random.nextFloat() * 0.3f + 0.1f)
                }
                
                val barHeight = baseHeight.coerceIn(4.dp.toPx(), canvasHeight * 0.9f)
                
                // Color based on volume level
                val barColor = if (isListening) {
                    AccessibilityColors.getVolumeColor(rmsDb)
                } else {
                    AccessibilityColors.WaveformIdle
                }
                
                // Draw the waveform bar
                drawLine(
                    color = barColor,
                    start = Offset(x, centerY - barHeight / 2),
                    end = Offset(x, centerY + barHeight / 2),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // Overlay listening indicator
        if (isListening) {
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing listening dot
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                
                Box(
                    modifier = Modifier
                        .size((6 * pulseScale).dp)
                        .background(
                            color = AccessibilityColors.AudioActive,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Simplified waveform for microphone button area
 */
@Composable
fun MicrophoneWaveform(
    isListening: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 5
) {
    WaveformVisualization(
        isListening = isListening,
        modifier = modifier.height(32.dp),
        barCount = barCount,
        waveformColor = if (isListening) AccessibilityColors.AudioActive else AccessibilityColors.AudioIdle,
        backgroundColor = Color.Transparent
    )
}