package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.KeyboardTheme

@Composable
fun EmojiAndSymbolBar(
    theme: KeyboardTheme,
    onSelectSymbol: (String) -> Unit
) {
    val symbolsAndEmojis = remember {
        listOf(
            "।", "৳", "₹", "?", "!", ",", "@", "#", "$", "%", "&", "*", "(", ")", "-", "+", "=",
            "😊", "❤️", "👍", "🇧🇩", "🙏", "🔥", "🎉", "😍", "👏", "😃", "🙌", "💯"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(theme.containerBgColor)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(symbolsAndEmojis) { item: String ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.keyBgColor)
                        .clickable { onSelectSymbol(item) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textColor,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
