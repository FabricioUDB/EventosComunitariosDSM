package com.example.eventoscomunitarios

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.eventoscomunitarios.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)

class MainActivity : ComponentActivity() {

    private val vm: EventsViewModel by viewModels()

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) vm.signInWithGoogleIdToken(idToken)
            } catch (e: Exception) { }
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
        crearCanalNotificaciones(this)

        setContent {
            EventosComunitariosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val user by vm.user.collectAsState()
                    PermisoNotificaciones()

                    AnimatedContent(
                        targetState = user == null,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) with
                                    fadeOut(animationSpec = tween(300))
                        },
                        label = "auth_transition"
                    ) { isLogin ->
                        if (isLogin) {
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
}

@Composable
fun PermisoNotificaciones() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

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

    AnimatedContent(
        targetState = showRegisterScreen,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() with
                    slideOutHorizontally { -it } + fadeOut()
        },
        label = "auth_screen_transition"
    ) { isRegister ->
        if (isRegister) {
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
}

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

    val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    LaunchedEffect(Unit) { vm.clearErrorState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp).scale(scale),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Eventos Comunitarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Conecta con tu comunidad",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = PrimaryLight) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLight,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryLight) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLight,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    when (ui) {
                        is UiState.Error -> {
                            Spacer(Modifier.height(16.dp))
                            AnimatedMessageCard(
                                message = (ui as UiState.Error).message,
                                isError = true
                            )
                        }
                        is UiState.Info -> {
                            Spacer(Modifier.height(16.dp))
                            AnimatedMessageCard(
                                message = (ui as UiState.Info).message,
                                isError = false
                            )
                        }
                        else -> {}
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { onLogin(email, pass) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = ui != UiState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight)
                    ) {
                        if (ui == UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Iniciar sesión", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = ui != UiState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryLight)
                    ) {
                        Text("Crear cuenta nueva", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text("  o  ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onLoginGoogle,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = ui != UiState.Loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Icon(Icons.Default.AccountCircle, null, tint = Color(0xFFDB4437))
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar con Google", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(GradientStartLight, GradientEndLight)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Crear Cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Únete a la comunidad",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = TertiaryLight) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TertiaryLight,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TertiaryLight) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Mínimo 6 caracteres") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TertiaryLight,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = TertiaryLight) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TertiaryLight,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Las contraseñas no coinciden",
                            color = ErrorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    when (ui) {
                        is UiState.Error -> {
                            Spacer(Modifier.height(16.dp))
                            AnimatedMessageCard((ui as UiState.Error).message, true)
                        }
                        is UiState.Info -> {
                            Spacer(Modifier.height(16.dp))
                            AnimatedMessageCard((ui as UiState.Info).message, false)
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
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = ui != UiState.Loading &&
                                email.isNotBlank() &&
                                password.length >= 6 &&
                                password == confirmPassword,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TertiaryLight)
                    ) {
                        if (ui == UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Crear cuenta", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TertiaryLight)
                    ) {
                        Icon(Icons.Default.ArrowBack, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Volver al inicio", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMessageCard(message: String, isError: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isError) ErrorColor.copy(alpha = 0.1f) else SuccessColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isError) ErrorColor else SuccessColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    message,
                    color = if (isError) ErrorColor else SuccessColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(vm: EventsViewModel, onLogout: () -> Unit) {
    val context = LocalContext.current
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Próximos", "Mis Eventos", "Historial")

    LaunchedEffect(Unit) {
        vm.notificacion.collect { mensaje ->
            mostrarNotificacion(context, "Evento Actualizado", mensaje)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Eventos Comunitarios",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryLight,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = PrimaryLight,
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (tabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (tabIndex) {
                0 -> EventosListScreen(vm)
                1 -> MisEventosScreen(vm)
                2 -> HistorialScreen(vm)
            }
        }
    }
}

@Composable
fun EventosListScreen(vm: EventsViewModel) {
    val eventos by vm.eventos.collectAsState()
    val ui by vm.ui.collectAsState()
    var mostrarCrearEvento by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.cargarEventos() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            ui is UiState.Loading && eventos.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryLight)
                        Spacer(Modifier.height(16.dp))
                        Text("Cargando eventos...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            eventos.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No hay eventos disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "¡Crea el primero!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(eventos, key = { it.id }) { evento ->
                        EventoCardMejorada(evento = evento, vm = vm)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { mostrarCrearEvento = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = SecondaryLight
        ) {
            Icon(Icons.Default.Add, "Crear evento", tint = Color.White)
        }
    }

    if (mostrarCrearEvento) {
        CrearEventoDialog(vm = vm, onDismiss = { mostrarCrearEvento = false })
    }
}

@Composable
fun EventoCardMejorada(evento: Evento, vm: EventsViewModel) {
    val context = LocalContext.current
    val estaInscrito = vm.estaInscrito(evento)
    val espaciosDisponibles = evento.maxParticipantes - evento.participantes.size
    val categoryColor = getCategoryColor(evento.categoria)
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.8f),
                                categoryColor.copy(alpha = 0.5f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    evento.categoria,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { agregarCalendario(context, evento) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Recordatorio",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { compartirEvento(context, evento, vm) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Compartir",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text(
                        evento.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                if (evento.descripcion.isNotBlank() && expanded) {
                    Text(
                        evento.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }

                InfoRow(Icons.Default.DateRange, vm.formatearFecha(evento.fecha), categoryColor)

                if (evento.ubicacion.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    InfoRow(Icons.Default.LocationOn, evento.ubicacion, categoryColor)
                }

                Spacer(Modifier.height(8.dp))
                InfoRow(Icons.Default.Person, "Organiza: ${evento.organizador}", categoryColor)

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Participantes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = evento.participantes.size.toFloat() / evento.maxParticipantes.toFloat(),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = categoryColor,
                            trackColor = categoryColor.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (espaciosDisponibles > 0)
                            SuccessColor.copy(alpha = 0.1f)
                        else
                            ErrorColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "${evento.participantes.size}/${evento.maxParticipantes}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (espaciosDisponibles > 0) SuccessColor else ErrorColor
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (estaInscrito) {
                    Button(
                        onClick = { vm.desinscribirseEvento(evento.id) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorColor.copy(alpha = 0.1f),
                            contentColor = ErrorColor
                        )
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Cancelar Inscripción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (espaciosDisponibles > 0) {
                    Button(
                        onClick = {
                            vm.inscribirseEvento(evento.id)
                            mostrarNotificacion(
                                context,
                                "Inscripción confirmada",
                                "Te has apuntado a ${evento.titulo}"
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Inscribirse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = false,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Evento Lleno", style = MaterialTheme.typography.titleMedium)
                    }
                }

                if (evento.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (expanded) "Ver menos" else "Ver más detalles")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (expanded) 180f else 0f
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MisEventosScreen(vm: EventsViewModel) {
    val misEventos by vm.misEventos.collectAsState()
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.cargarMisEventos() }

    when {
        ui is UiState.Loading && misEventos.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryLight)
            }
        }
        misEventos.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No has creado eventos aún",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(misEventos, key = { it.id }) { evento ->
                    MiEventoCard(evento = evento, vm = vm)
                }
            }
        }
    }
}

@Composable
fun MiEventoCard(evento: Evento, vm: EventsViewModel) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }
    val categoryColor = getCategoryColor(evento.categoria)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.7f),
                                categoryColor.copy(alpha = 0.4f)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                evento.categoria,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            evento.titulo,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2
                        )
                    }

                    if (vm.esOrganizador(evento)) {
                        Column {
                            IconButton(
                                onClick = { mostrarEditar = true },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                            }
                            IconButton(
                                onClick = { mostrarConfirmacion = true },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Fecha del evento",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            vm.formatearFecha(evento.fecha),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryColor.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${evento.participantes.size}/${evento.maxParticipantes}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                            Text(
                                "Inscritos",
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            icon = { Icon(Icons.Default.Warning, null, tint = ErrorColor) },
            title = { Text("Eliminar evento") },
            text = { Text("¿Seguro que deseas eliminar este evento? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.eliminarEvento(evento.id)
                        mostrarConfirmacion = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarEditar) {
        EditarEventoDialog(evento = evento, vm = vm, onDismiss = { mostrarEditar = false })
    }
}

@Composable
fun HistorialScreen(vm: EventsViewModel) {
    val eventosPasados by vm.eventosPasados.collectAsState()
    val ui by vm.ui.collectAsState()
    var eventoSeleccionado by remember { mutableStateOf<Evento?>(null) }

    LaunchedEffect(Unit) { vm.cargarEventosPasados() }

    when {
        ui is UiState.Loading && eventosPasados.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryLight)
            }
        }
        eventosPasados.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No hay eventos pasados",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(eventosPasados, key = { it.id }) { evento ->
                    EventoPasadoCard(evento, vm) { eventoSeleccionado = evento }
                }
            }
        }
    }

    eventoSeleccionado?.let { evento ->
        DetalleEventoDialog(evento, vm) { eventoSeleccionado = null }
    }
}

