package com.academiai.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.academiai.app.ui.screens.*
import com.academiai.app.viewmodel.ChatViewModel

@Composable
fun AcademiAINavGraph() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginStudent = { navController.navigate("student_dashboard") {
                    popUpTo("login") { inclusive = true }
                }},
                onLoginTeacher = { navController.navigate("teacher_dashboard") {
                    popUpTo("login") { inclusive = true }
                }}
            )
        }

        composable("student_dashboard") {
            StudentDashboardScreen(
                onOpenChat = { navController.navigate("chat") },
                onLogout = { navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }}
            )
        }

        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("teacher_dashboard") {
            TeacherDashboardScreen(
                viewModel = chatViewModel,
                onLogout = { navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }}
            )
        }
    }
}
