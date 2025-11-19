package com.example.eventoscomunitarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

// --- Modelo de Evento ---
data class Evento(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val ubicacion: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val categoria: String = "",
    val organizador: String = "",
    val organizadorId: String = "",
    val participantes: List<String> = emptyList(),
    val maxParticipantes: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)

// --- Estados de UI ---
sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Error(val message: String) : UiState()
    data class Info(val message: String) : UiState()
}

class EventsViewModel : ViewModel() {

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Estado expuesto a la UI
    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user.asStateFlow()

    private val _ui = MutableStateFlow<UiState>(UiState.Idle)
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos.asStateFlow()

    private val _misEventos = MutableStateFlow<List<Evento>>(emptyList())
    val misEventos: StateFlow<List<Evento>> = _misEventos.asStateFlow()

    // Categorías de eventos
    val categoriasEventos = listOf(
        "Deportes",
        "Cultura",
        "Educación",
        "Música",
        "Arte",
        "Gastronomía",
        "Tecnología",
        "Solidaridad",
        "Medio Ambiente",
        "Otros"
    )

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    init {
        try {
            db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        } catch (e: Exception) {
            // Ya configurado
        }
    }

    // ---------- AUTENTICACIÓN ----------
    fun registerWithEmail(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank() || password.length < 6) {
            _ui.value = UiState.Error("Email inválido o contraseña muy corta (mín. 6 caracteres)")
            return@launch
        }
        _ui.value = UiState.Loading
        runCatching {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
        }.onSuccess {
            _user.value = auth.currentUser
            _ui.value = UiState.Info("Cuenta creada correctamente")
        }.onFailure {
            _ui.value = UiState.Error(it.message ?: "Error al registrarse")
        }
    }

    fun loginWithEmail(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank() || password.isBlank()) {
            _ui.value = UiState.Error("Completa todos los campos")
            return@launch
        }
        _ui.value = UiState.Loading
        runCatching {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
        }.onSuccess {
            _user.value = auth.currentUser
            _ui.value = UiState.Info("Inicio de sesión exitoso")
        }.onFailure {
            _ui.value = UiState.Error(it.message ?: "Credenciales inválidas")
        }
    }

    fun signInWithGoogleIdToken(idToken: String) = viewModelScope.launch {
        _ui.value = UiState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        runCatching { auth.signInWithCredential(credential).await() }
            .onSuccess {
                _user.value = auth.currentUser
                _ui.value = UiState.Info("Inicio de sesión con Google exitoso")
            }
            .onFailure {
                _ui.value = UiState.Error("Google Sign-In falló: ${it.message}")
            }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
        _eventos.value = emptyList()
        _misEventos.value = emptyList()
        _ui.value = UiState.Idle
    }

    fun clearErrorState() {
        if (_ui.value is UiState.Error) {
            _ui.value = UiState.Idle
        }
    }

    private fun eventosRef() = db.collection("eventos")

    // ---------- OPERACIONES CRUD ----------
    fun crearEvento(
        titulo: String,
        descripcion: String,
        ubicacion: String,
        fecha: Timestamp,
        categoria: String,
        maxParticipantes: String
    ) = viewModelScope.launch {
        val user = auth.currentUser ?: return@launch
        val max = maxParticipantes.toIntOrNull()

        if (titulo.isBlank()) {
            _ui.value = UiState.Error("El título no puede estar vacío")
            return@launch
        }
        if (categoria.isBlank()) {
            _ui.value = UiState.Error("Selecciona una categoría")
            return@launch
        }
        if (max == null || max <= 0) {
            _ui.value = UiState.Error("Ingresa un número válido de participantes")
            return@launch
        }

        _ui.value = UiState.Loading

        val data = mapOf(
            "titulo" to titulo.trim(),
            "descripcion" to descripcion.trim(),
            "ubicacion" to ubicacion.trim(),
            "fecha" to fecha,
            "categoria" to categoria.trim(),
            "organizador" to (user.displayName ?: user.email ?: "Anónimo"),
            "organizadorId" to user.uid,
            "participantes" to emptyList<String>(),
            "maxParticipantes" to max,
            "createdAt" to Timestamp.now()
        )

        runCatching { eventosRef().add(data).await() }
            .onSuccess {
                _ui.value = UiState.Info("Evento creado correctamente")
                cargarEventos()
            }
            .onFailure {
                _ui.value = UiState.Error("Error al crear evento: ${it.message}")
            }
    }

    fun cargarEventos() = viewModelScope.launch {
        _ui.value = UiState.Loading

        runCatching {
            eventosRef()
                .orderBy("fecha", Query.Direction.ASCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { d -> d.toObject(Evento::class.java)?.copy(id = d.id) }
        }.onSuccess { list ->
            _eventos.value = list
            _ui.value = UiState.Idle
        }.onFailure {
            _ui.value = UiState.Error("No se pudieron cargar los eventos: ${it.message}")
        }
    }

    fun cargarMisEventos() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        _ui.value = UiState.Loading

        runCatching {
            eventosRef()
                .whereEqualTo("organizadorId", uid)
                .orderBy("fecha", Query.Direction.ASCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { d -> d.toObject(Evento::class.java)?.copy(id = d.id) }
        }.onSuccess { list ->
            _misEventos.value = list
            _ui.value = UiState.Idle
        }.onFailure {
            _ui.value = UiState.Error("No se pudieron cargar tus eventos: ${it.message}")
        }
    }

    fun inscribirseEvento(eventoId: String) = viewModelScope.launch {
        val user = auth.currentUser ?: return@launch
        _ui.value = UiState.Loading

        val eventoRef = eventosRef().document(eventoId)

        runCatching {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventoRef)
                val evento = snapshot.toObject(Evento::class.java) ?: throw Exception("Evento no encontrado")

                if (evento.participantes.contains(user.uid)) {
                    throw Exception("Ya estás inscrito en este evento")
                }

                if (evento.participantes.size >= evento.maxParticipantes) {
                    throw Exception("El evento está lleno")
                }

                val nuevosParticipantes = evento.participantes + user.uid
                transaction.update(eventoRef, "participantes", nuevosParticipantes)
            }.await()
        }.onSuccess {
            _ui.value = UiState.Info("Te has inscrito correctamente")
            cargarEventos()
        }.onFailure {
            _ui.value = UiState.Error(it.message ?: "Error al inscribirse")
        }
    }

    fun desinscribirseEvento(eventoId: String) = viewModelScope.launch {
        val user = auth.currentUser ?: return@launch
        _ui.value = UiState.Loading

        val eventoRef = eventosRef().document(eventoId)

        runCatching {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(eventoRef)
                val evento = snapshot.toObject(Evento::class.java) ?: throw Exception("Evento no encontrado")

                val nuevosParticipantes = evento.participantes.filter { it != user.uid }
                transaction.update(eventoRef, "participantes", nuevosParticipantes)
            }.await()
        }.onSuccess {
            _ui.value = UiState.Info("Te has desinscrito del evento")
            cargarEventos()
        }.onFailure {
            _ui.value = UiState.Error(it.message ?: "Error al desinscribirse")
        }
    }

    fun eliminarEvento(eventoId: String) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        _ui.value = UiState.Loading

        runCatching {
            eventosRef().document(eventoId).delete().await()
        }.onSuccess {
            _ui.value = UiState.Info("Evento eliminado")
            cargarMisEventos()
            cargarEventos()
        }.onFailure {
            _ui.value = UiState.Error("No se pudo eliminar: ${it.message}")
        }
    }

    fun clearUiState() {
        _ui.value = UiState.Idle
    }

    fun formatearFecha(timestamp: Timestamp): String {
        return dateFormatter.format(timestamp.toDate())
    }

    fun estaInscrito(evento: Evento): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return evento.participantes.contains(uid)
    }

    fun esOrganizador(evento: Evento): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return evento.organizadorId == uid
    }
}