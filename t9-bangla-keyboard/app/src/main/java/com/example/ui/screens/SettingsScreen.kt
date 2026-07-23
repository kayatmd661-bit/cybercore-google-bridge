package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyboard.KeyboardPreferences
import com.example.keyboard.KeyboardTheme
import com.example.keyboard.TypingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefs: KeyboardPreferences) {
    val context = LocalContext.current

    var selectedTheme by remember { mutableStateOf(prefs.theme) }
    var hapticEnabled by remember { mutableStateOf(prefs.hapticEnabled) }
    var keyHeightScale by remember { mutableFloatStateOf(prefs.keyHeightScale) }
    var defaultMode by remember { mutableStateOf(prefs.defaultTypingMode) }
    var showCandidateBar by remember { mutableStateOf(prefs.showCandidateBar) }
    var autoSpace by remember { mutableStateOf(prefs.autoSpaceAfterPunctuation) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কীবোর্ড সেটিংস ও কাস্টমাইজেশন", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কীবোর্ড থিম নির্বাচন করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyboardTheme.entries.forEach { theme ->
                            val isSelected = (theme == selectedTheme)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedTheme = theme
                                        prefs.theme = theme
                                        Toast.makeText(context, "${theme.displayName} থিম সেট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = theme.containerBgColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(theme.primaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = theme.displayName,
                                            color = theme.textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = theme.primaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Key Height & Typing Behavior Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "কিপ্যাড লেআউট ও উচ্চতা",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column {
                        Text(
                            text = "কিপ্যাডের উচ্চতা: ${String.format("%.0f%%", keyHeightScale * 100)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = keyHeightScale,
                            onValueChange = {
                                keyHeightScale = it
                                prefs.keyHeightScale = it
                            },
                            valueRange = 0.85f..1.25f,
                            steps = 4
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "কী চাপলে ভাইব্রেশন (হ্যাপটিক)", fontWeight = FontWeight.SemiBold)
                            Text(text = "প্রতিটি বাটনে স্পর্শ করার সময় হালকা ভাইব্রেশন প্রদান করে", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = {
                                hapticEnabled = it
                                prefs.hapticEnabled = it
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "প্রেডিক্টিভ ক্যান্ডিডেট বার", fontWeight = FontWeight.SemiBold)
                            Text(text = "টাইপ করার সময় সম্ভাব্য শব্দের তালিকা প্রদর্শন করবে", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = showCandidateBar,
                            onCheckedChange = {
                                showCandidateBar = it
                                prefs.showCandidateBar = it
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "শব্দ নির্বাচনের পর স্বয়ংক্রিয় স্পেস", fontWeight = FontWeight.SemiBold)
                            Text(text = "শব্দে ট্যাপ করার সাথে সাথে স্পেস যোগ হবে", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = autoSpace,
                            onCheckedChange = {
                                autoSpace = it
                                prefs.autoSpaceAfterPunctuation = it
                            }
                        )
                    }
                }
            }
        }
    }
}
