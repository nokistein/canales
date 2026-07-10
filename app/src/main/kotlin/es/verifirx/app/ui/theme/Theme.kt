package es.verifirx.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VerifiRxGreen = Color(0xFF0B6E4F)
val VerifiRxGreenDark = Color(0xFF6FDBAE)
val MatchColor = Color(0xFF1E8E3E)
val MismatchColor = Color(0xFFD93025)
val ReviewColor = Color(0xFFE8A400)

private val LightColors = lightColorScheme(
    primary = VerifiRxGreen,
    onPrimary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = VerifiRxGreenDark,
    onPrimary = Color(0xFF00391F),
)

@Composable
fun VerifiRxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
