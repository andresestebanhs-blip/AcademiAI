package com.academiai.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

object BackendClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    
    // Store the logged in user's ID
    var currentUid: String? = null

    // Request payload for our new backend
    private data class ChatRequest(val uid: String, val message: String)
    private data class RegisterRequest(val email: String, val password: String, val name: String, val role: String)
    
    // Server-Sent Events (SSE) chunk structure from FastAPI
    private data class SSEChunk(val token: String?, val done: Boolean?)

    fun streamChat(uid: String, message: String): Flow<String> = flow {
        val finalUid = currentUid ?: uid
        val requestBody = ChatRequest(finalUid, message)
        val jsonBody = gson.toJson(requestBody)

        val httpRequest = Request.Builder()
            .url("${SettingsManager.backendUrl}/api/chat")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                emit("⚠️ Error del servidor. Código: ${response.code}")
                return@flow
            }

            // Fallback for instant responses (not streaming)
            val contentType = response.header("Content-Type", "") ?: ""
            if (contentType.contains("application/json")) {
                val bodyStr = response.body?.string()
                if (bodyStr != null) {
                    // Try to parse instant response
                    try {
                        // We expect {"type": "instant", "response": "..."}
                        val map = gson.fromJson(bodyStr, Map::class.java)
                        val resp = map["response"] as? String
                        if (resp != null) {
                            emit(resp)
                            return@flow
                        }
                    } catch (e: Exception) {}
                }
            }

            // Streaming response reading SSE (Server-Sent Events)
            val reader: BufferedReader = response.body?.charStream()?.buffered()
                ?: run {
                    emit("⚠️ Respuesta vacía del servidor.")
                    return@flow
                }

            reader.use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val currentLine = line ?: ""
                    // SSE format: "data: {...}"
                    if (currentLine.startsWith("data: ")) {
                        val jsonStr = currentLine.substring(6).trim()
                        if (jsonStr.isNotEmpty()) {
                            try {
                                val chunk = gson.fromJson(jsonStr, SSEChunk::class.java)
                                if (chunk.token != null) {
                                    emit(chunk.token)
                                }
                                if (chunk.done == true) break
                            } catch (e: Exception) {
                                // Ignore parse errors on incomplete chunks
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("⚠️ No se pudo conectar al servidor.\nRevisa la configuración IP (⚙️) y asegúrate de que el servidor esté corriendo.\nError: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO)
    
    // Register function (suspend, runs on IO dispatcher)
    suspend fun register(email: String, password: String, name: String, role: String): Result<String> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val req = RegisterRequest(email, password, name, role)
                val json = gson.toJson(req)
                val request = Request.Builder()
                    .url("${SettingsManager.backendUrl}/api/register")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()
                    
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (bodyStr != null) {
                        try {
                            val map = gson.fromJson(bodyStr, Map::class.java)
                            val userMap = map["user"] as? Map<*, *>
                            val uid = userMap?.get("uid") as? String
                            if (uid != null) {
                                currentUid = uid
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    Result.success("ok")
                } else {
                    val errorBody = response.body?.string() ?: "Sin detalles"
                    Result.failure(Exception("Error servidor (${response.code}): $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Fetch dashboard data
    suspend fun getStudentDashboard(): Result<Map<String, Any>> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val uid = currentUid ?: return@withContext Result.failure(Exception("No UID"))
            try {
                val request = Request.Builder()
                    .url("${SettingsManager.backendUrl}/api/student/$uid")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (bodyStr != null) {
                        val map = gson.fromJson(bodyStr, Map::class.java)
                        Result.success(map as Map<String, Any>)
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Update Mood
    suspend fun updateMood(mood: String): Result<String> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val uid = currentUid ?: return@withContext Result.failure(Exception("No UID"))
            try {
                val json = gson.toJson(mapOf("uid" to uid, "mood" to mood))
                val request = Request.Builder()
                    .url("${SettingsManager.backendUrl}/api/mood")
                    .put(json.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success("ok")
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get Alerts
    suspend fun getAlerts(): Result<List<Map<String, Any>>> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${SettingsManager.backendUrl}/api/alerts")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (bodyStr != null) {
                        val list = gson.fromJson(bodyStr, List::class.java) as List<Map<String, Any>>
                        Result.success(list)
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get Student History
    suspend fun getStudentHistory(uid: String): Result<List<Map<String, Any>>> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${SettingsManager.backendUrl}/api/student/$uid/history")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (bodyStr != null) {
                        val map = gson.fromJson(bodyStr, Map::class.java)
                        val history = map["history"] as? List<Map<String, Any>> ?: emptyList()
                        Result.success(history)
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get All Students for Teacher Dashboard
    suspend fun getStudents(): Result<List<Map<String, Any>>> {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${SettingsManager.backendUrl}/api/students")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (bodyStr != null) {
                        val list = gson.fromJson(bodyStr, List::class.java) as List<Map<String, Any>>
                        Result.success(list)
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
