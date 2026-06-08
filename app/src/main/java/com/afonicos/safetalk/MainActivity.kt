package com.afonicos.safetalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.afonicos.safetalk.ui.theme.SafeTalkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var safeTalkClient: SafeTalkClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos los motores de backend y seguridad
        authManager = AuthManager()

        // ⚠️ Recuerda usar temporalmente tu API Key real en tu dispositivo local para probarla
        safeTalkClient = SafeTalkClient("")

        // Esto se ejecuta al abrir la app si el usuario ya tenía sesión iniciada
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val nombreDeFirebase = user.displayName ?: "Estudiante"
            safeTalkClient.nombreUsuario = nombreDeFirebase
        }

        setContent {
            SafeTalkTheme {
                // Estado lógico para saber qué pantalla pintar en el celular
                // false = Login/Registro, true = Sala de Chat activa
                var usuarioAutenticado by remember { mutableStateOf(false) }

                if (!usuarioAutenticado) {
                    // Pantalla de Autenticación
                    AuthScreen(
                        authManager = authManager,
                        onAuthSuccess = {
                            // --- AQUÍ ESTÁ LA MAGIA CORREGIDA ---
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                // Vamos directo a Firestore para leer el nombre
                                val db = FirebaseFirestore.getInstance()
                                db.collection("usuarios").document(currentUser.uid).get()
                                    .addOnSuccessListener { documento ->
                                        // Extraemos el nombre exacto de la base de datos
                                        val nombreReal = documento.getString("nombre") ?: "Estudiante"
                                        safeTalkClient.nombreUsuario = nombreReal

                                        // Ahora sí, entramos al chat
                                        usuarioAutenticado = true
                                    }
                                    .addOnFailureListener {
                                        // Plan B si falla la conexión
                                        safeTalkClient.nombreUsuario = "Estudiante"
                                        usuarioAutenticado = true
                                    }
                            } else {
                                usuarioAutenticado = true
                            }
                        }
                    )
                } else {
                    // Pantalla Principal del Chat
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