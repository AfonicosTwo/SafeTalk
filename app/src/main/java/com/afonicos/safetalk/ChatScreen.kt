package com.afonicos.safetalk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID

// =========================================================
// NUESTRA PALETA DE COLORES PERSONALIZADA
// =========================================================
val MoradoMascota = Color(0xFF8E24AA)
val AmbarComplemento = Color(0xFFFFB300)
val FondoCozy = Color(0xFFF6F4F9)
val BlancoPuro = Color(0xFFFFFFFF)
val TextoOscuro = Color(0xFF2C3E50)

data class Mensaje(val texto: String, val esUsuario: Boolean)
data class SesionChat(val id: String, var titulo: String, val mensajes: MutableList<Mensaje>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(safeTalkClient: SafeTalkClient, onCerrarSesion: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val chatManager = remember { ChatManager() }

    val sesiones = remember { mutableStateListOf<SesionChat>() }
    var chatActual by remember { mutableStateOf<SesionChat?>(null) }
    var textoIngresado by remember { mutableStateOf("") }
    var estaPensando by remember { mutableStateOf(false) }

    var chatAEliminar by remember { mutableStateOf<SesionChat?>(null) }

    LaunchedEffect(Unit) {
        chatManager.escucharConversaciones { listaDeNube ->
            sesiones.clear()
            sesiones.addAll(listaDeNube)

            if (sesiones.isNotEmpty()) {
                if (chatActual == null || sesiones.none { it.id == chatActual?.id }) {
                    chatActual = sesiones.first()
                }
            } else {
        // Esto solo se ejecutará la PRIMERA VEZ que el usuario crea su cuenta
        val idNuevo = UUID.randomUUID().toString()

        // 1. Asignamos el chat a la pantalla INMEDIATAMENTE para que no se quede en blanco
        val nuevaSesion = SesionChat(idNuevo, "Nueva Conversación", mutableStateListOf())
        chatActual = nuevaSesion

        chatManager.guardarNuevaConversacion(idNuevo, "Nueva Conversación") { exito ->
            if (exito) {
                // 2. Le damos el empujón inicial a Gemini en segundo plano
                coroutineScope.launch {
                    estaPensando = true

                    // Mensaje invisible para la IA
                    val contextoInicial = "El usuario acaba de crear su cuenta y entrar al chat. Por favor, preséntate de manera muy amigable, descríbete físicamente como el dinosaurio y salúdalo por su nombre para iniciar la sesión."

                    // Disparamos la solicitud
                    val respuestaDelDino = safeTalkClient.enviarMensaje(contextoInicial)

                    // Guardamos en la nube y mostramos en pantalla
                    chatManager.guardarMensaje(idNuevo, respuestaDelDino, false)
                    chatActual?.mensajes?.add(Mensaje(texto = respuestaDelDino, esUsuario = false))

                    estaPensando = false
                }
            }
        }
    }
        }
    }

    LaunchedEffect(chatActual?.id) {
        chatActual?.let { chat ->
            if (chat.mensajes.isEmpty()) {
                chatManager.cargarMensajesDeChat(chat.id) { mensajesDeNube ->
                    chat.mensajes.clear()
                    chat.mensajes.addAll(mensajesDeNube)
                }
            }
        }
    }

    LaunchedEffect(chatActual?.mensajes?.size, estaPensando) {
        val cantidadMensajes = chatActual?.mensajes?.size ?: 0
        if (cantidadMensajes > 0) {
            scrollState.animateScrollToItem(cantidadMensajes - 1)
        }
    }

    if (chatAEliminar != null) {
        AlertDialog(
            containerColor = BlancoPuro,
            onDismissRequest = { chatAEliminar = null },
            title = { Text("Eliminar Conversación", color = TextoOscuro, fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de eliminar esta conversación? Se borrará para siempre y no podrás recuperarla.", color = TextoOscuro) },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatAEliminar?.let { chat ->
                            /* =========================================================
                               ¡AQUÍ ENTRA TU IDEA BRILLANTE DE EVASIÓN DE ESTADO!
                               ========================================================= */
                            if (sesiones.size == 1) {
                                // Si es la última conversación, creamos el reemplazo PRIMERO
                                val idNuevo = UUID.randomUUID().toString()
                                val saludoInicial = "¡Hola! Qué gusto tenerte aquí en SafeTalk. Soy tu compañero de camino, estoy listo para escucharte y apoyarte sin juzgarte. ¿Cómo te va el día hoy?"

                                chatManager.guardarNuevaConversacion(idNuevo, "Nueva Conversación") { exito ->
                                    if (exito) {
                                        chatManager.guardarMensaje(idNuevo, saludoInicial, false)
                                        // Una vez que el reemplazo está a salvo en la nube, borramos la vieja
                                        chatManager.eliminarConversacion(chat.id)
                                    }
                                }
                            } else {
                                // Si hay más conversaciones, la borramos normalmente
                                chatManager.eliminarConversacion(chat.id)
                            }

                            // Limpiamos la pantalla actual para que el sistema salte a la siguiente disponible
                            if (chatActual?.id == chat.id) chatActual = null
                        }
                        chatAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { chatAEliminar = null }) {
                    Text("Cancelar", color = MoradoMascota)
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = BlancoPuro
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Tus Charlas",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MoradoMascota,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = FondoCozy, modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo Chat", tint = AmbarComplemento) },
                    label = { Text("Nueva Conversación", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        val idNuevo = UUID.randomUUID().toString()
                        val saludoInicial = "¡Hola de nuevo! Iniciemos una nueva charla. ¿De qué te gustaría platicar?"

                        val nuevaSesion = SesionChat(idNuevo, "Nueva Conversación", mutableStateListOf(Mensaje(saludoInicial, false)))
                        chatActual = nuevaSesion

                        chatManager.guardarNuevaConversacion(idNuevo, "Nueva Conversación") { exito ->
                            if (exito) chatManager.guardarMensaje(idNuevo, saludoInicial, false)
                        }
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = BlancoPuro)
                )

                HorizontalDivider(color = FondoCozy, modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn {
                    items(sesiones) { sesion ->
                        val isSelected = chatActual?.id == sesion.id
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sesion.titulo,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        color = if (isSelected) MoradoMascota else TextoOscuro
                                    )
                                    IconButton(
                                        onClick = { chatAEliminar = sesion },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.LightGray)
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                chatActual = sesion
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = FondoCozy,
                                unselectedContainerColor = BlancoPuro
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Surface(
                    shadowElevation = 4.dp,
                    color = BlancoPuro
                ) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Abrir Historial", tint = MoradoMascota)
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.dino_cerrado),
                                    contentDescription = "Mascota Escuchando",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(FondoCozy)
                                )
                                Column {
                                    Text("SafeTalk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MoradoMascota)
                                    Text(
                                        text = if (estaPensando) "Escribiendo..." else "En línea",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (estaPensando) AmbarComplemento else Color.Gray
                                    )
                                }
                            }
                        },
                        actions = {
                            TextButton(onClick = onCerrarSesion) {
                                Text("Salir", color = Color.Gray)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = BlancoPuro)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(FondoCozy)
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
                    chatActual?.mensajes?.let { mensajes ->
                        itemsIndexed(mensajes) { index, mensaje ->
                            FilaMensaje(mensaje = mensaje, esBienvenida = (index == 0 && !mensaje.esUsuario))
                        }
                    }
                    if (estaPensando) {
                        item { BurbujaCargando() }
                    }
                }

                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = BlancoPuro
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val enviarMensaje = {
                            val currentChat = chatActual
                            if (textoIngresado.isNotBlank() && !estaPensando && currentChat != null) {
                                val mensajeUsuario = textoIngresado.trim()

                                if (currentChat.titulo == "Nueva Conversación") {
                                    val tituloDinamico = if (mensajeUsuario.length > 25) {
                                        mensajeUsuario.substring(0, 25) + "..."
                                    } else {
                                        mensajeUsuario
                                    }
                                    chatManager.actualizarTituloConversacion(currentChat.id, tituloDinamico)
                                }

                                currentChat.mensajes.add(Mensaje(texto = mensajeUsuario, esUsuario = true))
                                chatManager.guardarMensaje(currentChat.id, mensajeUsuario, true)

                                textoIngresado = ""
                                estaPensando = true

                                coroutineScope.launch {
                                    val respuestaIA = safeTalkClient.enviarMensaje(mensajeUsuario)
                                    currentChat.mensajes.add(Mensaje(texto = respuestaIA, esUsuario = false))
                                    chatManager.guardarMensaje(currentChat.id, respuestaIA, false)
                                    estaPensando = false
                                }
                            }
                        }

                        OutlinedTextField(
                            value = textoIngresado,
                            onValueChange = { textoIngresado = it },
                            placeholder = { Text("Platícame algo...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                        if (event.isShiftPressed) {
                                            false
                                        } else {
                                            enviarMensaje()
                                            true
                                        }
                                    } else {
                                        false
                                    }
                                },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { enviarMensaje() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MoradoMascota,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                cursorColor = MoradoMascota
                            )
                        )

                        IconButton(
                            onClick = { enviarMensaje() },
                            enabled = textoIngresado.isNotBlank() && !estaPensando,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = AmbarComplemento,
                                contentColor = BlancoPuro,
                                disabledContainerColor = Color(0xFFE0E0E0)
                            ),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar Mensaje", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilaMensaje(mensaje: Mensaje, esBienvenida: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mensaje.esUsuario) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!mensaje.esUsuario) {
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
            color = if (mensaje.esUsuario) MoradoMascota else BlancoPuro,
            contentColor = if (mensaje.esUsuario) BlancoPuro else TextoOscuro,
            shadowElevation = if (mensaje.esUsuario) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = if (esBienvenida) 320.dp else 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = mensaje.texto,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f)
                )

                if (esBienvenida) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.dino_bienvenida),
                        contentDescription = "Dino dando la bienvenida",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        if (mensaje.esUsuario) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Usuario",
                tint = AmbarComplemento,
                modifier = Modifier.padding(start = 8.dp).size(32.dp)
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
        Image(
            painter = painterResource(id = R.drawable.dino_cerrado),
            contentDescription = "Dino Pensando",
            modifier = Modifier.padding(end = 8.dp).size(36.dp).clip(CircleShape)
        )

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp),
            color = BlancoPuro,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).size(20.dp),
                strokeWidth = 3.dp,
                color = AmbarComplemento
            )
        }
    }
}