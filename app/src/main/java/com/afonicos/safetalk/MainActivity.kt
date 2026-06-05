package com.afonicos.safetalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.afonicos.safetalk.ui.theme.SafeTalkTheme

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var safeTalkClient: SafeTalkClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos los motores de backend y seguridad
        authManager = AuthManager()

        // ⚠️ Recuerda usar temporalmente tu API Key real en tu dispositivo local para probarla
        safeTalkClient = SafeTalkClient("LLAVE_SEGURA_NO_SUBIR")

        setContent {
            SafeTalkTheme {
                // Estado lógico para saber qué pantalla pintar en el celular
                // false = Login/Registro, true = Sala de Chat activa
                var usuarioAutenticado by remember { mutableStateOf(false) }

                if (!usuarioAutenticado) {
                    AuthScreen(
                        authManager = authManager,
                        onAuthSuccess = {
                            usuarioAutenticado = true // Saltamos la seguridad e ingresamos al chat
                        }
                    )
                } else {
                    ChatScreen(
                        safeTalkClient = safeTalkClient,
                        onCerrarSesion = {
                            usuarioAutenticado = false // Regresamos de forma segura al Login
                        }
                    )
                }
            }
        }
    }
}