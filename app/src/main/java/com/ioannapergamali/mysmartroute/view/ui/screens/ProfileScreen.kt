package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.util.Log
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


import com.google.firebase.storage.FirebaseStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage

@Composable
fun ProfileScreen(navController: NavController, openDrawer: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val username = remember { mutableStateOf<String?>(null) }
    val photoUrl = remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val uid = user?.uid
        Log.d("ProfileScreen", "Ελήφθη uri=$uri για uid=$uid")
        if (uid == null || uri == null) {
            Log.w("ProfileScreen", "uid ή uri κενό - διακοπή διαδικασίας")
            return@rememberLauncherForActivityResult
        }
        val ref = FirebaseStorage.getInstance()
            .reference.child("profile_photos/$uid.jpg")
        Log.d("ProfileScreen", "Ξεκινάει ανέβασμα εικόνας για $uid")
        ref.putFile(uri)
            .addOnSuccessListener {
                Log.d("ProfileScreen", "Το ανέβασμα ολοκληρώθηκε, ανακτώ URL")
                ref.downloadUrl

                    .addOnSuccessListener { downloadUri ->
                        val url = downloadUri.toString()
                        photoUrl.value = url
                        Log.d("ProfileScreen", "Λήφθηκε URL: $url")
                        val docRef = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                        Log.d("ProfileScreen", "Αποθήκευση photoUrl στο Firestore για $uid")
                        docRef.update("photoUrl", url)
                            .addOnSuccessListener {
                                Log.d("ProfileScreen", "photoUrl αποθηκεύτηκε επιτυχώς")
                                docRef.get().addOnSuccessListener { refreshed ->
                                    Log.d(
                                        "ProfileScreen",
                                        "photoUrl μετά την αποθήκευση: ${refreshed.getString("photoUrl")}"
                                    )
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileScreen", "Αποτυχία αποθήκευσης photoUrl", e)
                            }

            }.addOnSuccessListener { downloadUri ->
                val url = downloadUri.toString()
                photoUrl.value = url
                val docRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)

                Log.d("ProfileScreen", "photoUrl προς αποθήκευση: $url")

                docRef.set(mapOf("photoUrl" to url), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("ProfileScreen", "photoUrl αποθηκεύτηκε επιτυχώς")
                        docRef.get().addOnSuccessListener { refreshed ->
                            Log.d(
                                "ProfileScreen",
                                "photoUrl μετά την αποθήκευση: ${refreshed.getString("photoUrl")}"
                            )
                        }

                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileScreen", "Αποτυχία λήψης URL", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileScreen", "Αποτυχία ανεβάσματος εικόνας", e)
            }
    }

    LaunchedEffect(user) {
        val uid = user?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    username.value = doc.getString("username")
                    photoUrl.value = doc.getString("photoUrl")
                    Log.d("ProfileScreen", "photoUrl από τη βάση: ${photoUrl.value}")
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
                Text(text = "Email: ${user?.email ?: ""}")
                username.value?.let { Text(text = "Username: $it") }
            }
        }
    }
}
