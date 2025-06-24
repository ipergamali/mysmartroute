package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(navController: NavController, openDrawer: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val username = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        val uid = user?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    username.value = doc.getString("username")
                }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Profile",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Email: ${user?.email ?: ""}")
                username.value?.let { Text(text = "Username: $it") }
            }
        }
    }
}
