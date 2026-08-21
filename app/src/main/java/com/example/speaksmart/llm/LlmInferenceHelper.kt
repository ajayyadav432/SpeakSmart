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

        // System prompt for direct Chat
        const val CHAT_SYSTEM_PROMPT = """You are SpeakSmart AI, an expert, encouraging, and friendly English Language Tutor. Answer the user's questions about English grammar, vocabulary, pronunciation, idioms, sentence structure, or general conversation practice concisely, accurately, and helpful for language learners."""
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

        // Try GPU backend first, fallback to CPU backend (for emulators without OpenCL support)
        val backendsToTry = listOf(LlmInference.Backend.GPU, LlmInference.Backend.CPU)

        for (backend in backendsToTry) {
            try {
                Log.d(TAG, "Attempting LLM initialization with backend: $backend")
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setPreferredBackend(backend)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                isInitialized = true
                Log.i(TAG, "LLM initialized successfully with backend $backend and model: $modelPath")
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "Failed initialization with backend $backend: ${e.message}")
            }
        }

        Log.e(TAG, "Failed to initialize LLM with all backends for model: $modelPath")
        false
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

    /**
     * Generate direct LLM chat response for user's question or conversation.
     */
    suspend fun generateChatResponse(
        userPrompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            return@withContext generateFallbackChatResponse(userPrompt)
        }

        try {
            val promptBuilder = StringBuilder()
            promptBuilder.appendLine(CHAT_SYSTEM_PROMPT)
            promptBuilder.appendLine()
            for ((userMsg, aiMsg) in history.takeLast(4)) {
                promptBuilder.appendLine("User: $userMsg")
                promptBuilder.appendLine("SpeakSmart AI: $aiMsg")
            }
            promptBuilder.appendLine("User: $userPrompt")
            promptBuilder.appendLine("SpeakSmart AI:")

            val response = llmInference!!.generateResponse(promptBuilder.toString())
            response.trim().ifEmpty {
                generateFallbackChatResponse(userPrompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM chat generation failed: ${e.message}", e)
            generateFallbackChatResponse(userPrompt)
        }
    }

    /**
     * Intelligent English Tutor fallback for direct LLM Chat queries.
     */
    private fun generateFallbackChatResponse(prompt: String): String {
        val lower = prompt.lowercase().trim()

        return when {
            lower.contains("affect") && lower.contains("effect") -> {
                """
                ✨ **Affect vs. Effect**
                
                • **Affect** (usually a verb): Means to influence or produce a change.
                  *Example:* "The noise affects my concentration."
                
                • **Effect** (usually a noun): Means the result or outcome.
                  *Example:* "The rule change had a positive effect."
                
                💡 *Quick Memory Tip:* **A**ffect = **A**ction (Verb), **E**ffect = **E**nd result (Noun)!
                """.trimIndent()
            }
            lower.contains("present perfect") || lower.contains("have done") || lower.contains("has done") -> {
                """
                📚 **Present Perfect Tense Guide**
                
                • **Formula:** `Subject + have/has + Past Participle (V3)`
                • **Usage:** Connects a past action with the present moment.
                
                Examples:
                1. "I **have lived** in Tokyo for 3 years." (I still live there)
                2. "She **has finished** her homework." (It's completed now)
                
                💡 *Compare:* "I lived in Tokyo" (Simple Past - action ended in past).
                """.trimIndent()
            }
            lower.contains("quiz") || lower.contains("test me") || lower.contains("question") -> {
                """
                🎓 **English Practice Quiz**
                
                **Question 1:**
                Choose the correct sentence:
                A) She don't like coffee.
                B) She doesn't like coffee.
                C) She not like coffee.
                
                **Question 2:**
                What is a synonym for 'meticulous'?
                A) Careless
                B) Detailed and careful
                C) Fast
                
                *Reply with your answers (e.g., '1B, 2B') and I will grade them for you!*
                """.trimIndent()
            }
            lower.contains("interview") || lower.contains("job") || lower.contains("work") -> {
                """
                💼 **Job Interview Practice**
                
                Great choice! Let's practice key interview questions in English.
                
                **Question for you:**
                *"Could you introduce yourself and describe one of your key professional strengths?"*
                
                💡 *Tutor Tip:* Use the **STAR** method (Situation, Task, Action, Result) when giving examples!
                
                Go ahead and type your response—I'll review your grammar and word choices!
                """.trimIndent()
            }
            lower.contains("hi") || lower.contains("hello") || lower.contains("hey") -> {
                """
                Hello! 👋 I'm your SpeakSmart AI Tutor.
                
                How can I help your English today? You can:
                • Ask any grammar or vocabulary question
                • Practice conversational topics
                • Ask for a mini-quiz or interview practice
                """.trimIndent()
            }
            else -> {
                """
                🤖 **SpeakSmart AI Answer**
                
                Thank you for your question: "$prompt"
                
                When learning English, pay attention to:
                1. **Sentence Structure**: Keep subjects and verbs in agreement.
                2. **Vocabulary**: Expand your expressions using descriptive adjectives and precise verbs.
                3. **Active Practice**: Regular speaking and writing build lasting fluency!
                
                Feel free to ask me to explain specific words, correct a sentence, or quiz your knowledge!
                """.trimIndent()
            }
        }
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

