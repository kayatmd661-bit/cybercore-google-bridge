package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.keyboard.KeyboardTheme

@Composable
fun CandidateViewAdapter(
    candidates: List<String>,
    currentSequence: String,
    theme: KeyboardTheme,
    onSelectCandidate: (String) -> Unit
) {
    CandidatePredictionBar(
        candidates = candidates,
        currentSequence = currentSequence,
        theme = theme,
        onSelectCandidate = onSelectCandidate
    )
}
