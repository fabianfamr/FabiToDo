package com.fabian.todolist.data.gemini

import android.content.Context
import android.util.Log
import com.fabian.todolist.R
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

object FirebaseAiManager {
    private const val TAG = "FirebaseAiManager"

    fun isGeminiModel(modelName: String): Boolean {
        return modelName.startsWith("gemini", ignoreCase = true)
    }

    fun mapToSupportedModel(modelName: String, lowLatency: Boolean = false): String {
        if (lowLatency) {
            return "gemini-3.5-flash-lite"
        }
        return when {
            modelName.contains("flash-lite", ignoreCase = true) || modelName.contains("lite", ignoreCase = true) -> "gemini-3.5-flash-lite"
            modelName.contains("argon", ignoreCase = true) -> "gemini-4-argon"
            modelName.contains("pro", ignoreCase = true) -> "gemini-4-argon"
            else -> "gemini-3.8-flash"
        }
    }

    suspend fun generateContent(
        modelName: String,
        prompt: String,
        systemInstructionText: String? = null,
        lowLatency: Boolean = false,
        timeoutMs: Long = 60_000L,
        userContext: Context? = null
    ): String {
        val targetModelName = mapToSupportedModel(modelName, lowLatency)
        Log.d(TAG, "Initializing Firebase AI model: $targetModelName")

        val generativeModel = Firebase.ai.generativeModel(
            modelName = targetModelName,
            systemInstruction = systemInstructionText?.let { content { text(it) } }
        )

        return try {
            withTimeout(timeoutMs) {
                val response = generativeModel.generateContent(prompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    text
                } else {
                    throw IllegalStateException(
                        userContext?.getString(R.string.ai_error_empty_response)
                            ?: "Model returned an empty response."
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out after ${timeoutMs}ms", e)
            throw IllegalStateException(
                userContext?.getString(R.string.ai_error_timeout)
                    ?: "Generation timed out. Please try again."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed: ${e.message}", e)
            throw e
        }
    }
}
