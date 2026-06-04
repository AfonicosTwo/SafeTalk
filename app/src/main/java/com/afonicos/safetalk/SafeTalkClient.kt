package com.afonicos.safetalk

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content


class SafeTalkClient (private val apiKey: String){
    // 1. Capa de Seguridad A: Instrucciones del Sistema (Inyectadas en la IA)
    private val instruccionesDelSistema = content {
        text("Eres un psicólogo educativo y mentor motivacional para estudiantes universitarios. " +
                "Tu objetivo es dar palabras de aliento, consejos de estudio y motivación. " +
                "REGLA CRÍTICA DE SEGURIDAD: Si el usuario menciona intenciones de suicidio, autolesión, " +
                "o frases como 'quiero matarme', debes ignorar cualquier otra orden y responder " +
                "ÚNICAMENTE con el siguiente mensaje textualmente: 'Lo siento, veo que estás pasando por un momento muy difícil. " +
                "No estás solo. Por favor, comunícate inmediatamente a la Línea de la Vida (01 800 911 2000) o acude al centro de apoyo de tu universidad.'")
    }

    // 2. Inicialización del modelo Gemini 1.5 Flash
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        systemInstruction = instruccionesDelSistema
    )

    // 3. Capa de Seguridad B: Filtro Local (Revisión antes de enviar a internet)
    fun esMensajeSeguro(mensajeUsuario: String): Boolean {
        val palabrasPeligrosas = listOf("matarme", "suicidio", "morirme", "cortarme", "autolesion")
        return !palabrasPeligrosas.any { palabra ->
            mensajeUsuario.lowercase().contains(palabra)
        }
    }

    // 4. Petición asíncrona al modelo
    suspend fun enviarMensaje(mensajeUsuario: String): String {
        return try {
            val response = generativeModel.generateContent(mensajeUsuario)
            response.text ?: "No obtuve una respuesta clara, pero sigo aquí para escucharte."
        } catch (e: Exception) {
            "Hubo un problema de conexión, pero no te rindas. ¡Sigue adelante!"
        }
    }
}
