package com.example.seedcatalog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SeedGreen,
    onPrimary = SeedOnGreen,
    primaryContainer = SeedGreenContainer
)

@Composable
fun SeedCatalogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}
