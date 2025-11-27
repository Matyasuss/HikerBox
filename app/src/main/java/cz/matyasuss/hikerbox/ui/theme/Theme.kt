package cz.matyasuss.hikerbox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreen80,
    onPrimary = DeepForest,
    primaryContainer = ForestGreen40,
    onPrimaryContainer = ForestGreen80,

    secondary = MossGreen80,
    onSecondary = DeepForest,
    secondaryContainer = MossGreen40,
    onSecondaryContainer = MossGreen80,

    tertiary = EarthBrown80,
    onTertiary = DeepForest,
    tertiaryContainer = EarthBrown40,
    onTertiaryContainer = EarthBrown80,

    background = DeepForest,
    onBackground = LightMist,
    surface = DarkMoss,
    onSurface = LightMist,
    surfaceVariant = ForestShadow,
    onSurfaceVariant = PaleGreen,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen40,
    onPrimary = Color.White,
    primaryContainer = ForestGreen80,
    onPrimaryContainer = ForestGreen20,

    secondary = MossGreen60,
    onSecondary = Color.White,
    secondaryContainer = MossGreen80,
    onSecondaryContainer = ForestGreen20,

    tertiary = EarthBrown60,
    onTertiary = Color.White,
    tertiaryContainer = EarthBrown80,
    onTertiaryContainer = ForestGreen20,

    background = LightMist,
    onBackground = DeepForest,
    surface = Color.White,
    onSurface = DeepForest,
    surfaceVariant = PaleGreen,
    onSurfaceVariant = ForestGreen40,

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun HikerBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled to use custom forest theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}