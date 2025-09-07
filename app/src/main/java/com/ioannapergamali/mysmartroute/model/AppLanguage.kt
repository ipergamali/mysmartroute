package com.ioannapergamali.mysmartroute.model

/**
 * Ελληνικά: Γλώσσες που υποστηρίζει η εφαρμογή με κωδικό και σημαία.
 * English: Languages supported by the app with code and flag.
 */
enum class AppLanguage(val code: String, val label: String, val flag: String) {
    Greek("el", "Ελληνικά", "\uD83C\uDDEC\uD83C\uDDF7"),
    English("en", "English", "\uD83C\uDDEC\uD83C\uDDE7")
}