@Composable
fun EventoPasadoCard(evento: Evento, vm: EventsViewModel, onClick: () -> Unit) {
    val categoryColor = getCategoryColor(evento.categoria)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(16.dp),
                color = categoryColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = categoryColor
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = categoryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        evento.categoria,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    evento.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    vm.formatearFecha(evento.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (evento.totalCalificaciones > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = WarningColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            String.format("%.1f", evento.calificacionPromedio),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningColor
                        )
                        Text(
                            " (${evento.totalCalificaciones})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (vm.estaInscrito(evento) && !vm.yaComentaste(evento.id)) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = categoryColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleEventoDialog(evento: Evento, vm: EventsViewModel, onDismiss: () -> Unit) {
    val comentarios by vm.comentariosEvento.collectAsState()
    val estaInscrito = vm.estaInscrito(evento)
    val yaComentaste = vm.yaComentaste(evento.id)
    var mostrarFormComentario by remember { mutableStateOf(false) }

    LaunchedEffect(evento.id) { vm.cargarComentarios(evento.id) }

    AlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(24.dp)
            ) {
                item {
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
                                color = getCategoryColor(evento.categoria).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    evento.categoria,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getCategoryColor(evento.categoria)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (evento.totalCalificaciones > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = WarningColor.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = WarningColor
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    String.format("%.1f", evento.calificacionPromedio),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningColor
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(${evento.totalCalificaciones})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (estaInscrito && !yaComentaste) {
                        Button(
                            onClick = { mostrarFormComentario = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningColor)
                        ) {
                            Icon(Icons.Default.Star, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Calificar este evento", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Text(
                        "Comentarios (${comentarios.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                }

                items(comentarios, key = { it.id }) { comentario ->
                    ComentarioCard(comentario, vm, evento.id)
                    Spacer(Modifier.height(8.dp))
                }

                if (comentarios.isEmpty()) {
                    item {
                        Text(
                            "No hay comentarios aún",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (mostrarFormComentario) {
        AgregarComentarioDialog(evento.id, vm) { mostrarFormComentario = false }
    }
}

@Composable
fun ComentarioCard(comentario: Comentario, vm: EventsViewModel, eventoId: String) {
    val currentUser = vm.user.collectAsState().value
    val esPropio = currentUser?.uid == comentario.usuarioId

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esPropio)
                PrimaryLight.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        comentario.nombreUsuario,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (index < comentario.calificacion)
                                    WarningColor
                                else
                                    Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                if (esPropio) {
                    IconButton(
                        onClick = { vm.eliminarComentario(eventoId, comentario.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(18.dp),
                            tint = ErrorColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(comentario.comentario, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(4.dp))
            Text(
                vm.formatearFecha(comentario.fecha),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarComentarioDialog(eventoId: String, vm: EventsViewModel, onDismiss: () -> Unit) {
    var comentario by remember { mutableStateOf("") }
    var calificacion by remember { mutableStateOf(5) }

    AlertDialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(
                    "Calificar Evento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                Text("Tu calificación:", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        IconButton(onClick = { calificacion = index + 1 }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = if (index < calificacion) WarningColor else Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text("Tu comentario") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
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
                            vm.agregarComentario(eventoId, comentario, calificacion)
                            onDismiss()
                        },
                        enabled = comentario.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Publicar")
                    }
                }
            }
        }
    }
}

// Continuará con CrearEventoDialog y EditarEventoDialog...

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
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
    }
    var fechaSeleccionada by remember { mutableStateOf(cal) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    AlertDialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    "Crear Nuevo Evento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del evento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = dateFormatter.format(fechaSeleccionada.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha del evento") },
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { mostrarDatePicker = true }) {
                            Icon(Icons.Default.Edit, "Cambiar fecha")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = timeFormatter.format(fechaSeleccionada.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hora del evento") },
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { mostrarTimePicker = true }) {
                            Icon(Icons.Default.Edit, "Cambiar hora")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
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
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(cat))
                                    )
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
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val fechaEvento = Timestamp(fechaSeleccionada.time)
                            vm.crearEvento(titulo, descripcion, ubicacion, fechaEvento, categoria, maxParticipantes)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Crear", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mostrarDatePicker) {
        DatePickerDialog(fechaSeleccionada, { fechaSeleccionada = it; mostrarDatePicker = false }) { mostrarDatePicker = false }
    }

    if (mostrarTimePicker) {
        TimePickerDialog(fechaSeleccionada, { h, m ->
            fechaSeleccionada.set(Calendar.HOUR_OF_DAY, h)
            fechaSeleccionada.set(Calendar.MINUTE, m)
            mostrarTimePicker = false
        }) { mostrarTimePicker = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarEventoDialog(evento: Evento, vm: EventsViewModel, onDismiss: () -> Unit) {
    var titulo by remember { mutableStateOf(evento.titulo) }
    var descripcion by remember { mutableStateOf(evento.descripcion) }
    var ubicacion by remember { mutableStateOf(evento.ubicacion) }
    var categoria by remember { mutableStateOf(evento.categoria) }
    var maxParticipantes by remember { mutableStateOf(evento.maxParticipantes.toString()) }
    var mostrarMenuCategorias by remember { mutableStateOf(false) }

    val fechaCal = Calendar.getInstance().apply { time = evento.fecha.toDate() }
    var fechaSeleccionada by remember { mutableStateOf(fechaCal) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    AlertDialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    "Editar Evento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del evento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = dateFormatter.format(fechaSeleccionada.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha del evento") },
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { mostrarDatePicker = true }) {
                            Icon(Icons.Default.Edit, "Cambiar fecha")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = timeFormatter.format(fechaSeleccionada.time),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hora del evento") },
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { mostrarTimePicker = true }) {
                            Icon(Icons.Default.Edit, "Cambiar hora")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
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
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val fechaEvento = Timestamp(fechaSeleccionada.time)
                            vm.editarEvento(evento.id, titulo, descripcion, ubicacion, fechaEvento, categoria, maxParticipantes)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mostrarDatePicker) {
        DatePickerDialog(fechaSeleccionada, { fechaSeleccionada = it; mostrarDatePicker = false }) { mostrarDatePicker = false }
    }

    if (mostrarTimePicker) {
        TimePickerDialog(fechaSeleccionada, { h, m ->
            fechaSeleccionada.set(Calendar.HOUR_OF_DAY, h)
            fechaSeleccionada.set(Calendar.MINUTE, m)
            mostrarTimePicker = false
        }) { mostrarTimePicker = false }
    }
}

@Composable
fun DatePickerDialog(
    fechaSeleccionada: Calendar,
    onFechaSeleccionada: (Calendar) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = fechaSeleccionada.timeInMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                onFechaSeleccionada(newCal)
            },
            fechaSeleccionada.get(Calendar.YEAR),
            fechaSeleccionada.get(Calendar.MONTH),
            fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.setOnDismissListener { onDismiss() }
        datePickerDialog.show()
    }
}

@Composable
fun TimePickerDialog(
    fechaSeleccionada: Calendar,
    onHoraSeleccionada: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val timePickerDialog = android.app.TimePickerDialog(
            context,
            { _, hour, minute -> onHoraSeleccionada(hour, minute) },
            fechaSeleccionada.get(Calendar.HOUR_OF_DAY),
            fechaSeleccionada.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.setOnDismissListener { onDismiss() }
        timePickerDialog.show()
    }
}

fun compartirEvento(context: Context, evento: Evento, vm: EventsViewModel) {
    val fechaStr = vm.formatearFecha(evento.fecha)
    val mensaje = """
        📅 ¡Mira este evento en Eventos Comunitarios!
        
        📍 *${evento.titulo}*
        📂 ${evento.categoria}
        🗓 Fecha: $fechaStr
        📌 Ubicación: ${evento.ubicacion}
        
        📝 ${evento.descripcion}
        
        ¡Te espero ahí!
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, mensaje)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Compartir evento vía...")
    context.startActivity(shareIntent)
}

fun agregarCalendario(context: Context, evento: Evento) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, evento.titulo)
        putExtra(CalendarContract.Events.EVENT_LOCATION, evento.ubicacion)
        putExtra(CalendarContract.Events.DESCRIPTION, evento.descripcion)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, evento.fecha.toDate().time)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, evento.fecha.toDate().time + 3600000)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // No calendar app available
    }
}

fun crearCanalNotificaciones(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Notificaciones de Eventos"
        val descriptionText = "Canal para recordatorios de eventos"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("EVENTOS_CHANNEL", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun mostrarNotificacion(context: Context, titulo: String, mensaje: String) {
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val builder = NotificationCompat.Builder(context, "EVENTOS_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}