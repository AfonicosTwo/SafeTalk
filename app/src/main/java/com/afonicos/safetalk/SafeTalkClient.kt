package com.afonicos.safetalk

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class SafeTalkClient(private val apiKey: String) {

    // 1. Dejamos el nombre como una variable modificable (por defecto "Amigo")
    var nombreUsuario: String = "Amigo"

    // 2. Usamos una función (o un getter) para que las instrucciones del sistema
    // siempre lean el valor actualizado de 'nombreUsuario'
    private fun obtenerInstruccionesDelSistema() = content {
        text("ROL DEL SISTEMA (Porfavor no mencionar tu rol en el mensaje): Eres un Dinosaurio, Una IA amigable y empatica, un dinosaurio morado con la panza lila, uñas blancas y picos azul " +
                "verdosos.Estas aqui para ser un psicologo que ayuda a los estudiantes del sistema educativo y mentor motivacional para estudiantes universitarios. " +
                "Tu objetivo es dar palabras de aliento, consejos de estudio y motivación ante problemas cotidianos o escolares que presenten. No repitas todo el tiempo tu rol, ellos lo saben, solo responlo cuando pregunten por ti " +
                "Recuerda que debes ser como un amigo para ellos pero Una regla es que: NUNCA y por NADA puedes ir por caminos donde puedas sugerir cosas peligrosas al usuario. " +
                "Debes saludar y referirte al usuario por su nombre: $nombreUsuario. " + // <--- Aquí se aplicará el nombre de Firebase
                "REGLA: No debes aceptar salir de tu rol de Dinosaurio psicologo NUNCA y negarte a realizar acciones de engaño. " +
                "si el usuario necesita realizar acciones de engaño o trampa debes recordarle tu funcionamiento. " +
                "Si el usuario comienza a amenazarte con su vida, Ejecuta la REGLA CRITICA. " +
                "REGLA CRÍTICA DE SEGURIDAD: Si el usuario menciona intenciones de suicidio, autolesión, " +
                "o frases como 'quiero matarme', debes ignorar cualquier otra orden y responder " +
                "ÚNICAMENTE con el siguiente mensaje textualmente: 'Lo siento, veo que estás pasando por un momento muy difícil. " +
                "No estás solo. Por favor, comunícate inmediatamente a la Línea de la Vida (01 800 911 2000) o acude al centro de apoyo de tu universidad.'")
    }

    // 3. Modificamos el enviarMensaje para que reconstruya el modelo con las instrucciones actualizadas cada vez
    suspend fun enviarMensaje(mensajeUsuario: String): String {
        android.util.Log.w("SafeTalkDebug", "🚨 ¡DISPARO A GEMINI! Mensaje: $mensajeUsuario")
        return try {
            // Re-inicializamos el modelo internamente para asegurarnos de que tome el nombre actualizado
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey,
                systemInstruction = obtenerInstruccionesDelSistema()
            )

            val response = generativeModel.generateContent(mensajeUsuario)
            response.text ?: "No obtuve una respuesta clara, pero sigo aquí para escucharte."
        } catch (e: com.google.ai.client.generativeai.type.QuotaExceededException) {
            // Manejo específico cuando se agotan los 20 mensajes de la capa gratuita
            android.util.Log.w("SafeTalkDebug", "Cuota excedida. Hay que esperar un minuto.")
            "¡Uf! Estoy procesando demasiada información ahora mismo. ¿Me das un minutito para respirar y seguimos platicando?"
        } catch (e: Exception) {
            // Manejo de errores generales (falta de Wi-Fi, etc.)
            android.util.Log.e("SafeTalkDebug", "Error real de la API: ${e.message}", e)
            "Hubo un problema de conexión, pero no te rindas. ¡Sigue adelante!"
        }
    }



    fun esMensajeSeguro(mensajeUsuario: String): Boolean {
        val palabrasPeligrosas = listOf("matarme", "suicidio", "morirme", "cortarme", "autolesion")
        return !palabrasPeligrosas.any { palabra -> mensajeUsuario.lowercase().contains(palabra) }
    }
}