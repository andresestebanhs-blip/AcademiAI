package com.academiai.app.data

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "AcademiAISettings"
    private const val KEY_SERVER_IP = "server_ip"
    
    // Default IP points to localhost from Android Emulator
    private const val DEFAULT_IP = "10.0.2.2"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var serverIp: String
        get() = prefs.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
        set(value) {
            prefs.edit().putString(KEY_SERVER_IP, value).apply()
        }

    // Retorna la URL base completa para el backend
    val backendUrl: String
        get() {
            return if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
                serverIp
            } else {
                "http://$serverIp:8000"
            }
        }
}
