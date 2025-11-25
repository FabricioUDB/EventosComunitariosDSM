/**
 * Crea un nuevo evento en Firestore.
 *
 * Flujo de la función:
 * 1. Valida que los campos obligatorios del evento no estén vacíos.
 * 2. Asigna el ID del usuario autenticado como organizador.
 * 3. Envía los datos a la colección `eventos` en Firestore.
 * 4. Actualiza el estado de UI con éxito o error.
 *
 * @param evento Objeto que contiene los datos del nuevo evento.
 */
package com.example.eventoscomunitarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentChange // <--- NUEVO: Importante para detectar cambios
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableSharedFlow // <--- NUEVO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow // <--- NUEVO
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
    val createdAt: Timestamp = Timestamp.now(),
    val calificacionPromedio: Double = 0.0,
    val totalCalificaciones: Int = 0
)

// --- Modelo de Comentario ---
data class Comentario(
    val id: String = "",
    val eventoId: String = "",
    val usuarioId: String = "",
    val nombreUsuario: String = "",
    val comentario: String = "",
    val calificacion: Int = 0,
    val fecha: Timestamp = Timestamp.now()
)

// --- Estados de UI ---
sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Error(val message: String) : UiState()
    data class Info(val message: String) : UiState()
}

class EventsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var listenerEventos: ListenerRegistration? = null
    private var listenerMisEventos: ListenerRegistration? = null
    private var listenerHistorial: ListenerRegistration? = null

    // <--- NUEVO: Canal para enviar notificaciones a la vista
    private val _notificacion = MutableSharedFlow<String>()
    val notificacion = _notificacion.asSharedFlow()

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user.asStateFlow()

    private val _ui = MutableStateFlow<UiState>(UiState.Idle)
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos.asStateFlow()

    private val _misEventos = MutableStateFlow<List<Evento>>(emptyList())
    val misEventos: StateFlow<List<Evento>> = _misEventos.asStateFlow()

    private val _eventosPasados = MutableStateFlow<List<Evento>>(emptyList())
    val eventosPasados: StateFlow<List<Evento>> = _eventosPasados.asStateFlow()

    private val _comentariosEvento = MutableStateFlow<List<Comentario>>(emptyList())
    val comentariosEvento: StateFlow<List<Comentario>> = _comentariosEvento.asStateFlow()

    val categoriasEventos = listOf(
        "Deportes", "Cultura", "Educación", "Música", "Arte",
        "Gastronomía", "Tecnología", "Solidaridad", "Medio Ambiente", "Otros"
    )

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    init {
        try {
            db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (e: Exception) { }
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
            iniciarEscuchaDeDatos()
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
            iniciarEscuchaDeDatos()
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
                iniciarEscuchaDeDatos()
            }
            .onFailure {
                _ui.value = UiState.Error("Google Sign-In falló: ${it.message}")
            }
    }

    fun signOut() {
        detenerEscuchaDeDatos()
        auth.signOut()
        _user.value = null
        _eventos.value = emptyList()
        _misEventos.value = emptyList()
        _eventosPasados.value = emptyList()
        _comentariosEvento.value = emptyList()
        _ui.value = UiState.Idle
    }

    private fun iniciarEscuchaDeDatos() {
        cargarEventos()
        cargarMisEventos()
        cargarEventosPasados()
    }

    private fun detenerEscuchaDeDatos() {
        listenerEventos?.remove()
        listenerMisEventos?.remove()
        listenerHistorial?.remove()
        listenerEventos = null
        listenerMisEventos = null
        listenerHistorial = null
    }

    fun clearErrorState() {
        if (_ui.value is UiState.Error) {
            _ui.value = UiState.Idle
        }
    }

    private fun eventosRef() = db.collection("eventos")

    private fun comentariosRef(eventoId: String) =
        db.collection("eventos").document(eventoId).collection("comentarios")

    // ---------- OPERACIONES CRUD ----------
    fun crearEvento(
        titulo: String, descripcion: String, ubicacion: String,
        fecha: Timestamp, categoria: String, maxParticipantes: String
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
            "createdAt" to Timestamp.now(),
            "calificacionPromedio" to 0.0,
            "totalCalificaciones" to 0
        )

        runCatching { eventosRef().add(data).await() }
            .onSuccess { _ui.value = UiState.Info("Evento creado correctamente") }
            .onFailure { _ui.value = UiState.Error("Error al crear evento: ${it.message}") }
    }

    fun cargarEventos() {
        if (listenerEventos != null) return

        val ahora = Timestamp.now()
        listenerEventos = eventosRef()
            .whereGreaterThanOrEqualTo("fecha", ahora)
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    _ui.value = UiState.Error("Error al cargar eventos: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    // <--- NUEVO: DETECTAR MODIFICACIONES PARA NOTIFICAR
                    for (dc in snapshots.documentChanges) {
                        if (dc.type == DocumentChange.Type.MODIFIED) {
                            val evento = dc.document.toObject(Evento::class.java)
                            val uid = auth.currentUser?.uid
                            // Si el usuario está logueado y participa en el evento...
                            if (uid != null && evento.participantes.contains(uid)) {
                                viewModelScope.launch {
                                    // ... Enviamos la notificación
                                    _notificacion.emit("Hubo cambios en el evento: ${evento.titulo}")
                                }
                            }
                        }
                    }

                    val list = snapshots.documents.mapNotNull { d ->
                        d.toObject(Evento::class.java)?.copy(id = d.id)
                    }
                    _eventos.value = list
                }
            }
    }

    fun cargarEventosPasados() {
        if (listenerHistorial != null) return
        val ahora = Timestamp.now()
        listenerHistorial = eventosRef()
            .whereLessThan("fecha", ahora)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { d ->
                        d.toObject(Evento::class.java)?.copy(id = d.id)
                    }
                    _eventosPasados.value = list
                }
            }
    }

    fun cargarMisEventos() {
        val uid = auth.currentUser?.uid ?: return
        if (listenerMisEventos != null) return
        listenerMisEventos = eventosRef()
            .whereEqualTo("organizadorId", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    _ui.value = UiState.Error("Error al cargar mis eventos: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { d ->
                        d.toObject(Evento::class.java)?.copy(id = d.id)
                    }
                    _misEventos.value = list
                }
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
                if (evento.participantes.contains(user.uid)) throw Exception("Ya estás inscrito en este evento")
                if (evento.participantes.size >= evento.maxParticipantes) throw Exception("El evento está lleno")
                val nuevosParticipantes = evento.participantes + user.uid
                transaction.update(eventoRef, "participantes", nuevosParticipantes)
            }.await()
        }.onSuccess { _ui.value = UiState.Info("Te has inscrito correctamente") }
            .onFailure { _ui.value = UiState.Error(it.message ?: "Error al inscribirse") }
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
        }.onSuccess { _ui.value = UiState.Info("Te has desinscrito del evento") }
            .onFailure { _ui.value = UiState.Error(it.message ?: "Error al desinscribirse") }
    }

    fun eliminarEvento(eventoId: String) = viewModelScope.launch {
        _ui.value = UiState.Loading
        runCatching { eventosRef().document(eventoId).delete().await() }
            .onSuccess { _ui.value = UiState.Info("Evento eliminado") }
            .onFailure { _ui.value = UiState.Error("No se pudo eliminar: ${it.message}") }
    }

    fun agregarComentario(eventoId: String, comentario: String, calificacion: Int) = viewModelScope.launch {
        val user = auth.currentUser ?: return@launch
        if (comentario.isBlank()) {
            _ui.value = UiState.Error("El comentario no puede estar vacío")
            return@launch
        }
        _ui.value = UiState.Loading
        val data = mapOf(
            "eventoId" to eventoId,
            "usuarioId" to user.uid,
            "nombreUsuario" to (user.displayName ?: user.email ?: "Anónimo"),
            "comentario" to comentario.trim(),
            "calificacion" to calificacion,
            "fecha" to Timestamp.now()
        )
        runCatching {
            comentariosRef(eventoId).add(data).await()
            actualizarCalificacionEvento(eventoId)
        }.onSuccess {
            _ui.value = UiState.Info("Comentario agregado correctamente")
            cargarComentarios(eventoId)
        }.onFailure { _ui.value = UiState.Error("Error al agregar comentario: ${it.message}") }
    }

    private suspend fun actualizarCalificacionEvento(eventoId: String) {
        val comentarios = comentariosRef(eventoId).get().await()
            .documents.mapNotNull { it.toObject(Comentario::class.java) }
        if (comentarios.isNotEmpty()) {
            val promedio = comentarios.map { it.calificacion }.average()
            eventosRef().document(eventoId).update(
                mapOf("calificacionPromedio" to promedio, "totalCalificaciones" to comentarios.size)
            ).await()
        }
    }

    fun cargarComentarios(eventoId: String) = viewModelScope.launch {
        runCatching {
            comentariosRef(eventoId).orderBy("fecha", Query.Direction.DESCENDING)
                .get().await().documents
                .mapNotNull { d -> d.toObject(Comentario::class.java)?.copy(id = d.id) }
        }.onSuccess { list -> _comentariosEvento.value = list }
    }

    fun eliminarComentario(eventoId: String, comentarioId: String) = viewModelScope.launch {
        _ui.value = UiState.Loading
        runCatching {
            comentariosRef(eventoId).document(comentarioId).delete().await()
            actualizarCalificacionEvento(eventoId)
        }.onSuccess {
            _ui.value = UiState.Info("Comentario eliminado")
            cargarComentarios(eventoId)
        }.onFailure { _ui.value = UiState.Error("Error al eliminar: ${it.message}") }
    }

    fun yaComentaste(eventoId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return _comentariosEvento.value.any { it.usuarioId == uid }
    }

    fun clearUiState() { _ui.value = UiState.Idle }
    fun formatearFecha(timestamp: Timestamp): String = dateFormatter.format(timestamp.toDate())
    fun estaInscrito(evento: Evento): Boolean = evento.participantes.contains(auth.currentUser?.uid)
    fun esOrganizador(evento: Evento): Boolean = evento.organizadorId == auth.currentUser?.uid

    fun editarEvento(
        eventoId: String, titulo: String, descripcion: String,
        ubicacion: String, fecha: Timestamp, categoria: String, maxParticipantes: String
    ) = viewModelScope.launch {
        val max = maxParticipantes.toIntOrNull()
        if (titulo.isBlank()) { _ui.value = UiState.Error("El título no puede estar vacío"); return@launch }
        if (max == null || max <= 0) { _ui.value = UiState.Error("Ingresa un número válido de participantes"); return@launch }

        _ui.value = UiState.Loading
        val data = mapOf(
            "titulo" to titulo.trim(), "descripcion" to descripcion.trim(),
            "ubicacion" to ubicacion.trim(), "fecha" to fecha,
            "categoria" to categoria.trim(), "maxParticipantes" to max
        )
        runCatching { eventosRef().document(eventoId).update(data).await() }
            .onSuccess { _ui.value = UiState.Info("Evento actualizado correctamente") }
            .onFailure { _ui.value = UiState.Error("Error al actualizar: ${it.message}") }
    }

    override fun onCleared() {
        super.onCleared()
        detenerEscuchaDeDatos()
    }
}