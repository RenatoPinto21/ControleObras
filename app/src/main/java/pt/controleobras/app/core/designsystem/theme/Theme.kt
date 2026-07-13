package pt.controleobras.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary              = OrangePrimaryDark,
    onPrimary            = OnOrangePrimaryDark,
    primaryContainer     = OrangePrimaryContainerDark,
    onPrimaryContainer   = OnOrangePrimaryContainerDark,
    secondary            = WarmSecondaryDark,
    onSecondary          = OnWarmSecondaryDark,
    secondaryContainer   = WarmSecondaryContainerDark,
    onSecondaryContainer = OnWarmSecondaryContainerDark,
    tertiary             = SlateTertiaryDark,
    onTertiary           = OnSlateTertiaryDark,
    tertiaryContainer    = SlateTertiaryContainerDark,
    onTertiaryContainer  = OnSlateTertiaryContainerDark,
    background           = BackgroundDark,
    onBackground         = OnBackgroundDark,
    surface              = IndustrialSurface,
    surfaceVariant       = IndustrialSurface2,
    onSurface            = OnBackgroundDark,
    onSurfaceVariant     = IndustrialSteel,
    outline              = IndustrialBorder,
    error                = ErrorDark,
    onError              = OnErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimaryLight,
    onPrimary = OnOrangePrimaryLight,
    primaryContainer = OrangePrimaryContainerLight,
    onPrimaryContainer = OnOrangePrimaryContainerLight,
    secondary = WarmSecondaryLight,
    onSecondary = OnWarmSecondaryLight,
    secondaryContainer = WarmSecondaryContainerLight,
    onSecondaryContainer = OnWarmSecondaryContainerLight,
    tertiary = SlateTertiaryLight,
    onTertiary = OnSlateTertiaryLight,
    tertiaryContainer = SlateTertiaryContainerLight,
    onTertiaryContainer = OnSlateTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = BackgroundLight,
    onSurface = OnBackgroundLight,
    error = ErrorLight,
    onError = OnErrorLight
)

/**
 * Tema da marca Controle Obras (laranja de obra/segurança, #FF6D00).
 * Não usa dynamic color (Android 12+) de propósito — a app mantém sempre
 * a paleta da marca em vez de seguir o wallpaper do dispositivo, para
 * garantir identidade visual consistente entre tablets da empresa.
 */
/**
 * Tema industrial da marca — SEMPRE escuro.
 * A identidade visual de obra/aço/laranja é exclusivamente dark.
 * O parâmetro darkTheme é mantido por compatibilidade mas ignorado.
 */
@Composable
fun ControleObrasTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
