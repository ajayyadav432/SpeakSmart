package com.crispr.ai.llm

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

        // System prompt for generic Quick Chat (no persona)
        const val QUICK_CHAT_SYSTEM_PROMPT = """You are SpeakSmart AI, a helpful, knowledgeable, and friendly AI assistant that runs fully on-device for complete privacy. Answer user questions across any topic — writing, learning, productivity, general knowledge, advice — concisely and helpfully. Never send data externally."""
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
     * Generate generic quick-chat response (no persona, general purpose AI assistant).
     */
    suspend fun generateQuickChatResponse(
        userPrompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            return@withContext generateFallbackQuickChat(userPrompt)
        }
        try {
            val promptBuilder = StringBuilder()
            promptBuilder.appendLine(QUICK_CHAT_SYSTEM_PROMPT)
            promptBuilder.appendLine()
            for ((userMsg, aiMsg) in history.takeLast(6)) {
                promptBuilder.appendLine("User: $userMsg")
                promptBuilder.appendLine("SpeakSmart AI: $aiMsg")
            }
            promptBuilder.appendLine("User: $userPrompt")
            promptBuilder.appendLine("SpeakSmart AI:")
            val response = llmInference!!.generateResponse(promptBuilder.toString())
            response.trim().ifEmpty { generateFallbackQuickChat(userPrompt) }
        } catch (e: Exception) {
            Log.e(TAG, "Quick chat LLM failed: ${e.message}", e)
            generateFallbackQuickChat(userPrompt)
        }
    }

    private fun generateFallbackQuickChat(prompt: String): String {
        val lower = prompt.lowercase().trim()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hi there! 👋 I'm SpeakSmart AI — your fully private, on-device assistant. Ask me anything! I'm great for writing help, learning, brainstorming, and general knowledge."
            lower.contains("write") || lower.contains("draft") || lower.contains("email") ->
                """✍️ **Writing Assistant**

Happy to help you write! For best results, tell me:
• **Type**: Email / Essay / Message / Cover Letter?
• **Tone**: Formal, casual, professional, friendly?
• **Length**: Short, medium, or detailed?

Share your draft or topic and I'll craft it for you!"""
            lower.contains("summarize") || lower.contains("summary") ->
                """📋 **Summarization Mode**

Paste the text you'd like summarized and I'll condense it into:
• A 3-5 bullet point summary
• Or a short paragraph overview

Your text stays 100% on-device — never sent anywhere!"""
            lower.contains("brainstorm") || lower.contains("idea") || lower.contains("help me think") ->
                """💡 **Brainstorm Mode Activated!**

I'm ready to generate ideas with you. Tell me:
• What topic or problem are you working on?
• Any constraints (budget, time, audience)?

Let's think through it together!"""
            lower.contains("who are you") || lower.contains("what can you do") ->
                """🤖 **About SpeakSmart AI**

I'm a fully on-device AI assistant — meaning:
• ✅ **Zero internet required** — works offline
• ✅ **100% private** — no data ever sent externally
• ✅ **No accounts, no tracking**

I can help with: writing, learning, brainstorming, translation, answering questions, and much more!"""
            else ->
                """🤖 **SpeakSmart AI (On-Device)**

Your question: "$prompt"

I'm currently running in **smart fallback mode** (LLM model not loaded). For full AI-powered responses, download the on-device model from the settings.

💡 **Quick Tip:** Even in fallback mode, try asking me about writing, English, brainstorming, or learning topics!"""
        }
    }

    /**
     * Generate direct LLM chat response for user's question or conversation using selected Agent Persona.
     */
    suspend fun generateChatResponse(
        userPrompt: String,
        history: List<Pair<String, String>> = emptyList(),
        persona: com.crispr.ai.data.AiAgentPersona = com.crispr.ai.data.AiAgentPersona.ENGLISH_COACH
    ): String = withContext(Dispatchers.IO) {
        if (!isInitialized || llmInference == null) {
            return@withContext generateFallbackChatResponse(userPrompt, persona)
        }

        try {
            val promptBuilder = StringBuilder()
            promptBuilder.appendLine(persona.systemPrompt)
            promptBuilder.appendLine()
            for ((userMsg, aiMsg) in history.takeLast(4)) {
                promptBuilder.appendLine("User: $userMsg")
                promptBuilder.appendLine("${persona.title}: $aiMsg")
            }
            promptBuilder.appendLine("User: $userPrompt")
            promptBuilder.appendLine("${persona.title}:")

            val response = llmInference!!.generateResponse(promptBuilder.toString())
            response.trim().ifEmpty {
                generateFallbackChatResponse(userPrompt, persona)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LLM chat generation failed: ${e.message}", e)
            generateFallbackChatResponse(userPrompt, persona)
        }
    }

    /**
     * Intelligent AI Agent fallback for direct LLM Chat queries across all 6 specialized personas.
     */
    private fun generateFallbackChatResponse(
        prompt: String,
        persona: com.crispr.ai.data.AiAgentPersona
    ): String {
        val lower = prompt.lowercase().trim()

        return when (persona.id) {
            "deep_reflection" -> {
                when {
                    lower.contains("anxious") || lower.contains("reframe") || lower.contains("worry") -> {
                        """
                        🌿 **Cognitive Reframing Thought Record**
                        
                        1. **Identify the Core Thought**: What exact situation triggered this anxiety?
                        2. **Examine Evidence**: Is this thought 100% factually true, or is your mind catastrophizing?
                        3. **Balanced Reframe**: *"I am facing a challenge, but I have navigated difficult situations before, and I can handle this step-by-step."*
                        
                        💭 *Reflective Question for you:* What is one small element of this situation that remains fully within your control today?
                        """.trimIndent()
                    }
                    lower.contains("journal") || lower.contains("5-minute") || lower.contains("reflect") -> {
                        """
                        📝 **5-Minute Reflection Journal Prompt**
                        
                        Take a quiet breath and answer these 3 quick reflections:
                        
                        1. **Current State:** What emotion is currently present in your body (e.g., tension, calmness, fatigue)?
                        2. **Gratitude:** What is one simple thing that brought you comfort or ease today?
                        3. **Intention:** What is one gentle goal for the rest of your day?
                        
                        *Feel free to write your answers right here when you're ready.*
                        """.trimIndent()
                    }
                    else -> {
                        """
                        🌿 **Deep Reflection Response**
                        
                        Thank you for sharing your thoughts: "$prompt"
                        
                        Remember that emotions are temporary signals, not permanent truths. Giving yourself space to write and process thoughts without judgment is a powerful step for mental clarity.
                        
                        Would you like to explore what emotions lie beneath this thought, or try a 2-minute grounding exercise together?
                        """.trimIndent()
                    }
                }
            }
            "deal_coach" -> {
                when {
                    lower.contains("price") || lower.contains("expensive") || lower.contains("objection") -> {
                        """
                        💼 **Price Objection Defense Strategy**
                        
                        1. **Acknowledge without conceding**: *"I understand budget constraints are top of mind for your team."*
                        2. **Pivot from Price to ROI**: *"Let's examine the risk of non-action versus the projected 3.5x ROI our solution delivers."*
                        3. **Isolate the Objection**: *"If we can demonstrate clear payback within 90 days, are there any other obstacles to closing this deal today?"*
                        
                        🎯 *Next Action:* Never discount without receiving a scope trade-off in return!
                        """.trimIndent()
                    }
                    lower.contains("contract") || lower.contains("clause") || lower.contains("risk") -> {
                        """
                        📜 **Contract Negotiation Playbook**
                        
                        Key focus areas for commercial agreements:
                        • **Limitation of Liability**: Cap liability to 12 months of fees paid.
                        • **Termination for Convenience**: Require 30-60 days written notice with non-refundable deposit.
                        • **Payment Terms**: Net 30 with automatic late interest penalty.
                        
                        💡 *Tactical Tip:* Keep your core IP and indemnity clauses unyielding; negotiate on payment schedules or SLA response windows instead.
                        """.trimIndent()
                    }
                    else -> {
                        """
                        💼 **Confidential Deal Coach Strategy**
                        
                        Re: "$prompt"
                        
                        In deal closing and negotiation, leverage comes from:
                        1. **Clear Alternatives (BATNA)**: Know your walkaway threshold before entering the conversation.
                        2. **Silence & Anchoring**: Let the opposing party make the first concession after you set your term anchor.
                        3. **Value Alignment**: Tie every feature directly to their top strategic KPI.
                        
                        What specific terms or timeline objections are you facing from their decision maker?
                        """.trimIndent()
                    }
                }
            }
            "financial_auditor" -> {
                when {
                    lower.contains("50/30/20") || lower.contains("budget") || lower.contains("analyze") -> {
                        """
                        📊 **Hyper-Private Budget Audit (50/30/20 Rule)**
                        
                        • **50% Needs**: Essential living expenses (Rent, Utilities, Groceries, Minimum Debt).
                        • **30% Wants**: Lifestyle, Dining out, Entertainment, Subscriptions.
                        • **20% Savings & Debt Acceleration**: High-yield savings, Investments, Debt payoff.
                        
                        💡 *Audit Tip:* If your Needs exceed 50%, audit fixed monthly contracts (insurance, internet, utility plans) before cutting essential quality-of-life needs.
                        """.trimIndent()
                    }
                    lower.contains("debt") || lower.contains("snowball") || lower.contains("avalanche") -> {
                        """
                        📉 **Debt Payoff Optimization**
                        
                        • **Avalanche Method (Mathematically Optimal)**: Pay off highest interest rate balance first (saves the most money).
                        • **Snowball Method (Psychologically Rewarding)**: Pay off smallest total balance first (builds quick momentum).
                        
                        🛡️ *Auditor Recommendation:* Maintain a $1,000 mini emergency reserve first so unexpected repairs don't push you back onto credit cards!
                        """.trimIndent()
                    }
                    else -> {
                        """
                        📊 **Financial Audit Report**
                        
                        Analysis for: "$prompt"
                        
                        To maintain financial health and privacy:
                        1. **Track Cash Flow Leakage**: Audit small recurring subscriptions and recurring automated charges.
                        2. **Automate Savings**: Move 15-20% of net income to high-yield accounts on payday before discretionary spending.
                        3. **Asset Protection**: Keep 3 to 6 months of living expenses liquid in FDIC-insured high-yield savings.
                        """.trimIndent()
                    }
                }
            }
            "second_brain" -> {
                when {
                    lower.contains("clean") || lower.contains("micro") || lower.contains("break down") -> {
                        """
                        🧠 **Executive Function Micro-Task Breakdown**
                        
                        Let's break down this task so you don't feel overwhelmed:
                        
                        1. 🟢 **Step 1 (30 secs):** Walk over and pick up 3 pieces of trash on the floor.
                        2. 🟡 **Step 2 (2 mins):** Put all dirty clothes into a laundry hamper.
                        3. 🔵 **Step 3 (2 mins):** Clear only the surface of your desk or bed.
                        
                        ⚡ *Rule:* Stop right after Step 1 if you want to! Starting is the only win we care about today.
                        """.trimIndent()
                    }
                    lower.contains("stuck") || lower.contains("paralysis") || lower.contains("start") -> {
                        """
                        ⚡ **ADHD Task Paralysis Emergency Protocol**
                        
                        1. **Lower the Bar to Zero:** Tell yourself: *"I am only going to work on this for 120 seconds."*
                        2. **Change Physical Location:** Stand up, stretch your arms, or grab a glass of cold water.
                        3. **Body Doubling:** Keep me open while you do the first 2 minutes.
                        
                        Ready? Set a timer for 2 minutes right now and do the smallest possible action!
                        """.trimIndent()
                    }
                    else -> {
                        """
                        🧠 **Second Brain Task Organizer**
                        
                        Re: "$prompt"
                        
                        When your brain feels full or stuck:
                        1. **Brain Dump:** Get all open loops out of your head onto screen/paper.
                        2. **Pick ONE Priority:** Hide everything else from view.
                        3. **Time-box:** Work in 15-minute sprints with guaranteed rest breaks.
                        
                        What is the ONE smallest action step you can take in the next 5 minutes?
                        """.trimIndent()
                    }
                }
            }
            "travel_translator" -> {
                when {
                    lower.contains("japan") || lower.contains("food") || lower.contains("phrases") -> {
                        """
                        ✈️ **Japan Dining & Etiquette Quick Guide**
                        
                        **Essential Phrases:**
                        • *"Sumimasen"* (soo-mee-mah-sen) - Excuse me / Sorry
                        • *"Arigatou gozaimasu"* (ah-ree-gah-toe go-zah-ee-mah-soo) - Thank you very much
                        • *"O-kaikei o onegai shimasu"* (oh-kye-kay oh oh-nay-gai she-mah-soo) - Check/bill please
                        
                        🌸 **Cultural Tip:** Never stick your chopsticks vertically into a bowl of rice (it is associated with funeral rites). Rest them on the chopstick holder (*hashi-oki*).
                        """.trimIndent()
                    }
                    lower.contains("emergency") || lower.contains("spanish") || lower.contains("french") -> {
                        """
                        🚨 **Emergency Travel Phrases**
                        
                        **Spanish:**
                        • *"¡Necesito ayuda!"* (Neh-seh-SEE-toh ah-YOO-dah) - I need help!
                        • *"¿Dónde está el hospital?"* (DON-deh ess-TAH el oss-pee-TAL) - Where is the hospital?
                        
                        **French:**
                        • *"Au secours !"* (Oh suh-KOOR) - Help!
                        • *"Où sont les toilettes ?"* (Oo son lay twah-LETT) - Where is the restroom?
                        """.trimIndent()
                    }
                    else -> {
                        """
                        ✈️ **Off-Grid Cultural Guide**
                        
                        Query: "$prompt"
                        
                        **Universal Travel Rules:**
                        1. Learn basic greeting and gratitude phrases in the local language—locals deeply appreciate the effort.
                        2. Respect dress codes at religious or sacred sites (shoulder & knee coverage).
                        3. Keep offline maps and localized currency conversion notes saved on device.
                        
                        Where are you traveling next, or what specific phrase do you need translated?
                        """.trimIndent()
                    }
                }
            }
            "medical_advisor" -> {
                when {
                    lower.contains("headache") || lower.contains("head pain") || lower.contains("migraine") -> {
                        """
                        🩺 **Understanding Headaches — Educational Overview**

                        Common headache types and potential causes:
                        • **Tension Headache**: Stress, poor posture, dehydration, eye strain. Most common type.
                        • **Migraine**: Intense throbbing pain, often with light/sound sensitivity. May last 4–72 hours.
                        • **Cluster Headache**: Severe pain around one eye, often occurring in cycles.

                        💡 **General Wellness Tips:**
                        1. Drink 8+ glasses of water daily
                        2. Take regular screen breaks (20-20-20 rule)
                        3. Maintain consistent sleep schedules

                        ⚠️ *Seek immediate medical care if headache is sudden, severe (thunderclap), or accompanied by fever, stiff neck, or vision changes.*
                        """.trimIndent()
                    }
                    lower.contains("tired") || lower.contains("fatigue") || lower.contains("exhausted") || lower.contains("no energy") -> {
                        """
                        😴 **Understanding Fatigue — Educational Overview**

                        Common causes of persistent tiredness:
                        • **Sleep Issues**: Inadequate sleep (< 7-9 hrs), sleep apnea, poor sleep quality
                        • **Nutritional**: Iron deficiency (anemia), low Vitamin D/B12, dehydration
                        • **Lifestyle**: Sedentary habits, high stress, excessive screen time
                        • **Medical**: Thyroid disorders, diabetes, or other conditions (requires professional evaluation)

                        💡 **Self-Care Steps to Try:**
                        1. Prioritize 7-9 hours of consistent sleep
                        2. Stay hydrated throughout the day
                        3. Take short daily walks for energy

                        ⚠️ *Consult a doctor if fatigue has persisted for 2+ weeks or is severe.*
                        """.trimIndent()
                    }
                    lower.contains("medication") || lower.contains("medicine") || lower.contains("drug") || lower.contains("interaction") -> {
                        """
                        💊 **Medication Safety — Educational Overview**

                        Key principles for safe medication use:
                        • **Always follow prescribed dosages** — never double-dose or self-medicate
                        • **Common interaction risk categories:**
                          - Blood thinners + NSAIDs (e.g., ibuprofen) → bleeding risk
                          - Antidepressants + certain supplements → serotonin sensitivity
                          - Antibiotics + alcohol → reduced effectiveness, nausea
                        • **Ask your pharmacist or doctor** about any new medication before starting it

                        💡 Always keep an updated list of your current medications to share with healthcare providers.

                        ⚠️ *For specific drug interactions, consult a licensed pharmacist or your prescribing physician.*
                        """.trimIndent()
                    }
                    lower.contains("diet") || lower.contains("nutrition") || lower.contains("healthy eating") || lower.contains("food") -> {
                        """
                        🥗 **Healthy Eating Foundations**

                        **Balanced Plate Guide:**
                        • 🥦 **½ Plate**: Non-starchy vegetables (leafy greens, broccoli, peppers)
                        • 🌾 **¼ Plate**: Whole grains (brown rice, oats, quinoa)
                        • 🍗 **¼ Plate**: Lean protein (chicken, fish, legumes, tofu)
                        • 💧 **Hydration**: Aim for 8-10 glasses of water daily

                        **Foods to Limit:**
                        Ultra-processed foods, sugary drinks, refined carbohydrates, excessive sodium.

                        ⚠️ *For personalized nutrition plans, consult a registered dietitian.*
                        """.trimIndent()
                    }
                    else -> {
                        """
                        🏥 **Private Health Advisor — Educational Response**

                        Regarding: "$prompt"

                        As your private health information guide, I can help you:
                        • Understand general symptoms and their common causes
                        • Learn about healthy lifestyle habits and wellness practices
                        • Explain medical terminology in plain language
                        • Prepare questions to ask your healthcare provider

                        📋 **Remember**: This information is educational, not a substitute for professional medical advice. Your data stays 100% on-device.

                        ⚠️ *Always consult a qualified healthcare professional for diagnosis, treatment, or any specific medical concern.*
                        """.trimIndent()
                    }
                }
            }
            else -> {
                // English Coach
                when {
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
                    else -> {
                        """
                        🤖 **English Improvement Coach**
                        
                        Thank you for your question: "$prompt"
                        
                        When practicing English, focus on:
                        1. **Sentence Structure**: Clear subject-verb agreement.
                        2. **Vocabulary Expansion**: Use vivid adjectives and precise verbs.
                        3. **Consistency**: Daily practice builds natural fluency.
                        """.trimIndent()
                    }
                }
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

