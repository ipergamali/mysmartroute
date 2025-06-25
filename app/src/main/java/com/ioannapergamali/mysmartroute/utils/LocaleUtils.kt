package com.ioannapergamali.mysmartroute.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleUtils {
    fun updateLocale(context: Context, language: String) {
        val locales = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
