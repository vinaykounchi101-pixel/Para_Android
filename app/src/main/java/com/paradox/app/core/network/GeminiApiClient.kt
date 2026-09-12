package com.paradox.app.core.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class GeminiReceiptExtraction(
    val merchant: String?,
    val totalAmount: Double?,
    val date: String?,
    val categoryName: String?,
    val paymentMode: String?,
    val notes: String?
)

data class GeminiCategorySuggestion(
    val categoryName: String,
    val budgetClassification: String, // NEEDS, WANTS, SAVINGS
    val sentiment: String // Happy, Neutral, Stressed, Remorse
)

@Singleton
class GeminiApiClient @Inject constructor() {

    suspend fun generateResponse(
        prompt: String,
        systemInstruction: String,
        apiKey: String,
        model: String = "gemini-1.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 25000
            }

            val rootJson = JSONObject().apply {
                if (systemInstruction.isNotBlank()) {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        })
                    })
                }
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 1500)
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(rootJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseStr)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (!text.isNullOrBlank()) {
                    Result.success(text.trim())
                } else {
                    Result.failure(IllegalStateException("Empty response from Gemini API"))
                }
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "HTTP $responseCode"
                Result.failure(IllegalStateException("Gemini API error ($responseCode): $errorStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeReceiptMultimodal(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        availableCategories: List<String>,
        apiKey: String,
        model: String = "gemini-1.5-flash"
    ): Result<GeminiReceiptExtraction> = withContext(Dispatchers.IO) {
        try {
            val base64Data = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val categoriesHint = if (availableCategories.isNotEmpty()) {
                "Available user categories: ${availableCategories.joinToString(", ")}."
            } else ""

            val prompt = """
                Analyze this receipt or bill image and extract the financial data.
                $categoriesHint
                Return ONLY valid JSON matching this schema:
                {
                  "merchant": "Store or merchant name",
                  "totalAmount": 123.45,
                  "date": "YYYY-MM-DD",
                  "categoryName": "Best matching category name",
                  "paymentMode": "CASH | CARD | UPI | BANK",
                  "notes": "Brief summary of items bought"
                }
                If any field cannot be found, use null.
            """.trimIndent()

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 20000
                readTimeout = 30000
            }

            val rootJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", mimeType)
                                    put("data", base64Data)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.1)
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(rootJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseStr)
                val rawText = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (!rawText.isNullOrBlank()) {
                    val cleanJsonStr = rawText.trim().removeSurrounding("```json", "```").trim()
                    val parsed = JSONObject(cleanJsonStr)
                    val extraction = GeminiReceiptExtraction(
                        merchant = if (parsed.isNull("merchant")) null else parsed.optString("merchant"),
                        totalAmount = if (parsed.isNull("totalAmount")) null else parsed.optDouble("totalAmount"),
                        date = if (parsed.isNull("date")) null else parsed.optString("date"),
                        categoryName = if (parsed.isNull("categoryName")) null else parsed.optString("categoryName"),
                        paymentMode = if (parsed.isNull("paymentMode")) null else parsed.optString("paymentMode"),
                        notes = if (parsed.isNull("notes")) null else parsed.optString("notes")
                    )
                    Result.success(extraction)
                } else {
                    Result.failure(IllegalStateException("Empty response from vision model"))
                }
            } else {
                val errorStr = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: "HTTP $responseCode"
                Result.failure(IllegalStateException("Vision API error ($responseCode): $errorStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun suggestCategoryAndMood(
        title: String,
        availableCategories: List<String>,
        apiKey: String
    ): Result<GeminiCategorySuggestion> = withContext(Dispatchers.IO) {
        try {
            val categoriesHint = availableCategories.joinToString(", ")
            val prompt = """
                Given the expense title: "$title"
                Match it to the best category from: [$categoriesHint].
                Also classify the budget bucket as NEEDS (50%), WANTS (30%), or SAVINGS (20%).
                Also detect the user's spending sentiment (Happy, Neutral, Stressed, Remorse).
                Return JSON only:
                {
                  "categoryName": "matching category",
                  "budgetClassification": "NEEDS | WANTS | SAVINGS",
                  "sentiment": "Happy | Neutral | Stressed | Remorse"
                }
            """.trimIndent()

            val res = generateResponse(
                prompt = prompt,
                systemInstruction = "You are a smart financial categorization system. Output valid JSON only.",
                apiKey = apiKey
            )

            res.mapCatching { jsonStr ->
                val clean = jsonStr.removeSurrounding("```json", "```").trim()
                val obj = JSONObject(clean)
                GeminiCategorySuggestion(
                    categoryName = obj.optString("categoryName", availableCategories.firstOrNull() ?: "General"),
                    budgetClassification = obj.optString("budgetClassification", "WANTS"),
                    sentiment = obj.optString("sentiment", "Neutral")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
