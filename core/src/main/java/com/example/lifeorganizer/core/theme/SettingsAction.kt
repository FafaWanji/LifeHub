package com.example.lifeorganizer.core.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.lifeorganizer.core.i18n.Str
import com.example.lifeorganizer.core.i18n.text

/** Opens the app settings; the app shell provides it, so every module's top bar can show the button. */
val LocalOpenSettings = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The settings button at the top right of a module; nothing when no shell provides [LocalOpenSettings]. */
@Composable
fun SettingsAction() {
    val open = LocalOpenSettings.current ?: return
    IconButton(onClick = open) { Icon(Icons.Outlined.Settings, Str.settings.text()) }
}
