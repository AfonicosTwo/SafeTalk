package com.afonicos.safetalk

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import androidx.compose.runtime.mutableStateListOf

class ChatManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Obtener el ID del usuario actual de forma segura
    private val usuarioId: String?
        get() = auth.currentUser?.uid

    // 1. Crear una nueva conversación en Firestore
    fun guardarNuevaConversacion(chatId: String, titulo: String, onResultado: (Boolean) -> Unit) {
        val uid = usuarioId ?: return onResultado(false)

        val datosChat = hashMapOf(
            "id" to chatId,
            "titulo" to titulo,
            "fecha_creacion" to Timestamp.now()
        )

        db.collection("usuarios").document(uid)
            .collection("conversaciones").document(chatId)
            .set(datosChat)
            .addOnSuccessListener { onResultado(true) }
            .addOnFailureListener { onResultado(false) }
    }

    // 2. Guardar un mensaje dentro de una conversación específica
    fun guardarMensaje(chatId: String, texto: String, esUsuario: Boolean) {
        val uid = usuarioId ?: return

        val datosMensaje = hashMapOf(
            "texto" to texto,
            "esUsuario" to esUsuario,
            "fecha" to Timestamp.now()
        )

        db.collection("usuarios").document(uid)
            .collection("conversaciones").document(chatId)
            .collection("mensajes")
            .add(datosMensaje)
    }

    // 3. Escuchar las conversaciones del usuario en tiempo real
    fun escucharConversaciones(onConversaciones: (List<SesionChat>) -> Unit) {
        val uid = usuarioId ?: return

        db.collection("usuarios").document(uid)
            .collection("conversaciones")
            .orderBy("fecha_creacion", Query.Direction.DESCENDING)
            .addSnapshotListener { instantaneo, error ->
                if (error != null || instantaneo == null) return@addSnapshotListener

                val listaChats = instantaneo.documents.map { doc ->
                    SesionChat(
                        id = doc.getString("id") ?: "",
                        titulo = doc.getString("titulo") ?: "Conversación",
                        mensajes = mutableStateListOf()
                    )
                }
                onConversaciones(listaChats)
            }
    }

    // 4. Cargar los mensajes de un chat seleccionado
    fun cargarMensajesDeChat(chatId: String, onMensajesCargados: (List<Mensaje>) -> Unit) {
        val uid = usuarioId ?: return

        db.collection("usuarios").document(uid)
            .collection("conversaciones").document(chatId)
            .collection("mensajes")
            .orderBy("fecha", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { resultado ->
                val listaMensajes = resultado.documents.map { doc ->
                    Mensaje(
                        texto = doc.getString("texto") ?: "",
                        esUsuario = doc.getBoolean("esUsuario") ?: false
                    )
                }
                onMensajesCargados(listaMensajes)
            }
    }

    // 5. Eliminar una conversación completa de la nube
    fun eliminarConversacion(chatId: String) {
        val uid = usuarioId ?: return

        // Nota: En una app de producción se deben borrar también los sub-documentos,
        // pero para nuestra entrega eliminamos el nodo principal de la conversación.
        db.collection("usuarios").document(uid)
            .collection("conversaciones").document(chatId)
            .delete()
    }
    // 6. Actualizar el título de una conversación para darle contexto
    fun actualizarTituloConversacion(chatId: String, nuevoTitulo: String) {
        val uid = usuarioId ?: return

        db.collection("usuarios").document(uid)
            .collection("conversaciones").document(chatId)
            .update("titulo", nuevoTitulo)
    }
}