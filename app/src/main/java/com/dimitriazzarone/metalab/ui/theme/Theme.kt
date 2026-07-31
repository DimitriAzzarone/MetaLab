package com.dimitriazzarone.metalab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MetaLabColors = darkColorScheme(
    primary = MetaPrimary,
    onPrimary = MetaOnBackground,
    primaryContainer = MetaPrimaryContainer,
    onPrimaryContainer = MetaOnBackground,
    background = MetaBackground,
    onBackground = MetaOnBackground,
    surface = MetaSurface,
    onSurface = MetaOnBackground,
    onSurfaceVariant = MetaOnSurfaceVariant,
    error = MetaError
)

@Composable
fun MetaLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MetaLabColors,
        typography = MetaLabTypography,
        content = content
    )
}
