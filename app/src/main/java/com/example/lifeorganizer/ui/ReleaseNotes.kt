package com.example.lifeorganizer.ui

/** GitHub release notes from [CHANGELOG], in every supported language, so the in-app notes and GitHub never differ. */
object ReleaseNotes {
    private val sections = listOf(
        "de" to "Deutsch", "en" to "English", "tr" to "Türkçe", "es" to "Español", "zh" to "简体中文"
    )
    private val apkHint = mapOf(
        "de" to "**Welche Datei?** Fast alle Handys: `LifeHub-%s-arm64-v8a.apk`. Sehr alte Handys: `armeabi-v7a`. `x86_64` nur für Emulatoren.",
        "en" to "**Which file?** Almost all phones: `LifeHub-%s-arm64-v8a.apk`. Very old phones: `armeabi-v7a`. `x86_64` is only for emulators.",
        "tr" to "**Hangi dosya?** Neredeyse tüm telefonlar: `LifeHub-%s-arm64-v8a.apk`. Çok eski telefonlar: `armeabi-v7a`. `x86_64` yalnızca emülatörler için.",
        "es" to "**¿Qué archivo?** Casi todos los móviles: `LifeHub-%s-arm64-v8a.apk`. Móviles muy antiguos: `armeabi-v7a`. `x86_64` solo para emuladores.",
        "zh" to "**下载哪个文件？** 几乎所有手机：`LifeHub-%s-arm64-v8a.apk`。很旧的手机：`armeabi-v7a`。`x86_64` 仅用于模拟器。"
    )

    /** [splitApks]: the release has one APK per CPU type (from 0.10 on). */
    fun markdown(version: String, splitApks: Boolean = true): String {
        val release = CHANGELOG.firstOrNull { it.version == version } ?: error("No changelog entry for $version")
        return buildString {
            append("## LifeHub ").append(version).append("\n")
            sections.forEach { (lang, name) ->
                append("\n### ").append(name).append("\n\n")
                release.items.forEach { append("- ").append(it.of(lang)).append("\n") }
                if (splitApks) append("\n").append(apkHint.getValue(lang).format(version)).append("\n")
            }
        }
    }
}
