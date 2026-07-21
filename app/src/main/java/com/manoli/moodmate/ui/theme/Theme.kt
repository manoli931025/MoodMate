package com.manoli.moodmate.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Esquema Clásico (Moderno) ──
private val ClassicLight = lightColorScheme(
    primary = Color(0xFF1B3A5C),           // Azul marino profundo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFFFF6B6B),         // Coral vibrante
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFF007B7F),          // Verde azulado
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA6F1F4),
    onTertiaryContainer = Color(0xFF002021),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FE),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7E8F0),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFAAC7FF),
    surfaceTint = Color(0xFF1B3A5C)
)

private val ClassicDark = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF00458E),
    onPrimaryContainer = Color(0xFFD4E4FF),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF690005),
    secondaryContainer = Color(0xFF93000A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF4DD9DE),
    onTertiary = Color(0xFF003739),
    tertiaryContainer = Color(0xFF004F52),
    onTertiaryContainer = Color(0xFFA6F1F4),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1E1E20),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF1A1C1E),
    inversePrimary = Color(0xFF1B3A5C)
)

// ── Esquema Naturaleza (Refrescante) ──
private val NatureLight = lightColorScheme(
    primary = Color(0xFF2E6B3E),           // Verde bosque
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC5F2C6),
    onPrimaryContainer = Color(0xFF002200),
    secondary = Color(0xFF8B5A2B),         // Marrón tierra
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC1),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF3D7A6B),          // Verde jade
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB8F5E8),
    onTertiaryContainer = Color(0xFF002117),
    background = Color(0xFFF6FBF2),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFE0E8DB),
    onSurfaceVariant = Color(0xFF44483E),
    outline = Color(0xFF74796E),
    outlineVariant = Color(0xFFC4C9BC),
    inverseSurface = Color(0xFF2F3129),
    inverseOnSurface = Color(0xFFF1F1E8),
    inversePrimary = Color(0xFF81C784)
)

private val NatureDark = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003900),
    primaryContainer = Color(0xFF005300),
    onPrimaryContainer = Color(0xFFC5F2C6),
    secondary = Color(0xFFFFCC80),
    onSecondary = Color(0xFF3E2700),
    secondaryContainer = Color(0xFF593D00),
    onSecondaryContainer = Color(0xFFFFDBC1),
    tertiary = Color(0xFF8CD9CA),
    onTertiary = Color(0xFF00382E),
    tertiaryContainer = Color(0xFF005143),
    onTertiaryContainer = Color(0xFFB8F5E8),
    background = Color(0xFF1A1C18),
    onBackground = Color(0xFFE2E3DC),
    surface = Color(0xFF1E1F1B),
    onSurface = Color(0xFFE2E3DC),
    surfaceVariant = Color(0xFF44483E),
    onSurfaceVariant = Color(0xFFC4C9BC),
    outline = Color(0xFF8E9386),
    outlineVariant = Color(0xFF44483E),
    inverseSurface = Color(0xFFE2E3DC),
    inverseOnSurface = Color(0xFF1A1C18),
    inversePrimary = Color(0xFF2E6B3E)
)

// ── Esquema Océano (Profundo y calmado) ──
private val OceanLight = lightColorScheme(
    primary = Color(0xFF005B82),           // Azul océano
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDE8FF),
    onPrimaryContainer = Color(0xFF001B2E),
    secondary = Color(0xFF006D73),         // Turquesa
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF8CF3F8),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF5B4B8A),          // Púrpura suave
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF160044),
    background = Color(0xFFF5FAFE),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDEE5F0),
    onSurfaceVariant = Color(0xFF434750),
    outline = Color(0xFF737781),
    outlineVariant = Color(0xFFC3C6D0),
    inverseSurface = Color(0xFF2E3136),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFF4FC3F7)
)

private val OceanDark = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003546),
    primaryContainer = Color(0xFF004D67),
    onPrimaryContainer = Color(0xFFBDE8FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF003F47),
    secondaryContainer = Color(0xFF005B65),
    onSecondaryContainer = Color(0xFF8CF3F8),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    tertiaryContainer = Color(0xFF4F378A),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF191C20),
    onBackground = Color(0xFFE2E2E8),
    surface = Color(0xFF1E1F23),
    onSurface = Color(0xFFE2E2E8),
    surfaceVariant = Color(0xFF434750),
    onSurfaceVariant = Color(0xFFC3C6D0),
    outline = Color(0xFF8D919A),
    outlineVariant = Color(0xFF434750),
    inverseSurface = Color(0xFFE2E2E8),
    inverseOnSurface = Color(0xFF191C20),
    inversePrimary = Color(0xFF005B82)
)

