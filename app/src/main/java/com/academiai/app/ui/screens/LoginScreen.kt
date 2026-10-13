package com.academiai.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academiai.app.ui.theme.*
import com.academiai.app.data.SettingsManager
import com.academiai.app.data.BackendClient
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Settings

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun LoginScreen(
    onLoginStudent: () -> Unit,
    onLoginTeacher: () -> Unit
) {
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var tempIp by remember { mutableStateOf(SettingsManager.serverIp) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Settings Icon Top Right
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            IconButton(onClick = { 
                tempIp = SettingsManager.serverIp
                showSettings = true 
            }) {
                Icon(Icons.Default.Settings, "Configuración", tint = TextGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logo area
        Box(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Purple, Violet))),
            contentAlignment = Alignment.Center
        ) {
            Text("🎓", fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AcademiAI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = "Plataforma de Apoyo Académico",
            fontSize = 14.sp,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Role selection
        Text("Selecciona tu rol", color = TextGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RoleCard(
                icon = Icons.Default.School,
                label = "Estudiante",
                selected = selectedRole == "student",
                onClick = { selectedRole = "student" },
                modifier = Modifier.weight(1f)
            )
            RoleCard(
                icon = Icons.Default.Person,
                label = "Docente",
                selected = selectedRole == "teacher",
                onClick = { selectedRole = "teacher" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo institucional") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                unfocusedBorderColor = NavyCard,
                focusedLabelColor = Purple,
                cursorColor = Purple,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                unfocusedBorderColor = NavyCard,
                focusedLabelColor = Purple,
                cursorColor = Purple,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error message
        if (errorMsg != null) {
            Text(errorMsg!!, color = RedStatus, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        }

        // Login button
        Button(
            onClick = {
                if (selectedRole == null) {
                    errorMsg = "Selecciona un rol primero"
                    return@Button
                }
                if (email.isBlank() || password.isBlank()) {
                    errorMsg = "Ingresa correo y contraseña"
                    return@Button
                }
                
                isLoading = true
                errorMsg = null
                
                scope.launch {
                    val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val roleStr = if (selectedRole == "student") "estudiante" else "docente"
                    
                    val result = BackendClient.register(email, password, name, roleStr)
                    isLoading = false
                    
                    if (result.isSuccess) {
                        if (selectedRole == "student") onLoginStudent() else onLoginTeacher()
                    } else {
                        errorMsg = result.exceptionOrNull()?.message ?: "Error al conectar con el servidor"
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Iniciar Sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Prototipo AcademiAI v1.0",
            color = TextMuted,
            fontSize = 12.sp
        )
        
        // Settings Dialog
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                containerColor = NavyCard,
                title = { Text("Configuración del Servidor", color = TextWhite) },
                text = {
                    Column {
                        Text("Ingresa la IP de tu PC (ej: 192.168.1.100).", color = TextGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempIp,
                            onValueChange = { tempIp = it },
                            label = { Text("IP Local") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                unfocusedBorderColor = TextGray,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        SettingsManager.serverIp = tempIp
                        showSettings = false 
                    }) {
                        Text("Guardar", color = Purple)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettings = false }) {
                        Text("Cancelar", color = TextGray)
                    }
                }
            )
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Purple.copy(alpha = 0.2f) else NavyCard
        ),
        border = if (selected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(Purple, Violet))
            )
        } else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) Purple else TextGray, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = if (selected) TextWhite else TextGray, fontWeight = FontWeight.Medium)
        }
    }
}
