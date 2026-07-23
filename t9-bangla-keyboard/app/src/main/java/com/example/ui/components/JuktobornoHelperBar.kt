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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dictionary.BanglaPhoneticConverter
import com.example.keyboard.KeyboardTheme

@Composable
fun JuktobornoHelperBar(
    theme: KeyboardTheme,
    onSelectJuktoborno: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(theme.containerBgColor.copy(alpha = 0.9f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "যুক্তবর্ণ:",
            style = MaterialTheme.typography.labelSmall,
            color = theme.primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 6.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(BanglaPhoneticConverter.commonJuktobornoList) { jukto ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.keyBgColor)
                        .clickable { onSelectJuktoborno(jukto) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = jukto,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
