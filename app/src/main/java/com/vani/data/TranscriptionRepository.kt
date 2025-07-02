package com.vani.data

import com.vani.data.model.TranscriptLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TranscriptionRepository {
    private val _transcriptLines = MutableStateFlow<List<TranscriptLine>>(emptyList())
    val transcriptLines: StateFlow<List<TranscriptLine>> = _transcriptLines.asStateFlow()
    
    fun addTranscriptLine(line: TranscriptLine) {
        val currentLines = _transcriptLines.value.toMutableList()
        currentLines.add(line)
        _transcriptLines.value = currentLines
    }
    
    fun clearTranscript() {
        _transcriptLines.value = emptyList()
    }
}