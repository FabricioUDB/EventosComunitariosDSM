package com.example.eventoscomunitarios.ui.theme

import androidx.compose.ui.graphics.Color

// Colores principales - Tema vibrante para eventos comunitarios
val PrimaryLight = Color(0xFF6200EE)
val PrimaryDark = Color(0xFFBB86FC)
val SecondaryLight = Color(0xFF03DAC6)
val SecondaryDark = Color(0xFF03DAC6)
val TertiaryLight = Color(0xFFFF6B6B)
val TertiaryDark = Color(0xFFFF8787)

// Colores de fondo
val BackgroundLight = Color(0xFFFAFAFA)
val BackgroundDark = Color(0xFF121212)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1E1E1E)

// Colores para categorías de eventos
val ColorDeportes = Color(0xFF4CAF50)
val ColorCultura = Color(0xFF9C27B0)
val ColorEducacion = Color(0xFF2196F3)
val ColorMusica = Color(0xFFE91E63)
val ColorArte = Color(0xFFFF9800)
val ColorGastronomia = Color(0xFFFF5722)
val ColorTecnologia = Color(0xFF00BCD4)
val ColorSolidaridad = Color(0xFFFFC107)
val ColorMedioAmbiente = Color(0xFF8BC34A)
val ColorOtros = Color(0xFF9E9E9E)

// Colores de estado
val SuccessColor = Color(0xFF4CAF50)
val ErrorColor = Color(0xFFEF5350)
val WarningColor = Color(0xFFFFA726)
val InfoColor = Color(0xFF29B6F6)

// Gradientes (se usarán en compose con Brush)
val GradientStart = Color(0xFF667eea)
val GradientEnd = Color(0xFF764ba2)

val GradientStartLight = Color(0xFFf093fb)
val GradientEndLight = Color(0xFFf5576c)

// Función para obtener color por categoría
fun getCategoryColor(categoria: String): Color {
    return when (categoria) {
        "Deportes" -> ColorDeportes
        "Cultura" -> ColorCultura
        "Educación" -> ColorEducacion
        "Música" -> ColorMusica
        "Arte" -> ColorArte
        "Gastronomía" -> ColorGastronomia
        "Tecnología" -> ColorTecnologia
        "Solidaridad" -> ColorSolidaridad
        "Medio Ambiente" -> ColorMedioAmbiente
        else -> ColorOtros
    }
}

// Colores legacy (mantenemos compatibilidad)
val Purple80 = PrimaryDark
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = PrimaryLight
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = TertiaryLight