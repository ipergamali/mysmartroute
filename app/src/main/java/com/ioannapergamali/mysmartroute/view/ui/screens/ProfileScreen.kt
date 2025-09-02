package com.ioannapergamali.mysmartroute.view.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import java.io.ByteArrayOutputStream

/**
 * Εμφανίζει τα στοιχεία του χρήστη και επιτρέπει την αλλαγή της φωτογραφίας προφίλ.
 * Η φωτογραφία ανεβαίνει στο Firebase Storage και το URL αποθηκεύεται στο Firestore.
 */
@Composable
fun ProfileScreen(navController: NavController, openDrawer: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val username = remember { mutableStateOf("") }
    val photoUrl = remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val uid = user?.uid ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profileImages/$uid.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    photoUrl.value = url

                    val docRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)

                    docRef.update("photoUrl", url)
                        .addOnFailureListener { docRef.set(mapOf("photoUrl" to url)) }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileScreen", "Αποτυχία ανεβάσματος εικόνας", e)
            }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val uid = user?.uid ?: return@rememberLauncherForActivityResult
        bitmap ?: return@rememberLauncherForActivityResult

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profileImages/$uid.jpg")

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    photoUrl.value = url

                    val docRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)

                    docRef.update("photoUrl", url)
                        .addOnFailureListener { docRef.set(mapOf("photoUrl" to url)) }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileScreen", "Αποτυχία ανεβάσματος εικόνας", e)
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Log.d("ProfileScreen", "Δεν δόθηκε άδεια κάμερας")
        }
    }

    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    username.value = doc.getString("username") ?: ""
                    photoUrl.value = doc.getString("photoUrl")


                    Log.d(
                        "ProfileScreen",
                        "Φορτώθηκε χρήστης: ${'$'}{username.value}, photoUrl: ${'$'}{photoUrl.value}"
                    )

                }
                .addOnFailureListener { e ->
                    Log.e("ProfileScreen", "Αποτυχία φόρτωσης photoUrl", e)
                }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.profile),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                photoUrl.value?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )
                }

                Button(onClick = { imagePicker.launch("image/*") }) {
                    Text(text = stringResource(id = R.string.upload_photo))
                }

                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(text = stringResource(id = R.string.take_photo))
                }

                Text(text = "Email: ${user?.email ?: ""}")

                OutlinedTextField(
                    value = username.value,
                    onValueChange = { username.value = it },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = {
                    val uid = user?.uid ?: return@Button
                    val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                    docRef.update("username", username.value)
                        .addOnFailureListener { docRef.set(mapOf("username" to username.value)) }
                }) {
                    Text(text = stringResource(id = R.string.save))
                }
            }
        }
    }
}

