package com.academiai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academiai.app.data.RiskLevel
import com.academiai.app.data.StudentAlert
import com.academiai.app.ui.theme.*
import com.academiai.app.viewmodel.ChatViewModel

import androidx.compose.runtime.*
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(viewModel: ChatViewModel, onLogout: () -> Unit) {
    val alerts = viewModel.alerts
    val highCount = alerts.count { it.riskLevel == RiskLevel.HIGH }
    val medCount = alerts.count { it.riskLevel == RiskLevel.MEDIUM }

    var selectedFilter by remember { mutableStateOf("Todos") }
    val filteredAlerts = alerts.filter {
        when (selectedFilter) {
            "Alto" -> it.riskLevel == RiskLevel.HIGH
            "Medio" -> it.riskLevel == RiskLevel.MEDIUM
            "Bajo" -> it.riskLevel == RiskLevel.LOW
            else -> true
        }
    }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var selectedStudentName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadAlerts()
    }

    Scaffold(
        containerColor = Navy,
        topBar = {
            TopAppBar(
                title = { Text("Panel Docente", fontWeight = FontWeight.Bold, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Salir", tint = TextWhite)
                    }
                },
                actions = {
                    BadgedBox(badge = {
                        if (highCount > 0) Badge(containerColor = RedStatus) { Text("$highCount") }
                    }) {
                        Icon(Icons.Default.Notifications, "Alertas", tint = TextWhite)
                    }
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyLight)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Summary card
            item {
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$highCount", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = RedStatus)
                            Text("Alto Riesgo", fontSize = 12.sp, color = TextGray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$medCount", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AmberStatus)
                            Text("Riesgo Medio", fontSize = 12.sp, color = TextGray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${alerts.size}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Purple)
                            Text("Total", fontSize = 12.sp, color = TextGray)
                        }
                    }
                }
            }

            item {
                Text("Estudiantes Monitoreados", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("Todos", "Alto", "Medio", "Bajo")
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Card(
                            modifier = Modifier.clickable { selectedFilter = filter },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Purple else NavyCard)
                        ) {
                            Text(
                                text = filter,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (isSelected) TextWhite else TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            items(filteredAlerts) { alert ->
                AlertCard(alert) {
                    selectedStudentName = alert.studentName
                    viewModel.loadStudentHistory(alert.uid)
                    showHistoryDialog = true
                }
            }
        }

        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                containerColor = NavyCard,
                title = { Text("Historial: $selectedStudentName", color = TextWhite) },
                text = {
                    if (viewModel.isLoadingHistory.value) {
                        CircularProgressIndicator(color = Purple)
                    } else if (viewModel.studentHistory.isEmpty()) {
                        Text("No hay historial de chat.", color = TextGray)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(viewModel.studentHistory) { msg ->
                                val color = if (msg.isUser) TextWhite else Purple
                                Text(
                                    text = if (msg.isUser) "Estudiante: ${msg.content}" else "AcademiAI: ${msg.content}",
                                    color = color,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("Cerrar", color = Purple)
                    }
                }
            )
        }
    }
}

@Composable
private fun AlertCard(alert: StudentAlert, onViewHistory: () -> Unit) {
    val borderColor = when (alert.riskLevel) {
        RiskLevel.HIGH -> RedStatus
        RiskLevel.MEDIUM -> AmberStatus
        RiskLevel.LOW -> GreenStatus
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(borderColor.copy(0.7f), borderColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(alert.avatarInitial, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 16.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(alert.studentName, fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 15.sp)
                    Text(alert.timestamp, color = TextMuted, fontSize = 11.sp)
                }
                // Risk badge
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = borderColor.copy(alpha = 0.15f))
                ) {
                    Text(
                        "${alert.riskLevel.emoji} ${alert.riskLevel.label}",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Medium, color = borderColor
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(alert.detail, color = TextGray, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewHistory,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Ver Historial", fontSize = 12.sp) }
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Enviar Mensaje", fontSize = 12.sp) }
            }
        }
    }
}
