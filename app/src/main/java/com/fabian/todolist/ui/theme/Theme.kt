package com.fabian.todolist.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Midnight Slate Scheme (Premium Blue Accents)
private val BlueLightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FF),
    surface = Color.White,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4FA),
    surfaceContainer = Color(0xFFE5EEF9),
    surfaceContainerHigh = Color(0xFFDAE7F7),
    surfaceContainerHighest = Color(0xFFCEE0F5),
    surfaceVariant = Color(0xFFDAE3F0),
    onSurfaceVariant = Color(0xFF42474E),
    onBackground = Color(0xFF111C2B),
    onSurface = Color(0xFF111C2B)
)
private val BlueDarkColors = darkColorScheme(
    primary = Color(0xFFD1E4FF), 
    onPrimary = Color(0xFF00315E),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBFC6DC),
    onSecondary = Color(0xFF293041),
    background = Color(0xFF09121F),
    surface = Color(0xFF0F1B2D),
    surfaceContainerLowest = Color(0xFF0B1524),
    surfaceContainerLow = Color(0xFF142238),
    surfaceContainer = Color(0xFF192A44),
    surfaceContainerHigh = Color(0xFF1F3351),
    surfaceContainerHighest = Color(0xFF263C5E),
    surfaceVariant = Color(0xFF203554),
    onSurfaceVariant = Color(0xFFC2D5EE),
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6)
)

// Emerald Mint Scheme
private val GreenLightColors = lightColorScheme(
    primary = Color(0xFF0F9D58),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4EA),
    onPrimaryContainer = Color(0xFF137333),
    secondary = Color(0xFF137333),
    onSecondary = Color.White,
    background = Color(0xFFF5FAF6),
    surface = Color.White,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF7F2),
    surfaceContainer = Color(0xFFE6F2EA),
    surfaceContainerHigh = Color(0xFFDCECE2),
    surfaceContainerHighest = Color(0xFFD2E6DA),
    surfaceVariant = Color(0xFFD8EADF),
    onSurfaceVariant = Color(0xFF325240),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val GreenDarkColors = darkColorScheme(
    primary = Color(0xFF81C995),
    onPrimary = Color(0xFF137333),
    primaryContainer = Color(0xFF137333),
    onPrimaryContainer = Color(0xFFE6F4EA),
    secondary = Color(0xFF81C995),
    onSecondary = Color(0xFF137333),
    background = Color(0xFF07140E),
    surface = Color(0xFF0E2218),
    surfaceContainerLowest = Color(0xFF0A1B12),
    surfaceContainerLow = Color(0xFF12291D),
    surfaceContainer = Color(0xFF163223),
    surfaceContainerHigh = Color(0xFF1B3C2A),
    surfaceContainerHighest = Color(0xFF214631),
    surfaceVariant = Color(0xFF1E3F2E),
    onSurfaceVariant = Color(0xFFB5D4C3),
    onBackground = Color(0xFFF1F8F4),
    onSurface = Color(0xFFF1F8F4)
)

// Sunset Coral Scheme
private val CoralLightColors = lightColorScheme(
    primary = Color(0xFFEA4335),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE8E6),
    onPrimaryContainer = Color(0xFFC5221F),
    secondary = Color(0xFFC5221F),
    onSecondary = Color.White,
    background = Color(0xFFFDF6F5),
    surface = Color.White,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF0EF),
    surfaceContainer = Color(0xFFF7E6E4),
    surfaceContainerHigh = Color(0xFFF2DCDA),
    surfaceContainerHighest = Color(0xFFEDD2CF),
    surfaceVariant = Color(0xFFF5D9D6),
    onSurfaceVariant = Color(0xFF5A302D),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val CoralDarkColors = darkColorScheme(
    primary = Color(0xFFF28B82),
    onPrimary = Color(0xFFC5221F),
    primaryContainer = Color(0xFFC5221F),
    onPrimaryContainer = Color(0xFFFCE8E6),
    secondary = Color(0xFFF28B82),
    onSecondary = Color(0xFFC5221F),
    background = Color(0xFF1D0B09),
    surface = Color(0xFF2B120E),
    surfaceContainerLowest = Color(0xFF200D0A),
    surfaceContainerLow = Color(0xFF351813),
    surfaceContainer = Color(0xFF401F19),
    surfaceContainerHigh = Color(0xFF4B261F),
    surfaceContainerHighest = Color(0xFF562C25),
    surfaceVariant = Color(0xFF48241D),
    onSurfaceVariant = Color(0xFFF7C9C4),
    onBackground = Color(0xFFFFECEB),
    onSurface = Color(0xFFFFECEB)
)

