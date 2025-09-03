package com.ioannapergamali.mysmartroute.view.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
    val name = remember { mutableStateOf("") }
    val surname = remember { mutableStateOf("") }
    val phone = remember { mutableStateOf("") }
    val streetName = remember { mutableStateOf("") }
    val streetNum = remember { mutableStateOf("") }
    val postalCode = remember { mutableStateOf("") }
    val username = remember { mutableStateOf("") }
    val photoUrl = remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val uid = user?.uid ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profileImages/$uid.jpg")
        Log.d("STORAGE", "Uploading to: ${storageRef.path}")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    photoUrl.value = url

                    val docRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)

                    docRef.update("photoUrl", url)
                        .addOnFailureListener {
                            docRef.set(mapOf("photoUrl" to url), SetOptions.merge())
                        }
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
                        .addOnFailureListener {
                            docRef.set(mapOf("photoUrl" to url), SetOptions.merge())
                        }
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
                    name.value = doc.getString("name") ?: ""
                    surname.value = doc.getString("surname") ?: ""
                    phone.value = doc.getString("phoneNum") ?: ""
                    streetName.value = doc.getString("streetName") ?: ""
                    streetNum.value = doc.getLong("streetNum")?.toString() ?: ""
                    postalCode.value = doc.getLong("postalCode")?.toString() ?: ""
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
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl.value != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUrl.value)
                                .crossfade(true)
                                .listener(
                                    onStart = {
                                        Log.d("ProfileScreen", "Ξεκίνησε η φόρτωση εικόνας")
                                    },
                                    onSuccess = { _, _ ->
                                        Log.d("ProfileScreen", "Η εικόνα φορτώθηκε επιτυχώς")
                                    },
                                    onError = { _, errorResult ->
                                        Log.e(
                                            "ProfileScreen",
                                            "Αποτυχία φόρτωσης εικόνας",
                                            errorResult.throwable
                                        )
                                    }
                                )
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Filled.Photo,
                            contentDescription = stringResource(R.string.upload_photo)
                        )
                    }
                    IconButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = stringResource(R.string.take_photo)
                        )
                    }
                }

                Text(text = "Email: ${user?.email ?: ""}")

                OutlinedTextField(
                    value = name.value,
                    onValueChange = { name.value = it },
                    label = { Text(stringResource(R.string.first_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = surname.value,
                    onValueChange = { surname.value = it },
                    label = { Text(stringResource(R.string.last_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone.value,
                    onValueChange = { phone.value = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = streetName.value,
                    onValueChange = { streetName.value = it },
                    label = { Text(stringResource(R.string.street_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = streetNum.value,
                    onValueChange = { streetNum.value = it },
                    label = { Text(stringResource(R.string.street_number)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = postalCode.value,
                    onValueChange = { postalCode.value = it },
                    label = { Text(stringResource(R.string.postal_code)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username.value,
                    onValueChange = { username.value = it },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = {
                    val uid = user?.uid ?: return@Button
                    val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                    val streetNumInt = streetNum.value.toIntOrNull()
                    val postalCodeInt = postalCode.value.toIntOrNull()
                    if (streetNumInt == null || postalCodeInt == null) return@Button
                    val updates = mapOf(
                        "name" to name.value,
                        "surname" to surname.value,
                        "phoneNum" to phone.value,
                        "streetName" to streetName.value,
                        "streetNum" to streetNumInt,
                        "postalCode" to postalCodeInt,
                        "username" to username.value
                    )
                    docRef.update(updates).addOnFailureListener {
                        docRef.set(updates, SetOptions.merge())
                    }
                }) {
                    Text(text = stringResource(id = R.string.save))
                }
            }
        }
    }
}

