package com.ioannapergamali.mysmartroute.utils

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Απλή συνάρτηση ελέγχου σύνδεσης χρήστη στο Firebase Authentication.
 */
fun checkLogin() {
    val auth = Firebase.auth
    val user = auth.currentUser
    if (user != null) {
        println("Συνδεδεμένος χρήστης: ${'$'}{user.uid}")
    } else {
        println("Κανένας χρήστης δεν είναι συνδεδεμένος")
    }
}
