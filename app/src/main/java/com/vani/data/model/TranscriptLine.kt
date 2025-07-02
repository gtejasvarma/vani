package com.vani.data.model

import java.util.UUID

data class TranscriptLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)