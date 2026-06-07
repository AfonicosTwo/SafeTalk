package com.afonicos.safetalk

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(authManager: AuthManager, onAuthSuccess: () -> Unit) {
    // Variable maestra que controla si estamos en "Modo Registro" o "Modo Login"
    var isRegistro by remember { mutableStateOf(false) }

    // Variables para guardar lo que escribe el usuario
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasenia by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // Permite deslizar si el teclado tapa
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        /* =========================================================
           1. LOGO DE LA MASCOTA
           ========================================================= */
        Image(
            painter = painterResource(id = R.drawable.logo_app),
            contentDescription = "Logo SafeTalk",
            modifier = Modifier.size(90.dp).clip(CircleShape) // Agregué el clip circular por si tu logo es cuadrado
        )
        Spacer(modifier = Modifier.height(40.dp))

        /* =========================================================
           2. TÍTULO DINÁMICO
           ========================================================= */
        Text(
            text = if (isRegistro) "Crear Nueva Cuenta" else "Iniciar Sesión",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        /* =========================================================
           3. CAMPOS DE TEXTO
           ========================================================= */

        // ¡El campo de Nombre solo aparece si el usuario quiere registrarse!
        if (isRegistro) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                placeholder = { Text("Ej. Jorge") },
                // Le damos la acción de "Siguiente" en el teclado también al nombre
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        // AQUÍ ES DONDE ESTABA EL ERROR.
        // En lugar de definir la función, ahora la estamos LLAMANDO y pasándole los datos.
        OutlinesTextFields(
            correo = correo,
            onCorreoChange = { correo = it },
            contrasenia = contrasenia,
            onContraseniaChange = { contrasenia = it }
        )

        /* =========================================================
           4. BOTÓN DE ACCIÓN PRINCIPAL
           ========================================================= */
        Button(
            onClick = {
                if (isRegistro) {
                    if (correo.isNotEmpty() && contrasenia.isNotEmpty() && nombre.isNotEmpty()) {
                        authManager.registrarEstudiante(correo, contrasenia, nombre) { exito, mensaje ->
                            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                            if (exito) onAuthSuccess()
                        }
                    } else {
                        Toast.makeText(context, "Por favor, llena todos los campos.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (correo.isNotEmpty() && contrasenia.isNotEmpty()) {
                        authManager.iniciarSesionEstudiante(correo, contrasenia) { exito, mensaje ->
                            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                            if (exito) onAuthSuccess()
                        }
                    } else {
                        Toast.makeText(context, "Por favor ingresa tu correo y contraseña.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (isRegistro) "Registrarme" else "Entrar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        /* =========================================================
           5. BOTÓN PARA CAMBIAR DE PANTALLA
           ========================================================= */
        TextButton(
            onClick = {
                isRegistro = !isRegistro
                correo = ""
                contrasenia = ""
                nombre = ""
            }
        ) {
            Text(if (isRegistro) "¿Ya tienes cuenta? Inicia sesión aquí" else "¿No tienes cuenta? Crea una aquí")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// =========================================================
// COMPONENTES SECUNDARIOS (FUERA DE LA PANTALLA PRINCIPAL)
// =========================================================

@Composable
fun OutlinesTextFields(
    correo: String,
    onCorreoChange: (String) -> Unit,
    contrasenia: String,
    onContraseniaChange: (String) -> Unit
) {
    // Controlador para ocultar el teclado mágicamente
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = correo,
        onValueChange = onCorreoChange,
        label = { Text("Correo Electrónico") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    )

    OutlinedTextField(
        value = contrasenia,
        onValueChange = onContraseniaChange,
        label = { Text("Contraseña") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.hide() }
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp) // Le agregué margen inferior para separarlo del botón Entrar
    )
}