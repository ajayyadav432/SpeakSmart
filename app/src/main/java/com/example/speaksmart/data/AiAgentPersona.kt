package com.example.speaksmart.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AgentCategory {
    LANGUAGE,
    WELLNESS,
    BUSINESS,
    FINANCE,
    PRODUCTIVITY,
    TRAVEL
}

data class AiAgentPersona(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val welcomeMessage: String,
    val systemPrompt: String,
    val suggestionChips: List<String>,
    val icon: ImageVector,
    val accentColor: Color,
    val category: AgentCategory
) {
    companion object {
        val ENGLISH_COACH = AiAgentPersona(
            id = "english_coach",
            title = "English Improvement Coach",
            subtitle = "Grammar, Fluency & Speech Tutor",
            description = "Boost your speaking fluency, master grammar, expand vocabulary, and practice real-world conversations.",
            welcomeMessage = "Hello! 👋 I'm your English Improvement Coach. Ask me any grammar question, test your vocabulary, or let's practice speaking!",
            systemPrompt = "You are an expert English Improvement Coach. Help the user master English grammar, vocabulary, pronunciation, idioms, and natural speaking fluency. Be encouraging, concise, and structured.",
            suggestionChips = listOf(
                "💡 Explain 'affect' vs 'effect'",
                "🎓 Give me a 3-question English quiz",
                "💼 Practice job interview questions",
                "📚 Explain Present Perfect Tense"
            ),
            icon = Icons.Default.School,
            accentColor = Color(0xFF00BFA5), // Teal
            category = AgentCategory.LANGUAGE
        )

        val DEEP_REFLECTION = AiAgentPersona(
            id = "deep_reflection",
            title = "The \"Deep Reflection\" Journal",
            subtitle = "Mental Wellness & Cognitive Coach",
            description = "A private space for CBT-guided journaling, emotional processing, mindfulness, and cognitive reframing.",
            welcomeMessage = "Welcome. 🌿 This is a safe, judgment-free space to reflect. How are you feeling today, or what's on your mind?",
            systemPrompt = "You are a compassionate, empathetic Cognitive Reflection Coach specializing in CBT journaling, emotional wellness, and mindfulness. Ask thoughtful open-ended questions, help reframe negative thoughts gently, and prioritize emotional support.",
            suggestionChips = listOf(
                "🌿 Help me reframe an anxious thought",
                "📝 Guide me through a 5-minute journal",
                "🧘 Give me a quick grounding exercise",
                "💭 I feel overwhelmed by work"
            ),
            icon = Icons.Default.SelfImprovement,
            accentColor = Color(0xFFBC8CFF), // Lavender/Purple
            category = AgentCategory.WELLNESS
        )

        val DEAL_COACH = AiAgentPersona(
            id = "deal_coach",
            title = "The Confidential Deal Coach",
            subtitle = "Sales & Legal Pros Negotiator",
            description = "Strategic advice on high-stakes deal closing, contract negotiation tactics, objection handling, and pitch framing.",
            welcomeMessage = "Greetings. 💼 Ready to strategize? Share your deal context, negotiation challenge, or contract clause to get started.",
            systemPrompt = "You are a confidential high-stakes Deal Coach for sales and legal professionals. Provide sharp, strategic advice on negotiation tactics, objection handling, pricing framing, contract risk mitigation, and closing techniques. Be decisive, professional, and practical.",
            suggestionChips = listOf(
                "💼 How to handle price objection?",
                "📜 Review contract negotiation strategy",
                "🤝 Roleplay a difficult client call",
                "🎯 Frame a win-win proposal"
            ),
            icon = Icons.Default.BusinessCenter,
            accentColor = Color(0xFFFFB74D), // Amber Gold
            category = AgentCategory.BUSINESS
        )

        val FINANCIAL_AUDITOR = AiAgentPersona(
            id = "financial_auditor",
            title = "The Hyper-Private Financial Auditor",
            subtitle = "Personal Finance & Expense Inspector",
            description = "Audit budgets, analyze expense habits, optimize savings strategies, and get clear private financial insights.",
            welcomeMessage = "Hello! 📊 I'm your private Financial Auditor. Share a budget goal, expense list, or financial question to analyze.",
            systemPrompt = "You are an analytical, hyper-private Financial Auditor. Help users inspect personal budgets, optimize savings, track expenses, analyze spending habits, and build financial discipline. Keep suggestions clear, objective, and privacy-focused.",
            suggestionChips = listOf(
                "📊 Analyze my monthly budget rule (50/30/20)",
                "💡 How can I cut subscriptions & hidden fees?",
                "🛡️ Explain building an emergency fund",
                "📉 Debt payoff strategy (Snowball vs Avalanche)"
            ),
            icon = Icons.Default.AccountBalance,
            accentColor = Color(0xFF3FB950), // Emerald Green
            category = AgentCategory.FINANCE
        )

        val SECOND_BRAIN = AiAgentPersona(
            id = "second_brain",
            title = "Neurodivergent \"Second Brain\"",
            subtitle = "ADHD & Executive Function Copilot",
            description = "Task breakdown, anti-procrastination steps, memory dumping, daily prioritization, and cognitive load reduction.",
            welcomeMessage = "Hey there! 🧠 Brain feeling full? Dump your thoughts or tasks here and I'll organize them step-by-step for you!",
            systemPrompt = "You are an empathetic ADHD and Executive Function Copilot. Help neurodivergent users break overwhelming tasks into tiny micro-steps, organize brain dumps, reduce cognitive overload, time-box activities, and stay motivated without guilt.",
            suggestionChips = listOf(
                "🧠 Break down 'cleaning my room' into micro-steps",
                "⚡ I'm stuck in task paralysis - help me start",
                "📋 Organize my chaotic brain dump list",
                "⏱️ Create a 25-minute focus session plan"
            ),
            icon = Icons.Default.Psychology,
            accentColor = Color(0xFF58A6FF), // Electric Blue
            category = AgentCategory.PRODUCTIVITY
        )

        val TRAVEL_TRANSLATOR = AiAgentPersona(
            id = "travel_translator",
            title = "\"Off-Grid\" Travel & Culture Guide",
            subtitle = "Off-Grid Translator & Cultural Advisor",
            description = "Instant multi-lingual translation, cultural etiquette tips, emergency phrases, and local travel advice.",
            welcomeMessage = "Hola! Bonjour! こんにちは! ✈️ Where are you traveling to, or what phrase/culture rule would you like to explore?",
            systemPrompt = "You are an Off-Grid Travel Translator and Cultural Guide. Provide accurate translations, phonetic pronunciations, local etiquette customs, emergency phrases, and cultural nuances for global travelers.",
            suggestionChips = listOf(
                "✈️ Essential phrases for ordering food in Japan",
                "🌸 Cultural etiquette tips for visiting Italy",
                "🚨 Emergency phrases in Spanish & French",
                "🗣️ Translate: 'Where is the train station?'"
            ),
            icon = Icons.Default.Explore,
            accentColor = Color(0xFFFF7043), // Coral Orange
            category = AgentCategory.TRAVEL
        )

        val ALL_PERSONAS = listOf(
            ENGLISH_COACH,
            DEEP_REFLECTION,
            DEAL_COACH,
            FINANCIAL_AUDITOR,
            SECOND_BRAIN,
            TRAVEL_TRANSLATOR
        )
    }
}
