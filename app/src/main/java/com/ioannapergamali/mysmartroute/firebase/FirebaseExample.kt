package com.ioannapergamali.mysmartroute.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Παράδειγμα απλής χρήσης των Firebase Auth, Firestore και Storage.
 */
object FirebaseExample {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    fun signIn(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password)

    fun fetchUsers() = db.collection("users").get()

    fun getFileRef(path: String) = storage.reference.child(path)
}
