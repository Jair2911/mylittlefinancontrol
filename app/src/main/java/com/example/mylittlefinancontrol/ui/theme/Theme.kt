package com.example.mylittlefinancontrol.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

@Immutable
data class DabaExtendedColors(
    val surfaceBg: Color,
    val surfaceCard: Color,
    val surfaceContainer: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val borderSubtle: Color,
    val borderInternal: Color,
    val primaryEmerald: Color,
    val primaryEmeraldDark: Color,
    val primaryEmeraldLight: Color,
    val primaryEmerald50: Color,
    val ghostBg: Color,
    val ghostBorder: Color,
    val activeStateDot: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textVariant: Color,
    val textMuted: Color,
    val textMeta: Color,
    val incomeGreen: Color,
    val incomeGreenBg: Color,
    val expenseRed: Color,
    val expenseRedBg: Color,
    val debtAmber: Color,
    val debtAmberBg: Color,
    val badgePaidBg: Color,
    val badgePaidText: Color,
    val badgePendingBg: Color,
    val badgePendingText: Color
)

val LightDabaExtendedColors = DabaExtendedColors(
    surfaceBg = LightSurfaceBg,
    surfaceCard = LightSurfaceCard,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    borderSubtle = LightBorderSubtle,
    borderInternal = LightBorderInternal,
    primaryEmerald = LightPrimaryEmerald,
    primaryEmeraldDark = LightPrimaryEmeraldDark,
    primaryEmeraldLight = LightPrimaryEmeraldLight,
    primaryEmerald50 = LightPrimaryEmerald50,
    ghostBg = LightGhostBg,
    ghostBorder = LightGhostBorder,
    activeStateDot = LightActiveStateDot,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textVariant = LightTextVariant,
    textMuted = LightTextMuted,
    textMeta = LightTextMeta,
    incomeGreen = LightIncomeGreen,
    incomeGreenBg = LightIncomeGreenBg,
    expenseRed = LightExpenseRed,
    expenseRedBg = LightExpenseRedBg,
    debtAmber = LightDebtAmber,
    debtAmberBg = LightDebtAmberBg,
    badgePaidBg = LightBadgePaidBg,
    badgePaidText = LightBadgePaidText,
    badgePendingBg = LightBadgePendingBg,
    badgePendingText = LightBadgePendingText
)

val DarkDabaExtendedColors = DabaExtendedColors(
    surfaceBg = DarkSurfaceBg,
    surfaceCard = DarkSurfaceCard,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    borderSubtle = DarkBorderSubtle,
    borderInternal = DarkBorderInternal,
    primaryEmerald = DarkPrimaryEmerald,
    primaryEmeraldDark = DarkPrimaryEmeraldDark,
    primaryEmeraldLight = DarkPrimaryEmeraldLight,
    primaryEmerald50 = DarkPrimaryEmerald50,
    ghostBg = DarkGhostBg,
    ghostBorder = DarkGhostBorder,
    activeStateDot = DarkActiveStateDot,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textVariant = DarkTextVariant,
    textMuted = DarkTextMuted,
    textMeta = DarkTextMeta,
    incomeGreen = DarkIncomeGreen,
    incomeGreenBg = DarkIncomeGreenBg,
    expenseRed = DarkExpenseRed,
    expenseRedBg = DarkExpenseRedBg,
    debtAmber = DarkDebtAmber,
    debtAmberBg = DarkDebtAmberBg,
    badgePaidBg = DarkBadgePaidBg,
    badgePaidText = DarkBadgePaidText,
    badgePendingBg = DarkBadgePendingBg,
    badgePendingText = DarkBadgePendingText
)

val LocalDabaColors = staticCompositionLocalOf { LightDabaExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryEmerald,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceContainerLow,
    onPrimaryContainer = LightPrimaryEmerald,
    secondary = LightTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightIncomeGreenBg,
    onTertiary = LightBadgePaidText,
    background = LightSurfaceBg,
    onBackground = LightTextPrimary,
    surface = LightSurfaceCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightBorderSubtle,
    outlineVariant = LightOutlineVariant,
    error = LightExpenseRed,
    onError = Color.White,
    errorContainer = LightExpenseRedBg,
    onErrorContainer = LightOnExpenseRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryEmerald,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurfaceContainer,
    onPrimaryContainer = DarkPrimaryEmerald,
    secondary = DarkTextSecondary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkIncomeGreenBg,
    onTertiary = DarkBadgePaidText,
    background = DarkSurfaceBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkBorderSubtle,
    outlineVariant = DarkOutlineVariant,
    error = DarkExpenseRed,
    onError = DarkOnPrimary,
    errorContainer = DarkExpenseRedBg,
    onErrorContainer = DarkOnExpenseRed
)

object DabaTheme {
    val colors: DabaExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDabaColors.current
}

@Composable
fun MyLittleFinanControlTheme(
    darkTheme: Boolean = rememberIsDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkDabaExtendedColors else LightDabaExtendedColors

    CompositionLocalProvider(LocalDabaColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = DabaShapes,
            typography = DabaTypography,
            content = content
        )
    }
}

@Composable
private fun rememberIsDarkTheme(): Boolean {
    if (LocalInspectionMode.current) {
        return isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val sp = androidx.compose.runtime.remember(context) {
        context.getSharedPreferences("TemaPrefs", Context.MODE_PRIVATE)
    }
    val isDarkState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            if (sp.contains("EsTemaOscuro")) sp.getBoolean("EsTemaOscuro", false) else systemDark
        )
    }
    androidx.compose.runtime.DisposableEffect(sp, systemDark) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "EsTemaOscuro") {
                isDarkState.value = if (prefs.contains("EsTemaOscuro")) {
                    prefs.getBoolean("EsTemaOscuro", false)
                } else {
                    systemDark
                }
            }
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return isDarkState.value
}
