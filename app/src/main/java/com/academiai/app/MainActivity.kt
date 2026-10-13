package com.academiai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.academiai.app.ui.navigation.AcademiAINavGraph
import com.academiai.app.ui.theme.AcademiAITheme
import com.academiai.app.ui.theme.Navy
import com.academiai.app.data.SettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsManager.init(this)
        enableEdgeToEdge()
        setContent {
            AcademiAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy
                ) {
                    AcademiAINavGraph()
                }
            }
        }
    }
}
