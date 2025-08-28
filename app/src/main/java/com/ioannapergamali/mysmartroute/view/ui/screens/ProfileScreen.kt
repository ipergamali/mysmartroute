package com.ioannapergamali.mysmartroute.view.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

/**
 * Εμφανίζει τα στοιχεία του χρήστη και επιτρέπει την αλλαγή της φωτογραφίας προφίλ.
 * Η φωτογραφία ανεβαίνει στο Firebase Storage και το URL αποθηκεύεται στο Firestore.
 */
@Composable
fun ProfileScreen(navController: NavController, openDrawer: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val username = remember { mutableStateOf<String?>(null) }
    val photoUrl = remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val uid = user?.uid ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
filescreen-ppycbc
        Log.d("ProfileScreen", "Επιλέχθηκε εικόνα: $uri")

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profileImages/$uid.jpg")

        Log.d("ProfileScreen", "Διαδρομή Storage: ${'$'}{storageRef.path}")


        val storageRef = FirebaseStorage.getInstance().reference
            .child("profileImages/$uid.jpg")


        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    photoUrl.value = url

                    Log.d("ProfileScreen", "Λήφθηκε download URL: $url")


                    val docRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)

                    docRef.update("photoUrl", url)

                        .addOnSuccessListener {
                            Log.d("ProfileScreen", "Αποθηκεύτηκε το URL στο Firestore")
                        }
                        .addOnFailureListener { error ->
                            Log.e("ProfileScreen", "Αποτυχία ενημέρωσης Firestore", error)
                            docRef.set(mapOf("photoUrl" to url))
                        }

                        .addOnFailureListener { docRef.set(mapOf("photoUrl" to url)) }

                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileScreen", "Αποτυχία ανεβάσματος εικόνας", e)
            }
    }

    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    username.value = doc.getString("username")
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

                Text(text = "Email: ${user?.email ?: ""}")
                username.value?.let { Text(text = "Username: $it") }
            }
        }
    }
}

