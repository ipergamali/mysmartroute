

package com.ioannapergamali.mysmartroute.view.ui.screens
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.FileProvider
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.data.local.UserEntity
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, openDrawer: () -> Unit) {
    val viewModel: UserViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    val userEntity = remember { mutableStateOf<UserEntity?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imagePreviewUri by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            imageUri = uri
            imagePreviewUri = uri
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
            bmp?.let {
                val uriTmp = saveBitmapToCache(context, it)
                imageUri = uriTmp
                imagePreviewUri = uriTmp
            }
        }

    LaunchedEffect(user) {
        if (user != null) {
            val entity = viewModel.getUser(context, user.uid)
            userEntity.value = entity
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
            if (userEntity.value == null) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // ΕΙΚΟΝΑ ΠΡΟΦΙΛ (με AsyncImage αν υπάρχει photoUrl, αλλιώς placeholder)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userEntity.value?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userEntity.value?.photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(text = "User", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = stringResource(R.string.select_from_gallery)
                            )
                        }
                        IconButton(onClick = { cameraLauncher.launch(null) }) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = stringResource(R.string.take_photo)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    var name by remember { mutableStateOf(userEntity.value?.name ?: "") }
                    var surname by remember { mutableStateOf(userEntity.value?.surname ?: "") }
                    var email by remember { mutableStateOf(userEntity.value?.email ?: "") }
                    var phoneNum by remember { mutableStateOf(userEntity.value?.phoneNum ?: "") }
                    var city by remember { mutableStateOf(userEntity.value?.city ?: "") }
                    var streetName by remember {
                        mutableStateOf(
                            userEntity.value?.streetName ?: ""
                        )
                    }
                    var streetNum by remember { mutableStateOf(userEntity.value?.streetNum.toString()) }
                    var postalCode by remember { mutableStateOf(userEntity.value?.postalCode.toString()) }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.first_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it },
                        label = { Text(stringResource(R.string.last_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = phoneNum,
                        onValueChange = { phoneNum = it },
                        label = { Text(stringResource(R.string.phone_number)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(stringResource(R.string.city)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = streetName,
                        onValueChange = { streetName = it },
                        label = { Text(stringResource(R.string.street_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = streetNum,
                        onValueChange = { streetNum = it },
                        label = { Text(stringResource(R.string.street_number)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text(stringResource(R.string.postal_code)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(Modifier.height(12.dp))
                    if (uploading) CircularProgressIndicator() else Button(onClick = {
                        uploading = true
                        scope.launch {
                            val entity = UserEntity(
                                id = user?.uid ?: "",
                                name = name.trim(),
                                surname = surname.trim(),
                                email = email.trim(),
                                phoneNum = phoneNum.trim(),
                                city = city.trim(),
                                streetName = streetName.trim(),
                                streetNum = streetNum.toIntOrNull() ?: 0,
                                postalCode = postalCode.toIntOrNull() ?: 0
                            )
                            viewModel.updateUser(context, entity, imageUri)
                            uploading = false
                            saveMessage = context.getString(R.string.profile_updated)
                        }
                    }) {
                        Text(stringResource(R.string.save))
                    }
                    if (saveMessage.isNotBlank()) {
                        Text(saveMessage, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    val filename = "profile_${UUID.randomUUID()}.jpg"
    val file = File(context.cacheDir, filename)
    val fos = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
    fos.flush()
    fos.close()
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
