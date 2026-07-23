package com.example.keyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

enum class KeyboardTheme(val displayName: String, val primaryColor: Color, val keyBgColor: Color, val containerBgColor: Color, val textColor: Color) {
    EMERALD("Emerald Bengal", Color(0xFF10B981), Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFFF8FAFC)),
    DARK_SLATE("Midnight Indigo", Color(0xFF6366F1), Color(0xFF334155), Color(0xFF0F172A), Color(0xFFF8FAFC)),
    CRIMSON("Crimson Rose", Color(0xFFF43F5E), Color(0xFF271318), Color(0xFF14080B), Color(0xFFFFF1F2)),
    SUNSET("Sunset Gold", Color(0xFFF59E0B), Color(0xFF291E13), Color(0xFF181008), Color(0xFFFEF3C7)),
    OLED_BLACK("OLED Pure Black", Color(0xFF14B8A6), Color(0xFF18181B), Color(0xFF000000), Color(0xFFFAFAFA)),
    LIGHT_BLUE("Classic Light", Color(0xFF0284C7), Color(0xFFE2E8F0), Color(0xFFF1F5F9), Color(0xFF0F172A))
}

enum class TypingMode(val title: String, val shortLabel: String) {
    PREDICTIVE("3x4 AI Bengali Keypad", "3x4 AI"),
    BANGLA_QWERTY("Bangla QWERTY", "Q-BN"),
    ENGLISH("English QWERTY", "Q-EN"),
    MULTI_TAP("Multi-tap", "TAP"),
    AVRO_PHONETIC("Avro Phonetic", "AVRO"),
    NUMERIC("Numbers & Symbols", "123")
}

class KeyboardPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("t9_bangla_prefs", Context.MODE_PRIVATE)

    var theme: KeyboardTheme
        get() {
            val name = prefs.getString("theme", KeyboardTheme.EMERALD.name) ?: KeyboardTheme.EMERALD.name
            return try { KeyboardTheme.valueOf(name) } catch (e: Exception) { KeyboardTheme.EMERALD }
        }
        set(value) = prefs.edit().putString("theme", value.name).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_enabled", true)
        set(value) = prefs.edit().putBoolean("haptic_enabled", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var keyHeightScale: Float
        get() = prefs.getFloat("key_height_scale", 1.0f)
        set(value) = prefs.edit().putFloat("key_height_scale", value).apply()

    var defaultTypingMode: TypingMode
        get() {
            val name = prefs.getString("default_typing_mode", TypingMode.PREDICTIVE.name) ?: TypingMode.PREDICTIVE.name
            return try { TypingMode.valueOf(name) } catch (e: Exception) { TypingMode.PREDICTIVE }
        }
        set(value) = prefs.edit().putString("default_typing_mode", value.name).apply()

    var autoSpaceAfterPunctuation: Boolean
        get() = prefs.getBoolean("auto_space", true)
        set(value) = prefs.edit().putBoolean("auto_space", value).apply()

    var showCandidateBar: Boolean
        get() = prefs.getBoolean("show_candidate_bar", true)
        set(value) = prefs.edit().putBoolean("show_candidate_bar", value).apply()
}
