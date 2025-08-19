package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble
import com.ioannapergamali.mysmartroute.view.ui.util.LocalKeyboardBubbleState

@Composable
fun AdminSignUpScreen(
    navController: NavController,
    onSignUpSuccess: () -> Unit,
    onSignUpFailure: (String) -> Unit = {},
    openDrawer: () -> Unit
) {
    val viewModel: AuthenticationViewModel = viewModel()
    val uiState by viewModel.signUpState.collectAsState()
    val context = LocalContext.current
    val activity = LocalContext.current as Activity


    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNum by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var city by remember { mutableStateOf("") }
    var streetName by remember { mutableStateOf("") }
    var streetNumInput by remember { mutableStateOf("") }
    var postalCodeInput by remember { mutableStateOf("") }




    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.admin_sign_up),
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            val bubbleState = LocalKeyboardBubbleState.current!!
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.first_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 0) { name },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text(stringResource(R.string.last_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 1) { surname },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 2) { username },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 3) { email },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNum,
                    onValueChange = { phoneNum = it },
                    label = { Text(stringResource(R.string.phone_number)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 4) { phoneNum },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 5) { password },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.address), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.city)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 6) { city },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = streetName,
                    onValueChange = { streetName = it },
                    label = { Text(stringResource(R.string.street_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 7) { streetName },
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = streetNumInput,
                    onValueChange = { streetNumInput = it },
                    label = { Text(stringResource(R.string.street_number)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 8) { streetNumInput },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = postalCodeInput,
                    onValueChange = { postalCodeInput = it },
                    label = { Text(stringResource(R.string.postal_code)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .observeBubble(bubbleState, 9) { postalCodeInput },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (uiState is AuthenticationViewModel.SignUpState.Error) {
                    val message = (uiState as AuthenticationViewModel.SignUpState.Error).message
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val streetNum = streetNumInput.toIntOrNull()
                    val postalCode = postalCodeInput.toIntOrNull()

                    if (streetNum != null && postalCode != null) {
                        viewModel.signUp(
                            activity,
                            context,
                            name, surname, username, email, phoneNum, password,
                            com.ioannapergamali.mysmartroute.model.classes.users.UserAddress(
                                city,
                                streetName,
                                streetNum,
                                postalCode
                            ),
                            UserRole.ADMIN
                        )
                    }
                }) {
                    Text(stringResource(R.string.sign_up))
                }

            }
        }

        LaunchedEffect(uiState) {
            when (uiState) {
                is AuthenticationViewModel.SignUpState.Success -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.sign_up_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    onSignUpSuccess()
                }

                is AuthenticationViewModel.SignUpState.Error -> {
                    val message = (uiState as AuthenticationViewModel.SignUpState.Error).message
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    onSignUpFailure(message)
                }

                else -> {}
            }
        }
        }
}
