package com.ioannapergamali.mysmartroute.model.enumerations

import java.util.Locale

enum class AppLanguage(val label: String, val locale: Locale) {
    ENGLISH("English", Locale.ENGLISH),
    GREEK("Ελληνικά", Locale("el"))
}
