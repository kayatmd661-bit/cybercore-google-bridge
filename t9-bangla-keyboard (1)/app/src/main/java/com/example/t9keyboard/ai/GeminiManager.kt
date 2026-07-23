package com.example.t9keyboard.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateWordCompletions(currentText: String, isBanglaMode: Boolean): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured in Secrets panel")
            return@withContext listOf(
                if (isBanglaMode) "ভালো আছ?" else "How are you?",
                if (isBanglaMode) "আজকে কেমন আছেন?" else "Have a nice day!"
            )
        }

        val targetLang = if (isBanglaMode) "Bengali" else "English"
        val prompt = "Give 3 short, natural next-word autocompletions or smart sentence continuations in $targetLang for the typed context: \"$currentText\". Respond with ONLY a comma-separated list of candidate completions, without any extra explanation or numbering."

        try {
            val jsonParam = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val requestBody = jsonParam.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textResult = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

                if (textResult.isNotEmpty()) {
                    return@withContext textResult.split(",")
                        .map { it.trim().removePrefix("-").trim() }
                        .filter { it.isNotEmpty() }
                }
            } else {
                Log.e(TAG, "Gemini API HTTP Error: ${response.code} - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed", e)
        }

        // Fallback suggestions if offline or API error
        return@withContext if (isBanglaMode) {
            listOf("কেমন আছেন", "ধন্যবাদ", "খুব ভাল")
        } else {
            listOf("thank you", "sounds good", "see you soon")
        }
    }

    suspend fun generateSmartReply(sender: String, message: String, userPrompt: String? = null, isBanglaMode: Boolean = true): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val targetLang = if (isBanglaMode) "Bengali" else "English"
        
        val prompt = if (!userPrompt.isNullOrEmpty()) {
            "Translate or rephrase this quick reply politely in $targetLang for $sender: \"$userPrompt\""
        } else {
            "Generate a polite, concise, natural reply in $targetLang to $sender who sent: \"$message\". Keep it under 15 words."
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext if (isBanglaMode) "ধন্যবাদ! আমি একটু ব্যস্ত আছি, পরে কথা বলছি।" else "Thanks! I'm a bit busy right now, I'll reply soon."
        }

        try {
            val jsonParam = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val requestBody = jsonParam.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.optJSONArray("candidates")
                val textResult = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")?.trim() ?: ""

                if (textResult.isNotEmpty()) return@withContext textResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Smart reply generation failed", e)
        }

        return@withContext if (isBanglaMode) "ঠিক আছে, ধন্যবাদ!" else "Got it, thank you!"
    }
}