// ── Esquema Lavanda (Elegante y calmante) ──
private val LavenderLight = lightColorScheme(
    primary = Color(0xFF6B3A8E),           // Púrpura lavanda
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0DDFF),
    onPrimaryContainer = Color(0xFF250045),
    secondary = Color(0xFF9C4586),         // Magenta suave
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD7EF),
    onSecondaryContainer = Color(0xFF3C0033),
    tertiary = Color(0xFF825500),          // Marrón dorado
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2B1800),
    background = Color(0xFFFEFBFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE9E0EE),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFCE93D8)
)

private val LavenderDark = darkColorScheme(
    primary = Color(0xFFCE93D8),
    onPrimary = Color(0xFF4A0062),
    primaryContainer = Color(0xFF67008C),
    onPrimaryContainer = Color(0xFFF0DDFF),
    secondary = Color(0xFFFFA6D5),
    onSecondary = Color(0xFF54004D),
    secondaryContainer = Color(0xFF74006A),
    onSecondaryContainer = Color(0xFFFFD7EF),
    tertiary = Color(0xFFFFB85C),
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF201F23),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF6B3A8E)
)

// ── Esquema Atardecer (Cálido y energético) ──
private val SunsetLight = lightColorScheme(
    primary = Color(0xFFB34400),           // Naranja quemado
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBBF),
    onPrimaryContainer = Color(0xFF371200),
    secondary = Color(0xFFC0425A),         // Rojo coral
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF400012),
    tertiary = Color(0xFF7F4F00),          // Mostaza
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF281600),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF201B16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201B16),
    surfaceVariant = Color(0xFFF4DFD0),
    onSurfaceVariant = Color(0xFF524536),
    outline = Color(0xFF847465),
    outlineVariant = Color(0xFFD7C3B2),
    inverseSurface = Color(0xFF362F2B),
    inverseOnSurface = Color(0xFFFBEEE2),
    inversePrimary = Color(0xFFFFB68C)
)

private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB68C),
    onPrimary = Color(0xFF561E00),
    primaryContainer = Color(0xFF7A2E00),
    onPrimaryContainer = Color(0xFFFFDBBF),
    secondary = Color(0xFFFFB2BE),
    onSecondary = Color(0xFF650021),
    secondaryContainer = Color(0xFF8D0031),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF442800),
    tertiaryContainer = Color(0xFF613D00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF201B16),
    onBackground = Color(0xFFEDE0D6),
    surface = Color(0xFF26201A),
    onSurface = Color(0xFFEDE0D6),
    surfaceVariant = Color(0xFF524536),
    onSurfaceVariant = Color(0xFFD7C3B2),
    outline = Color(0xFFA08D7E),
    outlineVariant = Color(0xFF524536),
    inverseSurface = Color(0xFFEDE0D6),
    inverseOnSurface = Color(0xFF201B16),
    inversePrimary = Color(0xFFB34400)
)

// ── Esquema Carbón (Minimalista y elegante) ──
private val CarbonLight = lightColorScheme(
    primary = Color(0xFF2E3238),           // Gris antracita
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5D9E0),
    onPrimaryContainer = Color(0xFF0E1218),
    secondary = Color(0xFF505660),         // Gris medio
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4D9E5),
    onSecondaryContainer = Color(0xFF0E1218),
    tertiary = Color(0xFF5B5E66),          // Gris azulado
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFE2EB),
    onTertiaryContainer = Color(0xFF181A20),
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E2E8),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFB0B7C3)
)

private val CarbonDark = darkColorScheme(
    primary = Color(0xFFB0B7C3),
    onPrimary = Color(0xFF1A1D23),
    primaryContainer = Color(0xFF2C3139),
    onPrimaryContainer = Color(0xFFD5D9E0),
    secondary = Color(0xFFB8BDC8),
    onSecondary = Color(0xFF23272F),
    secondaryContainer = Color(0xFF393D46),
    onSecondaryContainer = Color(0xFFD4D9E5),
    tertiary = Color(0xFFC3C7D0),
    onTertiary = Color(0xFF2D3038),
    tertiaryContainer = Color(0xFF44474F),
    onTertiaryContainer = Color(0xFFDFE2EB),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1E2023),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF1A1C1E),
    inversePrimary = Color(0xFF2E3238)
)

@Composable
fun MoodMateTheme(
    darkTheme: Boolean = ThemeManager.isDark,
    scheme: ThemeScheme = ThemeManager.currentScheme,
    content: @Composable () -> Unit
) {
    val colorScheme = when (scheme) {
        ThemeScheme.CLASSIC -> if (darkTheme) ClassicDark else ClassicLight
        ThemeScheme.NATURE -> if (darkTheme) NatureDark else NatureLight
        ThemeScheme.OCEAN -> if (darkTheme) OceanDark else OceanLight
        ThemeScheme.LAVENDER -> if (darkTheme) LavenderDark else LavenderLight
        ThemeScheme.SUNSET -> if (darkTheme) SunsetDark else SunsetLight
        ThemeScheme.CARBON -> if (darkTheme) CarbonDark else CarbonLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}