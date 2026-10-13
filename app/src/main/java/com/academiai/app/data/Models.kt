package com.academiai.app.data

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class StudentAlert(
    val studentName: String,
    val riskLevel: RiskLevel,
    val detail: String,
    val timestamp: String,
    val avatarInitial: String,
    val uid: String = ""
)

enum class RiskLevel(val label: String, val emoji: String) {
    HIGH("Alto Riesgo", "🔴"),
    MEDIUM("Riesgo Medio", "🟡"),
    LOW("Estable", "🟢")
}

data class Evaluation(
    val subject: String,
    val type: String,
    val date: String,
    val daysLeft: Int
)

// Ollama API
data class OllamaRequest(
    val model: String = "llama3.2:3b",
    val prompt: String,
    val system: String = SYSTEM_PROMPT,
    val stream: Boolean = true
)

data class OllamaChunk(
    val response: String = "",
    val done: Boolean = false
)

const val SYSTEM_PROMPT = """Eres AcademiAI, un tutor académico virtual empático y cercano de una institución de educación superior. Tu rol es:

1. Acompañar al estudiante de primer año en su adaptación universitaria
2. Ayudarlo a organizar su tiempo de estudio y preparar evaluaciones
3. Resolver dudas académicas de forma clara y paciente
4. Brindar contención emocional inicial cuando el estudiante manifieste estrés, ansiedad o ganas de abandonar la carrera
5. NUNCA dar diagnósticos de salud mental — si detectas señales graves, recomienda al estudiante acudir al servicio de bienestar estudiantil

Usa un tono cálido, cercano y motivador. Tutea al estudiante. Usa emojis con moderación para hacer la conversación más amigable. Responde de forma concisa (máximo 3 párrafos cortos)."""
