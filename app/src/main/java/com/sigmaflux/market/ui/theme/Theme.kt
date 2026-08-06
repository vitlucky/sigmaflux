package com.sigmaflux.market.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Graphite Premium palette (зафиксировано пользователем).
 * Фон глубокий графит, мягкий фиолетовый акцент, muted mint/rose, amber caution.
 * Минимум яркого синего.
 */
object Graphite {
    val Background = Color(0xFF16161C)
    val Surface = Color(0xFF1A1A22)
    val Elevated = Color(0xFF23232D)
    val Text = Color(0xFFF2EFF7)
    val Muted = Color(0xFFA49EB0)
    val Accent = Color(0xFFB7A1FF)
    val Positive = Color(0xFF87D5B6)
    val Negative = Color(0xFFE9A0B0)
    val Warning = Color(0xFFD8B875)
    val Danger = Color(0xFFE4574F)

    // Точки источников новостей
    val DotOfficial = Color(0xFF8FA6C7)   // muted blue/neutral
    val DotMedia = Color(0xFF7C7F9E)      // gray-blue
    val DotTelegram = Color(0xFFD8B875)   // tiny amber

    val Outline = Color(0xFF2E2E3A)
    val OutlineVariant = Color(0xFF26262F)
}

private val GraphiteDark = darkColorScheme(
    primary = Graphite.Accent,
    onPrimary = Color(0xFF1A1426),
    primaryContainer = Color(0xFF2E2740),
    onPrimaryContainer = Graphite.Text,
    secondary = Graphite.Muted,
    onSecondary = Color(0xFF1A1A22),
    background = Graphite.Background,
    onBackground = Graphite.Text,
    surface = Graphite.Surface,
    onSurface = Graphite.Text,
    surfaceVariant = Graphite.Elevated,
    onSurfaceVariant = Graphite.Muted,
    outline = Graphite.Outline,
    outlineVariant = Graphite.OutlineVariant,
    error = Graphite.Danger,
    onError = Color.White,
    surfaceContainer = Graphite.Surface,
    surfaceContainerHigh = Graphite.Elevated,
    surfaceContainerHighest = Graphite.Elevated
)

// Светлая тема — тоже тёмная графика (приложение терминального типа), но при желании
// можно расширить. Оставляем единый Graphite-режим для консистентности.
private val GraphiteLight = GraphiteDark

@Composable
fun SigmaFluxTheme(content: @Composable () -> Unit) {
    // widget/приложение следуют системной теме, палитра одинаковая в обоих случаях
    val colors = if (isSystemInDarkTheme()) GraphiteDark else GraphiteLight
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
