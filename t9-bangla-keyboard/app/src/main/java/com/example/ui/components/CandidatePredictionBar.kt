package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.KeyboardTheme

@Composable
fun CandidatePredictionBar(
    candidates: List<String>,
    currentSequence: String,
    theme: KeyboardTheme,
    onSelectCandidate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        color = theme.keyBgColor.copy(alpha = 0.6f)
    ) {
        if (candidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (currentSequence.isNotEmpty()) "শব্দ টাইপ করুন ($currentSequence)..." else "T9 বাংলা কীবোর্ড প্রস্তুত",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textColor.copy(alpha = 0.45f),
                    fontSize = 12.sp
                )
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(candidates) { index, word ->
                    val isFirst = (index == 0)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isFirst) theme.primaryColor else theme.containerBgColor.copy(alpha = 0.8f)
                            )
                            .clickable { onSelectCandidate(word) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal,
                            color = if (isFirst) Color.White else theme.textColor,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
