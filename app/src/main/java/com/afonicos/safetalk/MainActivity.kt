package com.afonicos.safetalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme // <-- NUEVO IMPORT NECESARIO
import androidx.compose.runtime.*
import com.afonicos.safetalk.ui.theme.SafeTalkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var safeTalkClient: SafeTalkClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos los motores
        authManager = AuthManager()
        safeTalkClient = SafeTalkClient("")

        // Esto se ejecuta al abrir la app si el usuario ya tenía sesión iniciada
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val nombreDeFirebase = user.displayName ?: "Estudiante"
            safeTalkClient.nombreUsuario = nombreDeFirebase
        }

        setContent {
            // =========================================================
            // AQUÍ INYECTAMOS LA LÓGICA DEL MODO OSCURO
            // =========================================================
            // 1. Leemos si el celular del usuario ya está en modo oscuro por defecto
            val sistemaOscuro = isSystemInDarkTheme()

            // 2. Creamos la variable maestra que controlará el tema en toda la app
            var isDarkMode by remember { mutableStateOf(sistemaOscuro) }

            // 3. Le pasamos esta variable a tu Tema Principal
            SafeTalkTheme(darkTheme = isDarkMode) {
                var usuarioAutenticado by remember { mutableStateOf(false) }

                if (!usuarioAutenticado) {
                    AuthScreen(
                        authManager = authManager,
                        onAuthSuccess = {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("usuarios").document(currentUser.uid).get()
                                    .addOnSuccessListener { documento ->
                                        val nombreReal = documento.getString("nombre") ?: "Estudiante"
                                        safeTalkClient.nombreUsuario = nombreReal
                                        usuarioAutenticado = true
                                    }
                                    .addOnFailureListener {
                                        safeTalkClient.nombreUsuario = "Estudiante"
                                        usuarioAutenticado = true
                                    }
                            } else {
                                usuarioAutenticado = true
                            }
                        }
                    )
                } else {
                    // =========================================================
                    // AQUÍ CONECTAMOS LOS NUEVOS DATOS CON LA PANTALLA DE CHAT
                    // =========================================================
                    ChatScreen(
                        safeTalkClient = safeTalkClient,
                        authManager = authManager,             // Para poder cambiar el nombre y contraseña
                        isDarkMode = isDarkMode,               // Le decimos a la UI en qué tema está
                        onToggleDarkMode = { isDarkMode = !isDarkMode }, // El botón que invierte el switch
                        onCerrarSesion = {
                            usuarioAutenticado = false
                        }
                    )
                }
            }
        }
    }
}