// Orchid Purple Scheme
private val PurpleLightColors = lightColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF6D28D9),
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    background = Color(0xFFFAF5FF),
    surface = Color.White,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6EEFF),
    surfaceContainer = Color(0xFFEFE2FE),
    surfaceContainerHigh = Color(0xFFE7D4FD),
    surfaceContainerHighest = Color(0xFFDFC6FC),
    surfaceVariant = Color(0xFFE9D8FD),
    onSurfaceVariant = Color(0xFF4C3078),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val PurpleDarkColors = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF6D28D9),
    primaryContainer = Color(0xFF6D28D9),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFC4B5FD),
    onSecondary = Color.White,
    background = Color(0xFF130A24),
    surface = Color(0xFF1F1138),
    surfaceContainerLowest = Color(0xFF170C2A),
    surfaceContainerLow = Color(0xFF271646),
    surfaceContainer = Color(0xFF301C55),
    surfaceContainerHigh = Color(0xFF3B2366),
    surfaceContainerHighest = Color(0xFF462977),
    surfaceVariant = Color(0xFF382062),
    onSurfaceVariant = Color(0xFFE2D6FF),
    onBackground = Color(0xFFF5F1FA),
    onSurface = Color(0xFFF5F1FA)
)

// Pixel Gold Scheme
private val GoldLightColors = lightColorScheme(
    primary = Color(0xFFE29500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF7E0),
    onPrimaryContainer = Color(0xFFB06000),
    secondary = Color(0xFFB06000),
    onSecondary = Color.White,
    background = Color(0xFFFFFDF5),
    surface = Color.White,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF3DC),
    surfaceContainer = Color(0xFFF5E8C3),
    surfaceContainerHigh = Color(0xFFEEDDAA),
    surfaceContainerHighest = Color(0xFFE6D191),
    surfaceVariant = Color(0xFFF2E2B5),
    onSurfaceVariant = Color(0xFF544218),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val GoldDarkColors = darkColorScheme(
    primary = Color(0xFFFDD663),
    onPrimary = Color(0xFFB06000),
    primaryContainer = Color(0xFFB06000),
    onPrimaryContainer = Color(0xFFFEF7E0),
    secondary = Color(0xFFFDD663),
    onSecondary = Color(0xFFB06000),
    background = Color(0xFF1A1303),
    surface = Color(0xFF261D07),
    surfaceContainerLowest = Color(0xFF1F1705),
    surfaceContainerLow = Color(0xFF31250B),
    surfaceContainer = Color(0xFF3C2F0E),
    surfaceContainerHigh = Color(0xFF483812),
    surfaceContainerHighest = Color(0xFF544216),
    surfaceVariant = Color(0xFF453610),
    onSurfaceVariant = Color(0xFFF7E6B8),
    onBackground = Color(0xFFFFF3DE),
    onSurface = Color(0xFFFFF3DE)
)

// Lavender Pink Scheme
private val PinkLightColors = lightColorScheme(
    primary = Color(0xFFE91E63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4EC),
    onPrimaryContainer = Color(0xFFC2185B),
    secondary = Color(0xFFC2185B),
    onSecondary = Color.White,
    background = Color(0xFFFFF2F6),
    surface = Color.White,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCE9F0),
    surfaceContainer = Color(0xFFF8D7E3),
    surfaceContainerHigh = Color(0xFFF3C5D6),
    surfaceContainerHighest = Color(0xFFEEB2C9),
    surfaceVariant = Color(0xFFF6CEDD),
    onSurfaceVariant = Color(0xFF5E2B41),
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)
private val PinkDarkColors = darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFFC2185B),
    primaryContainer = Color(0xFFC2185B),
    onPrimaryContainer = Color(0xFFFCE4EC),
    secondary = Color(0xFFF48FB1),
    onSecondary = Color(0xFFC2185B),
    background = Color(0xFF1F0813),
    surface = Color(0xFF2C0F1D),
    surfaceContainerLowest = Color(0xFF220A17),
    surfaceContainerLow = Color(0xFF381426),
    surfaceContainer = Color(0xFF441A30),
    surfaceContainerHigh = Color(0xFF51203A),
    surfaceContainerHighest = Color(0xFF5F2645),
    surfaceVariant = Color(0xFF4E1D37),
    onSurfaceVariant = Color(0xFFF9C8DE),
    onBackground = Color(0xFFFFF0F5),
    onSurface = Color(0xFFFFF0F5)
)

@Composable
fun FabiToDoTheme(
    selectedColor: String = "system", // system, blue, green, coral, purple, gold, pink
    selectedDarkTheme: String = "system", // system, light, dark
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (selectedDarkTheme) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        // Material You dynamic system colors (on Android 12+)
        selectedColor == "system" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallbacks and palette choices
        selectedColor == "blue" -> if (useDark) BlueDarkColors else BlueLightColors
        selectedColor == "green" -> if (useDark) GreenDarkColors else GreenLightColors
        selectedColor == "coral" -> if (useDark) CoralDarkColors else CoralLightColors
        selectedColor == "purple" -> if (useDark) PurpleDarkColors else PurpleLightColors
        selectedColor == "gold" -> if (useDark) GoldDarkColors else GoldLightColors
        selectedColor == "pink" -> if (useDark) PinkDarkColors else PinkLightColors
        // Default theme is Pixel Blue (if system dynamic color is unsupported or default is preferred)
        else -> if (useDark) BlueDarkColors else BlueLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
