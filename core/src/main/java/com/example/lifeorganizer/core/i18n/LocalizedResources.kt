package com.example.lifeorganizer.core.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

/**
 * Makes Android string resources (used e.g. by Dokki and Material date pickers) follow the
 * in-app language instead of the system language. The activity is wrapped rather than replaced,
 * so startActivity and friends keep working.
 */
@Composable
fun ProvideAppLocale(language: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    val localized = remember(language, baseContext, baseConfiguration) {
        val locale = Locale.forLanguageTag(language)
        val configuration = Configuration(baseConfiguration).apply { setLocale(locale) }
        val resources = baseContext.createConfigurationContext(configuration).resources
        Triple(LocaleContext(baseContext, resources), configuration, resources)
    }

    CompositionLocalProvider(
        LocalContext provides localized.first,
        LocalConfiguration provides localized.second,
        LocalResources provides localized.third,
        LocalAppLanguage provides language,
        content = content
    )
}

private class LocaleContext(base: Context, private val localizedResources: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}
