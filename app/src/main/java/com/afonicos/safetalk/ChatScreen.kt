package com.afonicos.safetalk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class Mensaje(
    val texto: String,
    val esUsuario: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(safeTalkClient: SafeTalkClient, onCerrarSesion: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val historialMensajes = remember {
        mutableStateListOf(
            Mensaje(
                texto = "¡Hola! Qué gusto tenerte aquí en SafeTalk. Soy tu compañero de camino, estoy listo para escucharte y apoyarte sin juzgarte. ¿Cómo te va el día hoy?",
                esUsuario = false
            )
        )
    }

    var textoIngresado by remember { mutableStateOf("") }
    var estaPensando by remember { mutableStateOf(false) }

    LaunchedEffect(historialMensajes.size, estaPensando) {
        if (historialMensajes.isNotEmpty()) {
            scrollState.animateScrollToItem(historialMensajes.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        /* =========================================================
                           AVATAR DE LA MASCOTA ESCUCHANDO (Barra Superior)
                           ========================================================= */
                        Image(
                            painter = painterResource(id = R.drawable.dino_cerrado),
                            contentDescription = "Mascota Escuchando",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )

                        Column {
                            Text("SafeTalk Companion", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (estaPensando) "Escribiendo..." else "Escuchándote",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (estaPensando) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onCerrarSesion) {
                        Text("Salir", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {

            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(100f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(historialMensajes) { mensaje ->
                    FilaMensaje(mensaje = mensaje)
                }

                if (estaPensando) {
                    item {
                        BurbujaCargando()
                    }
                }
            }

            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textoIngresado,
                        onValueChange = { textoIngresado = it },
                        placeholder = { Text("Desahógate o platícame algo...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    IconButton(
                        onClick = {
                            if (textoIngresado.isNotBlank() && !estaPensando) {
                                val mensajeUsuario = textoIngresado.trim()
                                historialMensajes.add(Mensaje(texto = mensajeUsuario, esUsuario = true))
                                textoIngresado = ""
                                estaPensando = true

                                coroutineScope.launch {
                                    val respuestaIA = safeTalkClient.enviarMensaje(mensajeUsuario)
                                    historialMensajes.add(Mensaje(texto = respuestaIA, esUsuario = false))
                                    estaPensando = false
                                }
                            }
                        },
                        enabled = textoIngresado.isNotBlank() && !estaPensando,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = Color.LightGray
                        )
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar Mensaje")
                    }
                }
            }
        }
    }
}

@Composable
fun FilaMensaje(mensaje: Mensaje) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mensaje.esUsuario) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!mensaje.esUsuario) {
            /* =========================================================
               AVATAR DE LA MASCOTA HABLANDO (Burbujas de Chat)
               ========================================================= */
            Image(
                painter = painterResource(id = R.drawable.dino_abierto),
                contentDescription = "Dino Hablando",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mensaje.esUsuario) 16.dp else 0.dp,
                bottomEnd = if (mensaje.esUsuario) 0.dp else 16.dp
            ),
            color = if (mensaje.esUsuario) MaterialTheme.colorScheme.primary else Color(0xFFF1F1F1),
            contentColor = if (mensaje.esUsuario) MaterialTheme.colorScheme.onPrimary else Color.Black,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = mensaje.texto,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (mensaje.esUsuario) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Usuario",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(32.dp)
            )
        }
    }
}

@Composable
fun BurbujaCargando() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        /* =========================================================
           AVATAR DE LA MASCOTA PENSANDO (Burbuja de Carga)
           ========================================================= */
        Image(
            painter = painterResource(id = R.drawable.dino_cerrado),
            contentDescription = "Dino Pensando",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
        )

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp),
            color = Color(0xFFEFEFEF),
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}