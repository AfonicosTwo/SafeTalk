package com.afonicos.safetalk

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afonicos.safetalk.ui.theme.SafeTalkTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var safeTalkClient: SafeTalkClient
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializamos Firebase Analytics
        firebaseAnalytics = Firebase.analytics

        // 2. Inicializamos nuestro cliente de Gemini
        // ⚠️ IMPORTANTE: Asegúrate de poner tu llave real aquí para la prueba
        safeTalkClient = SafeTalkClient("")
        setContent {
            SafeTalkTheme {
                PantallaDePrueba(safeTalkClient, firebaseAnalytics)
            }
        }
    }
}

@Composable
fun PantallaDePrueba(safeTalkClient: SafeTalkClient, firebaseAnalytics: FirebaseAnalytics) {
    // Variables de estado para actualizar el texto en tiempo real
    var textoPantalla by remember { mutableStateOf("Presiona el botón para iniciar los motores.") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = textoPantalla,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(onClick = {
            textoPantalla = "Probando conexión, por favor espera..."

            // PRUEBA A: Lanzar evento a Firebase
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "boton_de_prueba")
            firebaseAnalytics.logEvent("prueba_conexion", bundle)
            Log.d("PruebaSafeTalk", "Evento enviado a Firebase exitosamente.")

            // PRUEBA B: Hablar con Gemini
            coroutineScope.launch {
                val mensajePrueba = "Hola, esta es una prueba de sistema. Por favor responde únicamente con la frase: 'Gemini está en línea y listo para ayudar'."

                // Usamos nuestro cliente seguro
                val respuesta = safeTalkClient.enviarMensaje(mensajePrueba)

                // Mostramos el resultado final
                textoPantalla = "Firebase: Conectado ✅\n\nGemini dice:\n$respuesta"
            }
        }) {
            Text("Ejecutar Prueba de Sistemas")
        }
    }
}