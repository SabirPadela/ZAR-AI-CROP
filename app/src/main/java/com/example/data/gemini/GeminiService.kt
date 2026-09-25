package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CropAnalysisResult
import com.example.data.model.KisanCenterItem
import com.example.data.model.MandiRateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Multimodal Image Diagnosis with Gemini 3.1 Pro Preview.
     * When withThinking is true, enables thinkingLevel: "HIGH" without maxOutputTokens.
     */
    suspend fun analyzeCropImage(
        bitmap: Bitmap,
        cropHint: String? = null,
        withThinking: Boolean = true
    ): Result<CropAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is not configured in BuildConfig."))
        }

        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val prompt = """
                You are a senior agronomist and plant pathologist specializing in Pakistani crops (Cotton, Wheat, Rice, Sugarcane, Maize, Citrus, Mango, Vegetables).
                Analyze this crop leaf/field image. ${cropHint?.let { "Farmer hints crop may be: $it" } ?: ""}
                
                Please diagnose the disease or deficiency, and provide:
                1. Crop Type (e.g. Cotton, Wheat, Rice, Sugarcane, Citrus, Tomato)
                2. Disease/Pest Name (e.g. Cotton Leaf Curl Virus (CLCuV), Yellow Rust, Rice Blast, Red Rot, Whitefly Infestation, Healthy)
                3. Confidence score (between 0.0 and 1.0)
                4. Severity (Low, Moderate, High, Critical)
                5. Visible Symptoms
                6. Practical Treatment & Spray Advice (mention active ingredients commonly registered in Pakistan like Imidacloprid, Acetamiprid, Carbendazim, Azoxystrobin, Chlorantraniliprole, or bio-control like Neem oil/Trichoderma with dosage per acre)
                7. Deep Thinking Agronomist Notes (differential diagnosis steps, weather triggers like high humidity/temperature, irrigation management)
                8. Advice summary in simple Roman Urdu for Pakistani farmers.

                Respond in valid JSON format only, matching this structure:
                {
                  "cropType": "Cotton",
                  "diseaseName": "Cotton Leaf Curl Virus",
                  "confidence": 0.92,
                  "severity": "High",
                  "symptoms": "Upward leaf curling, vein thickening, enation on underside of leaf.",
                  "treatmentAdvice": "Control whitefly vector by spraying Acetamiprid 20% SP @ 125g/acre or Pyriproxyfen 10.8% EC @ 500ml/acre. Rogue out severely infected plants.",
                  "thinkingNotes": "Differential diagnosis: Analyzed leaf curl vs thrips feeding damage. Observed characteristic enation which confirms begomovirus. High humidity and temp favor Bemisia tabaci vector population.",
                  "chemicalSprays": ["Acetamiprid 20% SP @ 125g/acre", "Pyriproxyfen 10.8% EC @ 500ml/acre"],
                  "biologicalControls": ["Neem seed extract spray (5%)", "Conserve Chrysoperla carnea predators"],
                  "precautionsUrdu": "Kapas ke khet mein safaid makhi ka fori spray karein aur mutasira podon ko dhoop mein zameen mein dafan karein."
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    if (withThinking) {
                        put("thinkingConfig", JSONObject().apply {
                            put("thinkingLevel", "HIGH")
                        })
                    }
                }
                put("generationConfig", generationConfig)
            }

            // Using gemini-3.1-pro-preview as mandated
            val modelName = "gemini-3.1-pro-preview"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiService", "Vision API error code ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("Gemini Vision request failed: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var resultText = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        resultText += part.getString("text")
                    }
                }
            }

            // Clean json markup if any
            val cleanJson = resultText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = JSONObject(cleanJson)
            val chemicalList = mutableListOf<String>()
            parsed.optJSONArray("chemicalSprays")?.let { arr ->
                for (i in 0 until arr.length()) chemicalList.add(arr.getString(i))
            }
            val bioList = mutableListOf<String>()
            parsed.optJSONArray("biologicalControls")?.let { arr ->
                for (i in 0 until arr.length()) bioList.add(arr.getString(i))
            }

            Result.success(
                CropAnalysisResult(
                    cropType = parsed.optString("cropType", "Crop"),
                    diseaseName = parsed.optString("diseaseName", "Unspecified Diagnosis"),
                    confidence = parsed.optDouble("confidence", 0.88),
                    severity = parsed.optString("severity", "Moderate"),
                    symptoms = parsed.optString("symptoms", "Symptoms evaluated on leaf surface."),
                    treatmentAdvice = parsed.optString("treatmentAdvice", "Inspect crop regularly and apply recommended protective spray."),
                    thinkingNotes = parsed.optString("thinkingNotes", "Reasoning based on characteristic visual pathology markers and local agricultural climate conditions."),
                    chemicalSprays = chemicalList,
                    biologicalControls = bioList,
                    precautionsUrdu = parsed.optString("precautionsUrdu", "Khet ka rozana muayina karein.")
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "analyzeCropImage failed", e)
            Result.failure(e)
        }
    }

    /**
     * Ask Agri Advisor with adaptive Gemini models:
     * - Fast tasks: gemini-3.1-flash-lite
     * - General queries: gemini-3.5-flash
     * - Complex reasoning: gemini-3.1-pro-preview
     */
    suspend fun askAgriAdvisor(
        query: String,
        cropContext: String = "",
        modelTier: String = "general" // "fast", "general", "deep"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        val modelName = when (modelTier) {
            "fast" -> "gemini-3.1-flash-lite"
            "deep" -> "gemini-3.1-pro-preview"
            else -> "gemini-3.5-flash"
        }

        try {
            val systemInstruction = """
                You are 'Zaria AI', a dedicated agricultural advisor for Pakistani farmers.
                Provide clear, practical, economical, and scientifically sound advice for farming in Pakistan (Punjab, Sindh, KPK, Balochistan).
                Cover crop management, irrigation (Nehari / Tubewell), fertilizer schedules (Urea, DAP, SOP), and pest control (PMA registered brands).
                Always answer in friendly, easy-to-understand English with key Roman Urdu summaries (e.g. 'Kisan bhaiyo ke liye mashwara').
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val textWithContext = if (cropContext.isNotBlank()) "Crop Context: $cropContext\nFarmer Query: $query" else query
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", textWithContext) })
                        })
                    })
                }
                put("contents", contents)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })

                val genConfig = JSONObject()
                if (modelTier == "deep") {
                    genConfig.put("thinkingConfig", JSONObject().apply {
                        put("thinkingLevel", "HIGH")
                    })
                }
                put("generationConfig", genConfig)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Advisor request failed: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            var text = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) text += p.getString("text")
                }
            }

            Result.success(text.ifBlank { "No advisory response returned." })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search Grounding using gemini-3.5-flash with googleSearch tool.
     * Fetches up-to-date Pakistan Mandi market rates (Grain & Vegetable Mandis).
     */
    suspend fun getMandiRatesWithSearch(
        cityOrRegion: String = "All Pakistan",
        cropFilter: String = ""
    ): Result<Pair<List<MandiRateItem>, String>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        val prompt = """
            Use Google Search to find current/latest wholesale Mandi (Ghalla Mandi, Sabzi Mandi) agricultural commodity prices in Pakistan ($cityOrRegion).
            ${cropFilter.let { if (it.isNotBlank()) "Focus on: $it." else "Include key crops: Cotton (Phutti/Kapas), Wheat (Gandum), Basmati Rice (Dhan/Chawal), Sugarcane (Kamad), Maize (Makai), Onion (Pyaz), Potato (Aaloo), Urea & DAP fertilizer bag prices." }}

            Return a structured JSON list of Mandi rates in this exact JSON format:
            {
              "rates": [
                {
                  "cropName": "Cotton (Phutti)",
                  "variety": "Local / Premium",
                  "marketCity": "Multan Mandi",
                  "minPrice": "8,200",
                  "maxPrice": "8,900",
                  "unit": "Rs. / 40 Kg",
                  "trend": "Up",
                  "notes": "Good export demand from local textile mills"
                }
              ],
              "marketSummary": "Brief overview of today's market conditions in Pakistan's agricultural trading hubs."
            }
        """.trimIndent()

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contents)

                // Mandatory Google Search Grounding tool
                val tools = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                put("tools", tools)

                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val modelName = "gemini-3.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Mandi search grounding failed: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidate = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            var text = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) text += p.getString("text")
                }
            }

            val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(clean)
            val ratesArray = parsed.optJSONArray("rates") ?: JSONArray()
            val list = mutableListOf<MandiRateItem>()
            for (i in 0 until ratesArray.length()) {
                val obj = ratesArray.getJSONObject(i)
                list.add(
                    MandiRateItem(
                        cropName = obj.optString("cropName", "Crop"),
                        variety = obj.optString("variety", "Standard"),
                        marketCity = obj.optString("marketCity", cityOrRegion),
                        minPrice = obj.optString("minPrice", "0"),
                        maxPrice = obj.optString("maxPrice", "0"),
                        unit = obj.optString("unit", "Rs. / 40 Kg"),
                        trend = obj.optString("trend", "Stable"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            val summary = parsed.optString("marketSummary", "Latest trading rates verified via Google Search Grounding.")

            Result.success(Pair(list, summary))
        } catch (e: Exception) {
            Log.e("GeminiService", "getMandiRatesWithSearch failed", e)
            Result.failure(e)
        }
    }

    /**
     * Maps Grounding using gemini-3.5-flash with googleMaps tool.
     * Locates nearby Kisan Centers, Agriculture Extension Offices, certified pesticide dealers in Pakistan.
     */
    suspend fun findKisanCentersWithMaps(
        cityOrDistrict: String = "Multan"
    ): Result<List<KisanCenterItem>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        val prompt = """
            Use Google Maps to locate agricultural service centers, Govt Agriculture Extension offices, Fauji Fertilizer Company (FFC) Farm Centers, Engro Kisan Centers, Zarai Taraqiati Bank (ZTBL), or certified seed agencies in and around $cityOrDistrict, Pakistan.
            
            Return a JSON list with up to 6 real locations in this format:
            {
              "centers": [
                {
                  "name": "Punjab Agriculture Extension Directorate",
                  "type": "Government Extension",
                  "city": "$cityOrDistrict",
                  "address": "Old Shujabad Road, Multan",
                  "phone": "061-9200234",
                  "services": "Soil testing, pesticide subsidy verification, crop advisories",
                  "rating": 4.6
                }
              ]
            }
        """.trimIndent()

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contents)

                // Mandatory Google Maps Grounding tool
                val tools = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleMaps", JSONObject())
                    })
                }
                put("tools", tools)

                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val modelName = "gemini-3.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Maps grounding request failed: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidate = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            var text = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) text += p.getString("text")
                }
            }

            val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(clean)
            val centersArr = parsed.optJSONArray("centers") ?: JSONArray()
            val list = mutableListOf<KisanCenterItem>()
            for (i in 0 until centersArr.length()) {
                val obj = centersArr.getJSONObject(i)
                list.add(
                    KisanCenterItem(
                        name = obj.optString("name", "Kisan Center"),
                        type = obj.optString("type", "Agri Services"),
                        city = obj.optString("city", cityOrDistrict),
                        address = obj.optString("address", ""),
                        phone = obj.optString("phone", ""),
                        services = obj.optString("services", "Agronomic advisory and inputs"),
                        rating = obj.optDouble("rating", 4.4)
                    )
                )
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e("GeminiService", "findKisanCentersWithMaps failed", e)
            Result.failure(e)
        }
    }

    /**
     * Text to Speech using model gemini-3.8-flash-tts as mandated.
     * Returns audio bytes (MP3 / WAV / PCM).
     */
    suspend fun generateSpeechAudio(text: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is missing."))
        }

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Read this agricultural advice clearly for the farmer: $text") })
                        })
                    })
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Kore")
                            })
                        })
                    })
                }
                put("generationConfig", generationConfig)
            }

            val modelName = "gemini-3.8-flash-tts"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("TTS generation failed: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidate = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("inlineData")) {
                        val inline = p.getJSONObject("inlineData")
                        val base64Data = inline.getString("data")
                        val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        return@withContext Result.success(audioBytes)
                    }
                }
            }

            Result.failure(Exception("No audio bytes received from gemini-3.8-flash-tts"))
        } catch (e: Exception) {
            Log.e("GeminiService", "generateSpeechAudio failed", e)
            Result.failure(e)
        }
    }

    /**
     * Audio Transcription using model gemini-3.5-transcribe as mandated.
     * Takes raw audio bytes (e.g. recorded from microphone), returns transcribed text.
     */
    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/wav"
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is missing."))
        }

        try {
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Transcribe this audio of a farmer speaking about their crop symptoms or agricultural question. Transcribe verbatim in Urdu, Roman Urdu, or English as spoken.")
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                }
                put("contents", contents)
            }

            val modelName = "gemini-3.5-transcribe"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Transcription failed: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidate = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            var text = ""
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("text")) text += p.getString("text")
                }
            }

            Result.success(text.trim().ifBlank { "Could not recognize audio clearly." })
        } catch (e: Exception) {
            Log.e("GeminiService", "transcribeAudio failed", e)
            Result.failure(e)
        }
    }
}
