package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.widget.Toast
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.R
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer
import com.ioannapergamali.mysmartroute.view.ui.components.KeyboardBubble
import com.ioannapergamali.mysmartroute.view.ui.util.bringIntoViewOnFocus
import com.ioannapergamali.mysmartroute.view.ui.util.observeBubble
import com.ioannapergamali.mysmartroute.view.ui.util.rememberKeyboardBubbleState

@Composable
fun SignUpScreen(
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

    var selectedRole by remember { mutableStateOf(UserRole.PASSENGER) }

    val bubbleState = rememberKeyboardBubbleState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0



    Scaffold(
        topBar = {
            TopBar(
                title = "Sign Up",
                navController = navController,
                showMenu = true,
                onMenuClick = openDrawer
            )
        }
    ) { paddingValues ->
        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),

                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (bubbleState.activeFieldId == 0) bubbleState.text = it
                    },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        surname = it
                        if (bubbleState.activeFieldId == 1) bubbleState.text = it
                    },
                    label = { Text("Surname") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        username = it
                        if (bubbleState.activeFieldId == 2) bubbleState.text = it
                    },
                    label = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        email = it
                        if (bubbleState.activeFieldId == 3) bubbleState.text = it
                    },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        phoneNum = it
                        if (bubbleState.activeFieldId == 4) bubbleState.text = it
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        password = it
                        if (bubbleState.activeFieldId == 5) bubbleState.text = it
                    },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
                        .observeBubble(bubbleState, 5) { password },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(16.dp))
                Text("Address", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        if (bubbleState.activeFieldId == 6) bubbleState.text = it
                    },
                    label = { Text("City") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        streetName = it
                        if (bubbleState.activeFieldId == 7) bubbleState.text = it
                    },
                    label = { Text("Street Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        streetNumInput = it
                        if (bubbleState.activeFieldId == 8) bubbleState.text = it
                    },
                    label = { Text("Street Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
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
                    onValueChange = {
                        postalCodeInput = it
                        if (bubbleState.activeFieldId == 9) bubbleState.text = it
                    },
                    label = { Text("Postal Code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
                        .observeBubble(bubbleState, 9) { postalCodeInput },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.select_role), style = MaterialTheme.typography.titleMedium)
                Row {
                    RadioButton(
                        selected = selectedRole == UserRole.DRIVER,
                        onClick = { selectedRole = UserRole.DRIVER })
                    Text(stringResource(R.string.role_driver), modifier = Modifier.padding(end = 16.dp))
                    RadioButton(
                        selected = selectedRole == UserRole.PASSENGER,
                        onClick = { selectedRole = UserRole.PASSENGER })
                    Text(stringResource(R.string.role_passenger))
                }

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
                            selectedRole
                        )
                    } else {
                        Toast.makeText(
                            context,
                            "Συμπλήρωσε σωστά τον αριθμό οδού και τον ταχυδρομικό κώδικα",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Text("Sign Up")
                }

                }

                KeyboardBubble(
                    text = bubbleState.text,
                    visible = imeVisible && bubbleState.activeFieldId != null
                )
            }
        }

        LaunchedEffect(uiState) {
            when (uiState) {
                is AuthenticationViewModel.SignUpState.Success -> {
                    Toast.makeText(
                        context,
                        "Η εγγραφή ολοκληρώθηκε με επιτυχία",
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
