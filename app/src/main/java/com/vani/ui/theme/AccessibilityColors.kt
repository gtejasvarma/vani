package com.vani.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Professional color system designed for accessibility-first hard of hearing users
 * High contrast, meaningful colors for speaker identification and confidence levels
 */
object AccessibilityColors {
    
    // Speaker Identification Colors - High contrast and distinguishable
    val Speaker1Primary = Color(0xFF1976D2)      // Professional Blue
    val Speaker1Light = Color(0xFF63A4FF)        // Light Blue
    val Speaker1Dark = Color(0xFF004BA0)         // Dark Blue
    
    val Speaker2Primary = Color(0xFF388E3C)      // Success Green  
    val Speaker2Light = Color(0xFF6ABF69)        // Light Green
    val Speaker2Dark = Color(0xFF00600F)         // Dark Green
    
    val Speaker3Primary = Color(0xFF7B1FA2)      // Deep Purple
    val Speaker3Light = Color(0xFFAE52D4)        // Light Purple
    val Speaker3Dark = Color(0xFF4A0072)         // Dark Purple
    
    val UserSpeaker = Color(0xFFFF6F00)          // Vibrant Orange for user
    val UserSpeakerLight = Color(0xFFFF9800)     // Light Orange
    val UserSpeakerDark = Color(0xFFE65100)      // Dark Orange
    
    // Confidence Level Colors
    val HighConfidence = Color(0xFF4CAF50)       // Green - 90%+ accuracy
    val MediumConfidence = Color(0xFFFF9800)     // Orange - 70-89% accuracy  
    val LowConfidence = Color(0xFFF44336)        // Red - <70% accuracy
    val ProcessingConfidence = Color(0xFF9E9E9E) // Gray - Processing
    
    // Audio Visual Feedback Colors
    val AudioActive = Color(0xFF00E676)          // Bright Green - Audio detected
    val AudioIdle = Color(0xFF546E7A)            // Blue Gray - No audio
    val VolumeHigh = Color(0xFF4CAF50)           // Green - High volume
    val VolumeMedium = Color(0xFFFF9800)         // Orange - Medium volume
    val VolumeLow = Color(0xFFFF5722)            // Red - Low volume
    
    // Status and Alert Colors
    val ErrorRed = Color(0xFFD32F2F)             // Error states
    val WarningAmber = Color(0xFFFF8F00)         // Warning states
    val InfoBlue = Color(0xFF1976D2)             // Information states
    val SuccessGreen = Color(0xFF388E3C)         // Success states
    
    // Background and Surface Colors
    val SurfaceElevated = Color(0xFFF5F5F5)      // Light gray for cards
    val SurfaceHighEmphasis = Color(0xFFFFFFFF)   // White for high emphasis
    val OnSurfaceHigh = Color(0xFF212121)        // High contrast text
    val OnSurfaceMedium = Color(0xFF757575)      // Medium contrast text
    
    // Waveform Visualization Colors
    val WaveformActive = Color(0xFF00E676)       // Active waveform
    val WaveformIdle = Color(0xFFBDBDBD)         // Idle waveform
    val WaveformBackground = Color(0xFFF5F5F5)   // Waveform background
    
    /**
     * Get speaker color by index for consistent identification
     */
    fun getSpeakerColor(speakerIndex: Int): Color {
        return when (speakerIndex % 3) {
            0 -> Speaker1Primary
            1 -> Speaker2Primary  
            2 -> Speaker3Primary
            else -> Speaker1Primary
        }
    }
    
    /**
     * Get confidence color based on confidence level (0.0 to 1.0)
     */
    fun getConfidenceColor(confidence: Float): Color {
        return when {
            confidence >= 0.9f -> HighConfidence
            confidence >= 0.7f -> MediumConfidence
            confidence >= 0.0f -> LowConfidence
            else -> ProcessingConfidence
        }
    }
    
    /**
     * Get volume level color based on RMS volume
     */
    fun getVolumeColor(rmsDb: Float): Color {
        return when {
            rmsDb > -20f -> VolumeHigh
            rmsDb > -40f -> VolumeMedium
            else -> VolumeLow
        }
    }
}