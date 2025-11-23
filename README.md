# 🎉 Eventos Comunitarios

<div align="center">

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

**Aplicación Android para conectar comunidades a través de eventos**

</div>

---

## 📋 Información del Proyecto

**Universidad:** Universidad Don Bosco  
**Materia:** Desarrollo de Software para Móviles (DSM104)  
**Proyecto:** Segundo Proyecto en Android con Kotlin  
**Ciclo:** 01-2025

### 👥 Equipo de Desarrollo

| Nombre | Carnet |
|--------|--------|
| Fabricio Antonio Castro Martínez | CM240137 |
| José Alonso Aguirre Márquez | AM241838 |
| Ángel Marcelo Delgado Estrada | DE241507 |

---

## 📱 Descripción

Aplicación Android nativa que facilita la organización y participación en eventos comunitarios locales. Los usuarios pueden crear, descubrir y participar en eventos de diversas categorías con un sistema completo de calificaciones y licencias Creative Commons.

---

## ✨ Características Principales

### 🔐 Autenticación
- Login con email/contraseña
- Google Sign-In
- Gestión segura con Firebase Authentication

### 📅 Gestión de Eventos (CRUD)
- Crear, editar, eliminar eventos
- 10 categorías: Deportes, Cultura, Educación, Música, Arte, Gastronomía, Tecnología, Solidaridad, Medio Ambiente, Otros
- Inscripción/desinscripción con límite de participantes
- Navegación entre eventos próximos, propios e históricos

### ⭐ Sistema de Calificaciones
- Comentarios post-evento
- Calificación de 1-5 estrellas
- Promedio de calificaciones visible

### 📜 Licencias Creative Commons
- **7 licencias CC 4.0 implementadas:** CC BY, CC BY-SA, CC BY-NC, CC BY-NC-SA, CC BY-ND, CC BY-NC-ND, CC0
- Selector intuitivo con información educativa
- Visualización en cada evento
- Enlaces a documentación oficial

### 📱 Funcionalidades Adicionales
- 🔔 Notificaciones push
- 📅 Integración con calendario
- 🔗 Compartir eventos en redes sociales
- 🌙 Tema claro/oscuro
- 💾 Cache local
- ⚡ Actualizaciones en tiempo real

---

## 🛠️ Tecnologías

- **Lenguaje:** Kotlin 2.0.21
- **UI:** Jetpack Compose + Material Design 3
- **Arquitectura:** MVVM (Model-View-ViewModel)
- **Backend:** Firebase (Authentication + Firestore)
- **Asíncronia:** Kotlin Coroutines + Flow
- **SDK:** Min 24, Target 36

---

## 🚀 Instalación Rápida

### 1. Clonar Repositorio
```bash
git clone https://github.com/tu-usuario/eventos-comunitarios.git
cd eventos-comunitarios
```

### 2. Configurar Firebase

1. Crear proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Agregar app Android con package: `com.example.eventoscomunitarios`
3. Descargar `google-services.json` → `app/google-services.json`
4. Habilitar **Authentication** (Email/Password y Google)
5. Crear base de datos **Firestore**
6. Configurar reglas de seguridad:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /eventos/{eventoId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && request.resource.data.organizadorId == request.auth.uid;
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.organizadorId;
      
      match /comentarios/{comentarioId} {
        allow read: if request.auth != null;
        allow create: if request.auth != null && request.resource.data.usuarioId == request.auth.uid;
        allow delete: if request.auth != null && request.auth.uid == resource.data.usuarioId;
      }
    }
  }
}
```

7. Obtener SHA-1 y agregarlo a Firebase:
```bash
./gradlew signingReport
```

8. Copiar **Web Client ID** de Google Sign-In a `res/values/strings.xml`:
```xml
<string name="default_web_client_id">TU_CLIENT_ID.apps.googleusercontent.com</string>
```

### 3. Ejecutar

```bash
./gradlew clean build
./gradlew installDebug
```

O desde Android Studio: **Run** ▶️

---

## 📂 Estructura del Proyecto

```
app/src/main/java/com/example/eventoscomunitarios/
├── MainActivity.kt              # UI principal con Compose
├── EventsViewModel.kt           # Lógica de negocio
├── CreativeCommons.kt           # Modelo de licencias CC
├── CreativeCommonsUI.kt         # Componentes UI de licencias
└── ui/theme/                    # Tema, colores y tipografía
```

### Estructura Firestore

```javascript
eventos/
└── {eventoId}/
    ├── titulo, descripcion, ubicacion, fecha
    ├── categoria, organizador, organizadorId
    ├── participantes: Array, maxParticipantes
    ├── calificacionPromedio, totalCalificaciones
    ├── licenciaCC: String              // Creative Commons
    └── comentarios/{comentarioId}/
        ├── usuarioId, nombreUsuario, comentario
        ├── calificacion, fecha
