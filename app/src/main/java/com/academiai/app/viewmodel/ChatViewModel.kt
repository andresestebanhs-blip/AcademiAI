package com.academiai.app.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.academiai.app.data.ChatMessage
import com.academiai.app.data.BackendClient
import com.academiai.app.data.RiskLevel
import com.academiai.app.data.StudentAlert
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    
    // User ID to send to backend, set after login
    var currentUid: String = "test_user_123"

    val messages = mutableStateListOf<ChatMessage>()
    val isLoading = mutableStateOf(false)
    val alerts = mutableStateListOf<StudentAlert>()
    val currentStreamText = mutableStateOf("")

    init {
        // Welcome message
        messages.add(
            ChatMessage(
                content = "¡Hola! 👋 Soy AcademiAI, tu tutor virtual. Estoy aquí para ayudarte con tus dudas académicas, organizar tu tiempo de estudio o simplemente conversar si necesitas apoyo. ¿En qué puedo ayudarte hoy? 😊",
                isUser = false
            )
        )
    }

    fun loadAlerts() {
        viewModelScope.launch {
            val result = BackendClient.getStudents()
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                val parsedAlerts = list.mapNotNull { map ->
                    try {
                        val name = map["name"] as? String ?: "Desconocido"
                        val levelStr = (map["risk_level"] as? String)?.lowercase() ?: "bajo"
                        val risk = when(levelStr) {
                            "alto" -> RiskLevel.HIGH
                            "medio" -> RiskLevel.MEDIUM
                            else -> RiskLevel.LOW
                        }
                        val moodStr = (map["mood"] as? String) ?: "bien"
                        val detail = "Estado actual: $moodStr"
                        
                        val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
                        val uid = map["uid"] as? String ?: ""
                        
                        StudentAlert(name, risk, detail, "Actual", initial, uid)
                    } catch (e: Exception) { null }
                }
                alerts.clear()
                alerts.addAll(parsedAlerts)
            }
        }
    }

    val studentHistory = mutableStateListOf<ChatMessage>()
    val isLoadingHistory = mutableStateOf(false)

    fun loadStudentHistory(uid: String) {
        if (uid.isBlank()) return
        isLoadingHistory.value = true
        studentHistory.clear()
        
        viewModelScope.launch {
            val result = BackendClient.getStudentHistory(uid)
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                val parsed = list.mapNotNull { map ->
                    try {
                        val content = map["content"] as? String ?: ""
                        val role = map["role"] as? String ?: "user"
                        ChatMessage(content, role == "user")
                    } catch(e: Exception) { null }
                }
                studentHistory.addAll(parsed)
            }
            isLoadingHistory.value = false
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading.value) return

        // Add user message
        messages.add(ChatMessage(content = text, isUser = true))

        // No detectamos estrés aquí, lo hace el backend
        
        // Stream response from Ollama (via backend)
        isLoading.value = true
        currentStreamText.value = ""

        viewModelScope.launch {
            try {
                BackendClient.streamChat(currentUid, text).collect { token ->
                    currentStreamText.value += token
                }
                // Add completed AI message
                messages.add(ChatMessage(content = currentStreamText.value, isUser = false))
                currentStreamText.value = ""
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        content = "⚠️ No pude conectarme al servidor. Asegúrate de que Ollama esté corriendo en tu PC.\n\nError: ${e.localizedMessage}",
                        isUser = false
                    )
                )
            } finally {
                isLoading.value = false
            }
        }
    }
}
