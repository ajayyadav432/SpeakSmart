package com.example.speaksmart.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper around MediaPipe LLM Inference API.
 *
 * The user must push a compatible model (e.g. Gemma 2B .bin) to the device at
 * /data/local/tmp/llm/model.bin  (or configure a different path).
 *
 * adb shell mkdir -p /data/local/tmp/llm/
 * adb push gemma-2b-it-gpu-int4.bin /data/local/tmp/llm/model.bin
 */
class LlmInferenceHelper(private val context: Context) {

    companion object {
        private const val TAG = "LlmInferenceHelper"

        // Default path where models are expected
        private const val DEFAULT_MODEL_PATH = "/data/local/tmp/llm/model.bin"

        // Also check the app's internal files directory
        private fun getAppModelPath(context: Context): String =
            File(context.filesDir, "llm/model.bin").absolutePath

        // System prompt for the English tutor
        const val SYSTEM_PROMPT = """You are a private English tutor. Analyze the following transcribed text. Identify any grammatical errors, suggest better vocabulary, and generate one short multiple-choice quiz question to help the user improve based on their mistakes. Output only the corrections and the quiz."""
    }

    private var llmInference: LlmInference? = null
    private var isInitialized = false

    /**
     * Attempts to find and load a model file.
     * Returns true if initialization succeeds.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true

        val modelPath = findModelPath()
        if (modelPath == null) {
            Log.w(TAG, "No model file found. LLM features will be unavailable.")
            return@withContext false
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            Log.i(TAG, "LLM initialized successfully with model: $modelPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LLM: ${e.message}", e)
            false
        }
    }

    /**
     * Search for a model file in several locations.
     */
    private fun findModelPath(): String? {
        val candidates = listOf(
            DEFAULT_MODEL_PATH,
            getAppModelPath(context),
            "/sdcard/llm/model.bin",
            "/data/local/tmp/llm/gemma-2b-it-gpu-int4.bin",
        )
        return candidates.firstOrNull { File(it).exists() }
    }

    /**
     * Generate a response from the LLM given user's transcribed speech.
     */
    suspend fun analyzeText(transcribedText: String): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            return@withContext generateFallbackResponse(transcribedText)
        }

        try {
            val prompt = buildPrompt(transcribedText)
            val response = llmInference!!.generateResponse(prompt)
            response.trim().ifEmpty {
                generateFallbackResponse(transcribedText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM inference failed: ${e.message}", e)
            generateFallbackResponse(transcribedText)
        }
    }

    private fun buildPrompt(transcribedText: String): String {
        return "$SYSTEM_PROMPT\n\nTranscribed text: \"$transcribedText\""
    }

    /**
     * Smart rule-based fallback when no LLM model is loaded.
     * Provides basic grammar checks and vocabulary suggestions.
     */
    private fun generateFallbackResponse(text: String): String {
        val issues = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // Basic grammar checks
        val sentences = text.split(Regex("[.!?]+")).filter { it.isNotBlank() }

        for (sentence in sentences) {
            val trimmed = sentence.trim()

            // Check capitalization
            if (trimmed.isNotEmpty() && trimmed[0].isLowerCase()) {
                issues.add("• Capitalize the beginning of the sentence: \"${trimmed.take(20)}...\"")
            }

            // Check common errors
            val lowerSentence = trimmed.lowercase()
            val commonErrors = mapOf(
                "i " to "The pronoun 'I' should always be capitalized",
                "your welcome" to "Use 'you're welcome' (you are welcome)",
                "could of" to "Use 'could have' instead of 'could of'",
                "should of" to "Use 'should have' instead of 'should of'",
                "would of" to "Use 'would have' instead of 'would of'",
                "their is" to "Use 'there is' for existence, 'their' is possessive",
                "there going" to "Use 'they're going' (they are going)",
                "alot" to "Write 'a lot' as two separate words",
                "definately" to "Correct spelling: 'definitely'",
                "seperate" to "Correct spelling: 'separate'",
                "recieve" to "Correct spelling: 'receive' (i before e, except after c)",
            )

            for ((error, correction) in commonErrors) {
                if (lowerSentence.contains(error)) {
                    issues.add("• $correction")
                }
            }
        }

        // Vocabulary suggestions
        val simpleWords = mapOf(
            "good" to "excellent, outstanding, remarkable",
            "bad" to "terrible, dreadful, inadequate",
            "big" to "enormous, substantial, considerable",
            "small" to "diminutive, compact, modest",
            "happy" to "delighted, elated, overjoyed",
            "sad" to "melancholy, somber, disheartened",
            "nice" to "pleasant, delightful, agreeable",
            "very" to "extremely, remarkably, exceptionally",
            "said" to "mentioned, remarked, stated",
            "got" to "obtained, acquired, received",
        )

        val words = text.lowercase().split(Regex("\\s+"))
        for ((simple, alternatives) in simpleWords) {
            if (words.contains(simple)) {
                suggestions.add("• Instead of '$simple', consider: $alternatives")
            }
        }

        val sb = StringBuilder()
        sb.appendLine("═══ Grammar Analysis ═══")

        if (issues.isEmpty() && suggestions.isEmpty()) {
            sb.appendLine("✅ Great job! No obvious grammatical issues detected.")
            sb.appendLine()
            sb.appendLine("Your sentence structure looks good. Keep practicing to maintain your fluency!")
        } else {
            if (issues.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("🔍 Issues Found:")
                issues.forEach { sb.appendLine(it) }
            }
            if (suggestions.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("💡 Vocabulary Suggestions:")
                suggestions.forEach { sb.appendLine(it) }
            }
        }

        // Generate quiz
        sb.appendLine()
        sb.appendLine("═══ Quick Quiz ═══")
        sb.appendLine()

        val quizzes = listOf(
            Triple(
                "Which is correct?",
                listOf("A) Their going to the store", "B) They're going to the store", "C) There going to the store"),
                "B"
            ),
            Triple(
                "Choose the best word: 'The movie was really ___.'",
                listOf("A) good", "B) nice", "C) captivating"),
                "C"
            ),
            Triple(
                "Which sentence is grammatically correct?",
                listOf("A) I could of done better", "B) I could have done better", "C) I could has done better"),
                "B"
            ),
            Triple(
                "Fill in the blank: 'She ___ to the party last night.'",
                listOf("A) goed", "B) gone", "C) went"),
                "C"
            ),
            Triple(
                "Which is the correct spelling?",
                listOf("A) definately", "B) definitly", "C) definitely"),
                "C"
            ),
        )

        val quiz = quizzes.random()
        sb.appendLine("❓ ${quiz.first}")
        quiz.second.forEach { sb.appendLine("   $it") }
        sb.appendLine()
        sb.appendLine("Answer: ${quiz.third}")

        sb.appendLine()
        sb.appendLine("───────────────────")
        sb.appendLine("ℹ️ Note: Using built-in analysis. For deeper AI-powered corrections, load a Gemma model to the device.")

        return sb.toString()
    }

    fun close() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LLM: ${e.message}")
        }
        llmInference = null
        isInitialized = false
    }

    fun isModelLoaded(): Boolean = isInitialized
}