```

---

## 📜 Licencias Creative Commons

Implementación completa del sistema CC 4.0 para proteger contenido de usuarios:

| Licencia | Comercial | Modificar | Compartir Igual |
|----------|-----------|-----------|-----------------|
| CC BY | ✅ | ✅ | ❌ |
| CC BY-SA | ✅ | ✅ | ✅ |
| CC BY-NC | ❌ | ✅ | ❌ |
| CC BY-NC-SA | ❌ | ✅ | ✅ |
| CC BY-ND | ✅ | ❌ | ❌ |
| CC BY-NC-ND | ❌ | ❌ | ❌ |
| CC0 | ✅ | ✅ | ❌ |

Cada evento incluye selector de licencia con información educativa y enlaces oficiales.

---

## 🎨 Paleta de Colores

**Principales:**
- Púrpura: `#6200EE`
- Verde azulado: `#03DAC6`
- Rosa coral: `#FF6B6B`

**Por Categoría:**
- 🏃 Deportes: `#4CAF50` | 🎭 Cultura: `#9C27B0` | 📚 Educación: `#2196F3`
- 🎵 Música: `#E91E63` | 🎨 Arte: `#FF9800` | 🍽️ Gastronomía: `#FF5722`
- 💻 Tecnología: `#00BCD4` | 🤝 Solidaridad: `#FFC107` | 🌱 Medio Ambiente: `#8BC34A`

---

## 🐛 Solución de Problemas

**Firebase no inicializa:** Verifica `google-services.json` en `app/`  
**Google Sign-In falla:** Confirma SHA-1 en Firebase y Web Client ID en strings.xml  
**App lenta:** Usa modo Release y dispositivo físico  
**Errores de compilación:** `./gradlew clean` + Invalidate Caches

---

## 📊 Estadísticas

- **Líneas de código:** ~3,500
- **Composables:** ~40
- **Funciones ViewModel:** 15+
- **Licencias CC:** 7

---

## 📞 Contacto

**Universidad Don Bosco**  

DSM104 - Desarrollo de Software para Móviles

---

## 🔄 Metodología de Trabajo - Scrum

Este proyecto se desarrolló utilizando la metodología **Scrum** con sprints de 2 semanas.

### 👥 Roles del Equipo

| Integrante | Carnet | Rol Scrum | Responsabilidades |
|------------|--------|-----------|-------------------|
| **Fabricio Antonio Castro Martínez** | CM240137 | **Scrum Master / Backend Developer** | • Facilitar ceremonias Scrum<br>• Gestión de Firebase (Auth + Firestore)<br>• ViewModel y lógica de negocio<br>• Sistema de notificaciones |
| **José Alonso Aguirre Márquez** | AM241838 | **Product Owner / Frontend Developer** | • Definir requisitos y prioridades<br>• UI/UX con Jetpack Compose<br>• Sistema de temas y colores<br>• Integración con Google Calendar |
| **Ángel Marcelo Delgado Estrada** | DE241507 | **Developer / QA** | • Sistema de licencias Creative Commons<br>• Testing y validación<br>• Documentación técnica<br>• Sistema de calificaciones |

### 📊 Gestión del Proyecto

**Herramienta:** Trello / Notion (Metodología Kanban)

