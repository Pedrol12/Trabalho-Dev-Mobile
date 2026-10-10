package com.example.appdeteccaocachorro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandPrimary = Color(0xFF0077B6)
val BrandSecondary = Color(0xFF00B4D8)
val BrandAccent = Color(0xFF90E0EF)
val BrandSurface = Color(0xFFF8F9FA)
val BrandCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)
val SuccessGreen = Color(0xFF10B981)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    tertiary = BrandAccent,
    background = Color(0xFFF3F4F6),
    surface = Color.White,
    onSurface = TextPrimary,
    onBackground = TextPrimary
)

// O projeto deve obrigatoriamente seguir o tema Light Mode (Requisito Secao 7)
@Composable
fun AppDeteccaoCachorroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}