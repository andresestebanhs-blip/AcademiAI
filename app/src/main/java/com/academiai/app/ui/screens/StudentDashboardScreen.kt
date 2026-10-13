package com.academiai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academiai.app.data.Evaluation
import com.academiai.app.ui.theme.*
import kotlinx.coroutines.launch
import com.academiai.app.data.BackendClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(onOpenChat: () -> Unit, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("Estudiante") }
    var evals by remember { mutableStateOf<List<Evaluation>>(emptyList()) }
    var mood by remember { mutableStateOf("Bien") }
    var selectedTab by remember { mutableStateOf("Inicio") }

    LaunchedEffect(Unit) {
        val result = BackendClient.getStudentDashboard()
        if (result.isSuccess) {
            val data = result.getOrNull()
            if (data != null) {
                val user = data["user"] as? Map<*, *>
                if (user != null) {
                    name = user["name"] as? String ?: "Estudiante"
                    mood = (user["mood"] as? String)?.replaceFirstChar { it.uppercase() } ?: "Bien"
                }
                val evalsList = data["evaluations"] as? List<Map<*, *>>
                if (evalsList != null) {
                    val parsedEvals = evalsList.mapNotNull { e ->
                        try {
                            val subj = e["subject"] as? String ?: "Materia"
                            val type = e["type"] as? String ?: "Evaluación"
                            val dateStr = e["date"] as? String ?: return@mapNotNull null
                            
                            var daysLeft = 0
                            try {
                                val targetDate = LocalDate.parse(dateStr)
                                val today = LocalDate.now()
                                daysLeft = ChronoUnit.DAYS.between(today, targetDate).toInt()
                            } catch (ex: Exception) { }
                            
                            Evaluation(subj, type, dateStr, daysLeft)
                        } catch (ex: Exception) { null }
                    }.sortedBy { it.daysLeft }
                    evals = parsedEvals
                }
            }
        }
    }

    Scaffold(
        containerColor = Navy,
        floatingActionButton = {
            if (selectedTab == "Inicio") {
                FloatingActionButton(onClick = onOpenChat, containerColor = Purple, shape = CircleShape) {
                    Icon(Icons.Default.Chat, "Chat", tint = TextWhite)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = NavyLight) {
                NavigationBarItem(selectedTab == "Inicio", { selectedTab = "Inicio" }, { Icon(Icons.Default.Home, "Inicio") }, label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple, selectedTextColor = Purple, indicatorColor = Purple.copy(0.15f), unselectedIconColor = TextMuted, unselectedTextColor = TextMuted))
                NavigationBarItem(false, onOpenChat, { Icon(Icons.Default.Chat, "Chat") }, label = { Text("Chat IA") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextMuted, unselectedTextColor = TextMuted))
                NavigationBarItem(selectedTab == "Agenda", { selectedTab = "Agenda" }, { Icon(Icons.Default.CalendarMonth, "Agenda") }, label = { Text("Agenda") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple, selectedTextColor = Purple, indicatorColor = Purple.copy(0.15f), unselectedIconColor = TextMuted, unselectedTextColor = TextMuted))
                NavigationBarItem(selectedTab == "Perfil", { selectedTab = "Perfil" }, { Icon(Icons.Default.Person, "Perfil") }, label = { Text("Perfil") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple, selectedTextColor = Purple, indicatorColor = Purple.copy(0.15f), unselectedIconColor = TextMuted, unselectedTextColor = TextMuted))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (selectedTab == "Inicio") {
                Text("Hola, $name 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Text("Tu resumen de hoy", fontSize = 14.sp, color = TextGray)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("😊", "Ánimo", mood, GreenStatus, Modifier.weight(1f))
                    StatCard("📝", "Próximas", "${evals.size}", AmberStatus, Modifier.weight(1f))
                    StatCard("🤖", "Sesiones", "12", Purple, Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))
                Text("¿Cómo te sientes hoy?", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextWhite)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val moods = listOf("Bien" to "😊", "Regular" to "😐", "Estresado" to "😟", "Mal" to "😫")
                    moods.forEach { (m, emoji) ->
                        val isSelected = mood.equals(m, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Purple else NavyCard)
                                .clickable {
                                    mood = m
                                    scope.launch { BackendClient.updateMood(m.lowercase()) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text("📅 Próximas Evaluaciones", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                Spacer(Modifier.height(12.dp))
                evals.forEach { e ->
                    val c = if (e.daysLeft <= 3) RedStatus else if (e.daysLeft <= 7) AmberStatus else GreenStatus
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.subject, fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 15.sp)
                                Text(e.type, color = TextGray, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(e.date, color = TextGray, fontSize = 12.sp)
                                Text("${e.daysLeft} días", fontWeight = FontWeight.Bold, color = c, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("💡 Recomendaciones de AcademiAI", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                Spacer(Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                    Column(Modifier.padding(16.dp)) {
                        val recommendations = if (evals.isNotEmpty()) {
                            val closest = evals.first()
                            listOf(
                                "📚 Dedica 2h a ${closest.subject} hoy — es en ${closest.daysLeft} días",
                                "⏰ Descansa 10 min cada 45 min de estudio",
                                "🏃 Camina 15 min para reducir el estrés"
                            )
                        } else {
                            listOf(
                                "📚 Revisa si tienes nuevo material en tus cursos",
                                "⏰ Mantén un horario regular de estudio",
                                "🏃 Camina 15 min para reducir el estrés"
                            )
                        }
                        recommendations.forEach {
                            Text(it, color = TextGray, fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            } else if (selectedTab == "Agenda") {
                Text("Tu Agenda", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(Modifier.height(20.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Purple, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Sincronización con el calendario académico en desarrollo para la próxima versión.", color = TextGray, fontSize = 14.sp)
                    }
                }
            } else if (selectedTab == "Perfil") {
                Text("Perfil de Usuario", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Spacer(Modifier.height(20.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Purple, Violet))), contentAlignment = Alignment.Center) {
                            Text(name.firstOrNull()?.toString()?.uppercase() ?: "U", fontSize = 36.sp, color = TextWhite)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(name, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Estudiante", color = TextGray, fontSize = 14.sp)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = RedStatus), shape = RoundedCornerShape(12.dp)) {
                            Text("Cerrar Sesión")
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatCard(emoji: String, label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier.heightIn(min = 90.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = TextGray)
        }
    }
}