🔗 **[Ver Tablero del Proyecto](https://trello.com/w/segundoproyectoenandroidconkotlin/home)**

### 📅 Sprints Realizados

#### Sprint 1 (Semana 1-2): Fundamentos
- ✅ Configuración inicial del proyecto
- ✅ Integración con Firebase
- ✅ Sistema de autenticación
- ✅ Estructura MVVM base

#### Sprint 2 (Semana 3-4): CRUD de Eventos
- ✅ Crear eventos
- ✅ Listar eventos
- ✅ Editar eventos
- ✅ Eliminar eventos
- ✅ Sistema de categorías

#### Sprint 3 (Semana 5-6): Funcionalidades Avanzadas
- ✅ Sistema de participación
- ✅ Sistema de calificaciones
- ✅ Notificaciones push
- ✅ Integración con calendario

#### Sprint 4 (Semana 7-8): Creative Commons y Pulido
- ✅ Implementación de 7 licencias CC
- ✅ UI/UX mejorado
- ✅ Testing completo
- ✅ Documentación final

### 🎯 Ceremonias Scrum

- **Daily Standup:** Lunes, Miércoles, Viernes (15 min)
- **Sprint Planning:** Inicio de cada sprint (2 horas)
- **Sprint Review:** Final de cada sprint (1 hora)
- **Sprint Retrospective:** Final de cada sprint (1 hora)

### 📋 Tablero Kanban

Nuestro tablero está organizado en las siguientes columnas:

1. **📝 Backlog** - Tareas pendientes
2. **🎯 To Do** - Tareas planificadas para el sprint actual
3. **🔨 In Progress** - Tareas en desarrollo
4. **🧪 Testing** - Tareas en pruebas
5. **✅ Done** - Tareas completadas

---

## 📖 Documentación Adicional

Para más información sobre la implementación de licencias Creative Commons, consulta: [CREATIVE_COMMONS.md](CREATIVE_COMMONS.md)

---

## 🎯 Funcionalidades Destacadas

### CRUD Completo
✅ Crear, leer, actualizar y eliminar eventos  
✅ Validación de formularios en tiempo real  
✅ Confirmaciones de seguridad  

### Sistema de Participación
✅ Inscripción/desinscripción instantánea  
✅ Control de capacidad máxima  
✅ Notificaciones de cambios  

### Calificaciones
✅ Sistema de estrellas (1-5)  
✅ Comentarios textuales  
✅ Promedio visible para todos  

### Creative Commons
✅ 7 licencias oficiales CC 4.0  
✅ Selector educativo con descripciones  
✅ Visualización en todas las vistas  

---

## 🔧 Mantenimiento

### Actualizar Dependencias
```bash
./gradlew dependencyUpdates
```

### Limpiar Proyecto
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### Generar APK Firmada
```bash
./gradlew assembleRelease
```

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT para el **código fuente**.  
Las **licencias Creative Commons** aplican únicamente al **contenido generado por usuarios** (eventos).

---

<div align="center">

**🎓 Universidad Don Bosco**  
**Desarrollo de Software para Móviles (DSM104)**  


⭐ Si te gustó el proyecto, dale una estrella ⭐

</div> Basados en categoría de evento
- 📊 **Barras de Progreso:** Visualización de participantes
- ⭐ **Estrellas Interactivas:** Sistema de calificación visual

---

## 🧪 Testing

### Unit Tests

```bash
./gradlew test
```

### Instrumentation Tests

```bash
./gradlew connectedAndroidTest
```

### Tests Incluidos

- ✅ Validación de formularios
- ✅ Lógica de inscripción/desinscripción
- ✅ Cálculo de espacios disponibles
- ✅ Sistema de calificaciones
- ✅ Formateo de fechas

---

## 🐛 Solución de Problemas

### Error: "Default FirebaseApp is not initialized"

**Solución:** Verifica que `google-services.json` esté en `app/google-services.json`

### Error: Google Sign-In no funciona

**Solución:**
1. Verifica que el SHA-1 esté en Firebase
2. Confirma el Web Client ID en `strings.xml`
3. Asegúrate de que Google Sign-In esté habilitado

### La app es lenta

**Solución:**
1. Prueba en modo Release
2. Usa un dispositivo físico
3. Verifica tu conexión a internet
4. Revisa que los índices de Firestore estén creados

### Errores de compilación

**Solución:**
```bash
# Limpiar caché
./gradlew clean
File > Invalidate Caches / Restart

# Actualizar dependencias
./gradlew --refresh-dependencies
```

---

## 🔐 Seguridad

### Medidas Implementadas

- ✅ Autenticación obligatoria para todas las operaciones
- ✅ Reglas de seguridad en Firestore
- ✅ Validación en cliente y servidor
- ✅ Encriptación de datos en tránsito (HTTPS)
- ✅ Tokens de sesión seguros
- ✅ No se exponen APIs keys sensibles

### Reglas de Firestore

Las reglas garantizan que:
- Solo usuarios autenticados pueden acceder
- Solo los organizadores pueden editar/eliminar sus eventos
- Solo los participantes pueden calificar eventos
- Validación de estructura de datos

---

## 📊 Métricas del Proyecto


### Funcionalidades

- ✅ CRUD completo de eventos
- ✅ Sistema de autenticación
- ✅ Sistema de participación
- ✅ Sistema de calificaciones
- ✅ Sistema de licencias CC
- ✅ Notificaciones push
- ✅ Compartir en redes
- ✅ Integración con calendario

---

## 🚧 Roadmap Futuro

### Funcionalidades Planeadas

- [ ] 🗺️ Mapa de eventos cercanos (Google Maps)
- [ ] 🔍 Búsqueda avanzada con filtros
- [ ] 👤 Perfiles de usuario personalizados
- [ ] 📸 Galería de fotos de eventos
- [ ] 💬 Chat entre participantes
- [ ] 🏆 Sistema de badges y logros
- [ ] 📧 Notificaciones por email
- [ ] 🌐 Versión web con sync
- [ ] 📱 App para iOS
- [ ] 🤖 Recomendaciones con IA

### Mejoras Técnicas

- [ ] Testing automatizado completo
- [ ] CI/CD con GitHub Actions
- [ ] Análisis de código con SonarQube
- [ ] Optimización de rendimiento
- [ ] Modo offline completo
- [ ] Migración a Kotlin Multiplatform

---



---

## 📄 Licencia del Proyecto

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

### 📜 Sobre las Licencias Creative Commons

Las licencias Creative Commons implementadas en esta aplicación son para el **contenido generado por los usuarios** (eventos), no para el código fuente de la aplicación.

---

## 🙏 Agradecimientos

- **Universidad Don Bosco** - Por la formación académica
- **Firebase** - Por los servicios backend
- **Jetpack Compose** - Por el framework de UI moderno
- **Creative Commons** - Por el sistema de licencias
- **Material Design** - Por las guías de diseño
- **Comunidad de Android** - Por la documentación y recursos

---

## 📞 Contacto

### Equipo de Desarrollo

- **Fabricio Castro** - CM240137
- **José Alonso Aguirre** - AM241838
- **Ángel Delgado** - DE241507

### Universidad

**Universidad Don Bosco**  

Desarrollo de Software para Móviles (DSM104)

---



[⬆ Volver arriba](#-eventos-comunitarios)

</div>
