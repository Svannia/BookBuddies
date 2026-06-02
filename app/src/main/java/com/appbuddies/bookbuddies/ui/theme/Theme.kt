package com.appbuddies.bookbuddies.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.appbuddies.bookbuddies.datastore.ThemeChoice

val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = DarkPurple,
    tertiary = LightGrey,
    outline = DarkGrey,
    background = Color.Black,
    inversePrimary = Color.White,
    onBackground = LightGrey
)

val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = LightPurple,
    tertiary = VeryLightGrey,
    outline = LightGrey,
    background = Color.White,
    inversePrimary = Color.Black,
    onBackground = DarkGrey
)

@Composable
fun BookBuddiesTheme(
    themeChoice: ThemeChoice,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeChoice) {
        ThemeChoice.SYSTEM_DEFAULT -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MyTypography,
        content = content
    )
}
