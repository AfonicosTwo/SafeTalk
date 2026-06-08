package com.afonicos.safetalk

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.firebase.auth.UserProfileChangeRequest

class AuthManager {
    // Inicializamos las herramientas de Firebase
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Función 1: Registrar Estudiante
    fun registrarEstudiante(correo: String, contrasenia: String, nombre: String, onResultado: (Boolean, String) -> Unit) {
        auth.createUserWithEmailAndPassword(correo, contrasenia)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    val usuarioActual = auth.currentUser

                    // 1. Guardamos el nombre en el perfil interno de Auth
                    val actualizacionPerfil = UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre)
                        .build()

                    usuarioActual?.updateProfile(actualizacionPerfil)?.addOnCompleteListener {
                        // 2. Una vez actualizado Auth, guardamos en Firestore como ya lo hacías
                        val usuarioId = usuarioActual.uid
                        val datosUsuario = hashMapOf(
                            "nombre" to nombre,
                            "correo" to correo,
                            "fecha_registro" to Timestamp.now()
                        )

                        db.collection("usuarios").document(usuarioId)
                            .set(datosUsuario)
                            .addOnSuccessListener {
                                onResultado(true, "¡Registro exitoso en SafeTalk!")
                            }
                            .addOnFailureListener { e ->
                                onResultado(false, "Error al guardar perfil: ${e.message}")
                            }
                    }
                } else {
                    onResultado(false, "Error en registro: ${tarea.exception?.message}")
                }
            }
    }

    // Función 2: Iniciar Sesión
    fun iniciarSesionEstudiante(correo: String, contrasenia: String, onResultado: (Boolean, String) -> Unit) {
        auth.signInWithEmailAndPassword(correo, contrasenia)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    onResultado(true, "¡Bienvenido de vuelta a SafeTalk!")
                } else {
                    onResultado(false, "Correo o contraseña incorrectos.")
                }
            }
    }

    // Función 3: Eliminar Cuenta Definitivamente
    fun eliminarCuenta(onResultado: (Boolean, String) -> Unit) {
        val usuarioActual = auth.currentUser
        val usuarioId = usuarioActual?.uid

        if (usuarioActual != null && usuarioId != null) {
            // 1. Primero destruimos su perfil en la base de datos (Firestore)
            db.collection("usuarios").document(usuarioId).delete()
                .addOnSuccessListener {

                    // 2. Si se borraron sus datos, ahora destruimos su acceso (Authentication)
                    usuarioActual.delete()
                        .addOnCompleteListener { tarea ->
                            if (tarea.isSuccessful) {
                                onResultado(true, "Tu cuenta ha sido eliminada para siempre.")
                            } else {
                                onResultado(false, "Error de autenticación al eliminar: ${tarea.exception?.message}")
                            }
                        }
                }
                .addOnFailureListener { e ->
                    onResultado(false, "No pudimos borrar tu perfil: ${e.message}")
                }
        } else {
            onResultado(false, "No hay ninguna sesión activa para eliminar.")
        }
    }
}

