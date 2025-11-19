package com.example.eventoscomunitarios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.eventoscomunitarios.ui.theme.EventosComunitariosTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Timestamp
import java.util.*

class MainActivity : ComponentActivity() {

    private val vm: EventsViewModel by viewModels()

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) vm.signInWithGoogleIdToken(idToken)
            } catch (e: Exception) {
                // Error manejado por el ViewModel
            }
        }

    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        googleLauncher.launch(client.signInIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventosComunitariosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val user by vm.user.collectAsState()
                    if (user == null) {
                        AuthScreen(
                            onLoginEmail = { e, p -> vm.loginWithEmail(e, p) },
                            onRegisterEmail = { e, p -> vm.registerWithEmail(e, p) },
                            onLoginGoogle = { startGoogleSignIn() },
                            vm = vm
                        )
                    } else {
                        HomeScreen(vm = vm, onLogout = { vm.signOut() })
                    }
                }
            }
        }
    }
}

// ==================== PANTALLAS DE AUTENTICACIÓN ====================

@Composable
private fun AuthScreen(
    onLoginEmail: (String, String) -> Unit,
    onRegisterEmail: (String, String) -> Unit,
    onLoginGoogle: () -> Unit,
    vm: EventsViewModel
) {
    var showRegisterScreen by remember { mutableStateOf(false) }
    val user by vm.user.collectAsState()

    if (user != null) return

    if (showRegisterScreen) {
        RegisterScreen(
            onRegister = onRegisterEmail,
            onBackToLogin = { showRegisterScreen = false },
            vm = vm
        )
    } else {
        LoginScreen(
            onLogin = onLoginEmail,
            onNavigateToRegister = { showRegisterScreen = true },
            onLoginGoogle = onLoginGoogle,
            vm = vm
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginGoogle: () -> Unit,
    vm: EventsViewModel
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.clearErrorState() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Eventos Comunitarios",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Conecta con tu comunidad",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        when (ui) {
            is UiState.Error -> {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        (ui as UiState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is UiState.Info -> {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        (ui as UiState.Info).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            else -> {}
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onLogin(email, pass) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = ui != UiState.Loading
        ) {
            if (ui == UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Iniciar sesión")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = ui != UiState.Loading
        ) {
            Text("Crear cuenta nueva")
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "  o  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLoginGoogle,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = ui != UiState.Loading
        ) {
            Icon(Icons.Default.AccountCircle, null)
            Spacer(Modifier.width(8.dp))
            Text("Continuar con Google")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreen(
    onRegister: (String, String) -> Unit,
    onBackToLogin: () -> Unit,
    vm: EventsViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.clearErrorState() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToLogin) {
                Icon(Icons.Default.ArrowBack, "Volver")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Crear Cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Únete a la comunidad",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Mínimo 6 caracteres") }
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Las contraseñas no coinciden",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        when (ui) {
            is UiState.Error -> {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        (ui as UiState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is UiState.Info -> {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        (ui as UiState.Info).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            else -> {}
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (password == confirmPassword) {
                    onRegister(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = ui != UiState.Loading &&
                    email.isNotBlank() &&
                    password.length >= 6 &&
                    password == confirmPassword
        ) {
            if (ui == UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Crear cuenta")
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBackToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

// ==================== PANTALLA PRINCIPAL ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(vm: EventsViewModel, onLogout: () -> Unit) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Todos los Eventos", "Mis Eventos")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eventos Comunitarios") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (tabIndex) {
                0 -> EventosListScreen(vm)
                1 -> MisEventosScreen(vm)
            }
        }
    }
}

@Composable
fun EventosListScreen(vm: EventsViewModel) {
    val eventos by vm.eventos.collectAsState()
    val ui by vm.ui.collectAsState()
    var mostrarCrearEvento by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.cargarEventos()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            ui is UiState.Loading && eventos.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            eventos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No hay eventos disponibles",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(eventos, key = { it.id }) { evento ->
                        EventoCard(evento = evento, vm = vm)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { mostrarCrearEvento = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Crear evento")
        }
    }

    if (mostrarCrearEvento) {
        CrearEventoDialog(
            vm = vm,
            onDismiss = { mostrarCrearEvento = false }
        )
    }
}

@Composable
fun MisEventosScreen(vm: EventsViewModel) {
    val misEventos by vm.misEventos.collectAsState()
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.cargarMisEventos()
    }

    when {
        ui is UiState.Loading && misEventos.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        misEventos.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No has creado eventos aún",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(misEventos, key = { it.id }) { evento ->
                    MiEventoCard(evento = evento, vm = vm)
                }
            }
        }
    }
}

@Composable
fun EventoCard(evento: Evento, vm: EventsViewModel) {
    val estaInscrito = vm.estaInscrito(evento)
    val espaciosDisponibles = evento.maxParticipantes - evento.participantes.size

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        evento.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            evento.categoria,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (evento.descripcion.isNotBlank()) {
                Text(
                    evento.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    vm.formatearFecha(evento.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (evento.ubicacion.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        evento.ubicacion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Organizador: ${evento.organizador}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "$espaciosDisponibles espacios",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (espaciosDisponibles > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))

            if (estaInscrito) {
                Button(
                    onClick = { vm.desinscribirseEvento(evento.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar Inscripción")
                }
            } else if (espaciosDisponibles > 0) {
                Button(
                    onClick = { vm.inscribirseEvento(evento.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Inscribirse")
                }
            } else {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text("Evento Lleno")
                }
            }
        }
    }
}

@Composable
fun MiEventoCard(evento: Evento, vm: EventsViewModel) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        evento.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        evento.categoria,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { mostrarConfirmacion = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                vm.formatearFecha(evento.fecha),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Inscritos: ${evento.participantes.size}/${evento.maxParticipantes}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Eliminar evento") },
            text = { Text("¿Estás seguro? Los participantes inscritos serán notificados.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.eliminarEvento(evento.id)
                        mostrarConfirmacion = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEventoDialog(vm: EventsViewModel, onDismiss: () -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var maxParticipantes by remember { mutableStateOf("") }
    var mostrarMenuCategorias by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
    }
    val fechaEvento = Timestamp(cal.time)

    AlertDialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Crear Nuevo Evento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del evento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = mostrarMenuCategorias,
                    onExpandedChange = { mostrarMenuCategorias = it }
                ) {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mostrarMenuCategorias) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = mostrarMenuCategorias,
                        onDismissRequest = { mostrarMenuCategorias = false }
                    ) {
                        vm.categoriasEventos.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categoria = cat
                                    mostrarMenuCategorias = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = maxParticipantes,
                    onValueChange = { maxParticipantes = it },
                    label = { Text("Máximo de participantes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            vm.crearEvento(
                                titulo,
                                descripcion,
                                ubicacion,
                                fechaEvento,
                                categoria,
                                maxParticipantes
                            )
                            onDismiss()
                        }
                    ) {
                        Text("Crear")
                    }
                }
            }
        }
    }
}