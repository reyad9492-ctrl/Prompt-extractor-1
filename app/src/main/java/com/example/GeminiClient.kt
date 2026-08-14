package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
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

/**
 * Data structures for Gemini Response.
 */
data class PromptGenerationResult(
    val analyzedFace: String,
    val analyzedStyle: String,
    val generatedPrompt: String,
    val previewBitmap: Bitmap? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

data class ImagePreviewResult(
    val bitmap: Bitmap? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    
    // OkHttpClient with 60s timeout as requested by the Gemini API guidelines
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Compresses the image from Uri to a Base64 string that fits token sizes.
     */
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 800): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // Scale down bitmap to save memory and network bandwidth
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1f
            }

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            // Compress with high quality JPEG
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()

            // Clean up bitmaps
            if (scale < 1f) scaledBitmap.recycle()
            originalBitmap.recycle()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image to Base64", e)
            null
        }
    }

    /**
     * Call the Gemini API Model to analyze the portrait and generate a styled prompt.
     */
    suspend fun generatePrompt(
        context: Context,
        apiKey: String,
        instagramUrl: String = "",
        userPhotoUri: Uri? = null,
        stylePhotoUri: Uri? = null,
        targetEngine: String = "ChatGPT",
        creativeVibe: String = "🌸 Soft Glam Pastel",
        aspectRatio: String = "9:16",
        customPrefix: String = "",
        profileName: String = "Elma the cutie"
    ): PromptGenerationResult = withContext(Dispatchers.IO) {
        
        // 1. Convert photos to Base64 (if provided)
        val userBase64 = userPhotoUri?.let { uriToBase64(context, it) }
        val styleBase64 = stylePhotoUri?.let { uriToBase64(context, it) }

        // 2. Prepare Prompter instructions
        val promptText = """
            You are an expert AI prompt engineer specializing in creating detailed, high-fidelity visual prompts for $targetEngine.

            Your task is to craft an exceptionally high-fidelity image generation prompt that seamlessly blends a "User's Identity" (Persona: $profileName${if (userBase64 != null) ", captured with precision from their uploaded portrait photo" else ", gorgeous cutie aesthetic persona"}) with a "Cinematic Visual Style" (derived from the Instagram video link context, reference screenshot, and thematic vibe).

            Inputs provided:
            1. Subject Profile Persona: $profileName
            2. Instagram Video / Post Context: ${if (instagramUrl.isNotBlank()) instagramUrl else "Style derived from visual reference and vibe modifier"}
            3. Target Style Engine: $targetEngine
            4. Modifier Creative Vibe: $creativeVibe
            ${if (customPrefix.isNotBlank()) "5. Custom Prefix / Directive: $customPrefix" else ""}

            Step-by-Step Multimodal Analysis:
            1. Analyze the Subject Persona / Likeness:
               - Catalog facial features, eye shape, eye color, brow structure, nose contour, lips, jawline, and skin undertones.
               - Catalog hair style, texture, length, parting, and color.
               - Describe their likeness with respectful, highly accurate, and vivid photographic descriptors so an image generation model will faithfully replicate their identity.
            2. Analyze the Video Style Reference & Theme:
               - Deconstruct the lighting setup (volumetric rays, neon glow, Rembrandt lighting, rim highlights, golden hour warmth, soft diffused shadows).
               - Deconstruct the color grading (color palette, saturation, contrast, film stock aesthetic like 35mm Kodak Portra, teal & orange, Cyberpunk, or vintage warmth).
               - Deconstruct the environment (background architectural details, outdoor scenery, urban streetscapes, weather/atmosphere, particles, depth of field).
               - Deconstruct the wardrobe and styling (fabrics, textures, modern/vintage fashion, accessories).
            3. Synthesize & Harmonize:
               - Place the user as the central subject naturally embedded inside the video's aesthetic world and wardrobe.
            4. Structure the output prompt specifically tailored for the target engine ($targetEngine):
               - For ChatGPT (DALL-E 3):
                 Write a cohesive, evocative, and photorealistic natural language visual prompt starting with "A breathtaking high-fashion photograph capturing [detailed persona likeness, facial traits, hair styling]..." continuing with rich descriptive prose specifying outfit fabrics, volumetric lighting, color palette, scenic backdrop, depth of field, and atmosphere in $aspectRatio format.
               - For Gemini (Imagen 3):
                 Write an ultra-detailed, texture-rich photographic prompt focusing on authentic physical realism, skin undertones, delicate fabric sheen, precise focal depth, environmental lighting gradients, and composition framing in $aspectRatio aspect ratio.

            Output your response strictly in JSON format as shown below:

            ```json
            {
              "analyzedFace": "Detailed textual breakdown of the user's face, hair, and identity features to preserve.",
              "analyzedStyle": "Detailed textual breakdown of the visual theme, lighting, outfit, and background.",
              "generatedPrompt": "The final complete, ready-to-copy image generation prompt tailored for $targetEngine."
            }
            ```
        """.trimIndent()

        // 3. Construct JSON Payload using Android's native JSON utilities
        try {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Text Prompt part
            val textPart = JSONObject()
            textPart.put("text", promptText)
            partsArray.put(textPart)

            // User Photo Part (if provided)
            if (userBase64 != null) {
                val userPart = JSONObject()
                val userInlineData = JSONObject()
                userInlineData.put("mimeType", "image/jpeg")
                userInlineData.put("data", userBase64)
                userPart.put("inlineData", userInlineData)
                partsArray.put(userPart)
            }

            // Optional Style Photo Part
            if (styleBase64 != null) {
                val stylePart = JSONObject()
                val styleInlineData = JSONObject()
                styleInlineData.put("mimeType", "image/jpeg")
                styleInlineData.put("data", styleBase64)
                stylePart.put("inlineData", styleInlineData)
                partsArray.put(stylePart)
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)

            val payload = JSONObject()
            payload.put("contents", contentsArray)

            // Configuration for JSON Structured Output
            val config = JSONObject()
            config.put("responseMimeType", "application/json")
            config.put("temperature", 0.6)
            payload.put("generationConfig", config)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            // 4. Construct Request with apiKey. We call gemini-3.5-flash as the standard model.
            // Note: If the user provides a custom key, it passes here; otherwise they are advised.
            val model = "gemini-3.5-flash"
            val url = "$BASE_URL/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string()

            if (!response.isSuccessful || rawBody == null) {
                Log.e(TAG, "Request failed: ${response.code} with payload: $rawBody")
                return@withContext PromptGenerationResult(
                    analyzedFace = "",
                    analyzedStyle = "",
                    generatedPrompt = "",
                    isSuccess = false,
                    errorMessage = "Gemini API HTTP Error (${response.code}): ${response.message}"
                )
            }

            // 5. Parse Gemini API response JSON
            val responseObj = JSONObject(rawBody)
            val candidates = responseObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext PromptGenerationResult(
                    analyzedFace = "",
                    analyzedStyle = "",
                    generatedPrompt = "",
                    isSuccess = false,
                    errorMessage = "No valid response generated. The input photo may have triggered content filters. Please try another image."
                )
            }

            val firstCandidate = candidates.getJSONObject(0)
            val outputContent = firstCandidate.optJSONObject("content")
            val parts = outputContent?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text")

            if (textOutput == null) {
                return@withContext PromptGenerationResult(
                    analyzedFace = "",
                    analyzedStyle = "",
                    generatedPrompt = "",
                    isSuccess = false,
                    errorMessage = "Visual analysis output was empty."
                )
            }

            // Extract pure JSON block from raw text output (it might contain markdown blocks like ```json ... ```)
            var cleanJsonText = textOutput.trim()
            if (cleanJsonText.startsWith("```json")) {
                cleanJsonText = cleanJsonText.substring(7)
            }
            if (cleanJsonText.endsWith("```")) {
                cleanJsonText = cleanJsonText.substring(0, cleanJsonText.length - 3)
            }
            cleanJsonText = cleanJsonText.trim()

            try {
                val parsedResult = JSONObject(cleanJsonText)
                val face = parsedResult.optString("analyzedFace", "Preserved user identity parameters.")
                val style = parsedResult.optString("analyzedStyle", "Decoded visual mood parameters.")
                var generated = parsedResult.optString("generatedPrompt", "")

                // Standardize aspect ratio and engine syntax modifiers if missing
                when {
                    targetEngine.contains("Midjourney", ignoreCase = true) -> {
                        if (!generated.contains("--ar", ignoreCase = true)) {
                            generated += " --ar $aspectRatio"
                        }
                        if (!generated.contains("--v", ignoreCase = true)) {
                            generated += " --v 6.0"
                        }
                    }
                    targetEngine.contains("Bing", ignoreCase = true) -> {
                        if (!generated.contains("aspect ratio", ignoreCase = true) && !generated.contains(aspectRatio)) {
                            generated += ", composition framed in $aspectRatio aspect ratio, 8k resolution, photorealistic"
                        }
                    }
                    targetEngine.contains("ChatGPT", ignoreCase = true) -> {
                        if (!generated.contains("aspect ratio", ignoreCase = true)) {
                            generated += ", aspect ratio $aspectRatio"
                        }
                    }
                    else -> { // Gemini (Imagen 3)
                        if (!generated.contains("aspect ratio", ignoreCase = true)) {
                            generated += ", in $aspectRatio aspect ratio"
                        }
                    }
                }

                PromptGenerationResult(
                    analyzedFace = face,
                    analyzedStyle = style,
                    generatedPrompt = generated,
                    isSuccess = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed parsing prompt generation JSON, falling back to raw output", e)
                PromptGenerationResult(
                    analyzedFace = "Analyzed face successfully.",
                    analyzedStyle = "Analyzed style successfully.",
                    generatedPrompt = textOutput, // Fallback to raw text
                    isSuccess = true
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Network or unexpected processing failure", e)
            PromptGenerationResult(
                analyzedFace = "",
                analyzedStyle = "",
                generatedPrompt = "",
                isSuccess = false,
                errorMessage = "Failed to coordinate with AI: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Call the Gemini 2.5 Flash Image Model to render an AI visual preview for the engineered prompt.
     */
    suspend fun generateImagePreview(
        context: Context,
        apiKey: String,
        prompt: String,
        userPhotoUri: Uri?,
        aspectRatio: String = "1:1"
    ): ImagePreviewResult = withContext(Dispatchers.IO) {
        try {
            val userBase64 = userPhotoUri?.let { uriToBase64(context, it) }

            val validAspectRatio = when (aspectRatio) {
                "16:9" -> "16:9"
                "9:16" -> "9:16"
                "4:3" -> "4:3"
                "3:4", "4:5" -> "3:4"
                else -> "1:1"
            }

            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Clean prompt description for image generation model
            val cleanedPrompt = prompt
                .replace(Regex("--ar\\s+\\S+"), "")
                .replace(Regex("--v\\s+\\S+"), "")
                .replace(Regex("--stylize\\s+\\S+"), "")
                .trim()

            val textPart = JSONObject()
            textPart.put("text", "A high-fidelity, photorealistic, cinematic image based on this description: $cleanedPrompt")
            partsArray.put(textPart)

            // Include user face photo if available for identity preservation
            if (userBase64 != null) {
                val userPart = JSONObject()
                val userInlineData = JSONObject()
                userInlineData.put("mimeType", "image/jpeg")
                userInlineData.put("data", userBase64)
                userPart.put("inlineData", userInlineData)
                partsArray.put(userPart)
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)

            val payload = JSONObject()
            payload.put("contents", contentsArray)

            val imageConfig = JSONObject()
            imageConfig.put("aspectRatio", validAspectRatio)

            val config = JSONObject()
            config.put("imageConfig", imageConfig)
            val responseModalities = JSONArray()
            responseModalities.put("TEXT")
            responseModalities.put("IMAGE")
            config.put("responseModalities", responseModalities)

            payload.put("generationConfig", config)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            // Use gemini-2.5-flash-image for image generation tasks
            val model = "gemini-2.5-flash-image"
            val url = "$BASE_URL/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string()

            if (!response.isSuccessful || rawBody == null) {
                Log.e(TAG, "Image preview request failed: ${response.code} with payload: $rawBody")
                return@withContext ImagePreviewResult(
                    isSuccess = false,
                    errorMessage = "Image preview generation failed (${response.code})."
                )
            }

            val responseObj = JSONObject(rawBody)
            val candidates = responseObj.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val data = inlineData.optString("data")
                            if (data.isNotBlank()) {
                                val bytes = Base64.decode(data, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    return@withContext ImagePreviewResult(
                                        bitmap = bitmap,
                                        isSuccess = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ImagePreviewResult(
                isSuccess = false,
                errorMessage = "No visual image stream was returned by the preview engine."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating image preview", e)
            ImagePreviewResult(
                isSuccess = false,
                errorMessage = "Failed to render visual preview: ${e.localizedMessage}"
            )
        }
    }
}
