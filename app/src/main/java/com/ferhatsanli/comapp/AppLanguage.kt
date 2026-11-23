package com.ferhatsanli.comapp

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * The app's language. [SYSTEM] follows the phone (Turkish or Dutch when the phone uses one of
 * them, English otherwise); the others pin it. Android keeps the choice itself (per-app language,
 * Android 13+) and recreates the screen in the new language, so the same choice also shows under
 * the phone's own Settings > Apps > ComApp > Language.
 */
internal enum class AppLanguage(val tag: String?, val nativeName: String) {
    SYSTEM(null, ""),
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe"),
    DUTCH("nl", "Nederlands"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag != null && it.tag == tag?.substringBefore('-') } ?: SYSTEM

        fun current(context: Context): AppLanguage {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            return if (locales == null || locales.isEmpty) SYSTEM else fromTag(locales[0].language)
        }

        fun apply(context: Context, language: AppLanguage) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            manager.applicationLocales = language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        }
    }
}
