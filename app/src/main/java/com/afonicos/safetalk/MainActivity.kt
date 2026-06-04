package com.afonicos.safetalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.afonicos.safetalk.ui.theme.SafeTalkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var safeTalkClient: SafeTalkClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Inicializamos el cliente de IA
        safeTalkClient = SafeTalkClient("AQUÍ_PONES_TU_API_KEY")

        // 2. Configuramos la interfaz gráfica usando Jetpack Compose
        setContent {
            SafeTalkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "SafeTalk (En Desarrollo)",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // 3. Simulamos el uso de la IA por consola (para probar que funciona)
        val mensajeDelUsuario = "Me siento muy estresado por los exámenes de esta semana"
        procesarFlujoDeChat(mensajeDelUsuario)
    }

    private fun procesarFlujoDeChat(mensaje: String) {
        // Ejecutamos el Filtro Local primero
        if (!safeTalkClient.esMensajeSeguro(mensaje)) {
            mostrarEnPantalla("Bot: Tu vida es valiosa. Por favor, busca ayuda profesional en el departamento psicológico de la escuela o llama al 01 800 911 2000.")
            return
        }

        // Si es seguro, abrimos una corrutina para consultar a Gemini
        lifecycleScope.launch {
            mostrarEnPantalla("Bot pensando...")

            // Llamada asíncrona a la IA
            val respuestaBot = safeTalkClient.enviarMensaje(mensaje)

            mostrarEnPantalla("Bot: $respuestaBot")
        }
    }

    // Esta función pinta el texto en la consola del IDE (Logcat/Run)
    private fun mostrarEnPantalla(texto: String) {
        println(texto)
    }
}

// Interfaz gráfica temporal
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "¡Hola $name! Tu motor de IA se está configurando.",
        modifier = modifier
    )
}