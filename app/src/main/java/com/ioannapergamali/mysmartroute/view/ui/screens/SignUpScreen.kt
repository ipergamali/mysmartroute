package com.ioannapergamali.mysmartroute.view.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import android.widget.Toast
import com.ioannapergamali.mysmartroute.view.ui.components.TopBar
import com.ioannapergamali.mysmartroute.model.enumerations.UserRole
import com.ioannapergamali.mysmartroute.viewmodel.AuthenticationViewModel
import com.ioannapergamali.mysmartroute.view.ui.components.ScreenContainer

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
        val scrollState = rememberScrollState()

        ScreenContainer(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = surname, onValueChange = { surname = it }, label = { Text("Surname") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = phoneNum, onValueChange = { phoneNum = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(Modifier.height(8.dp))
            TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())

            Spacer(Modifier.height(16.dp))
            Text("Address", style = MaterialTheme.typography.titleMedium)
            TextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = streetName, onValueChange = { streetName = it }, label = { Text("Street Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            TextField(value = streetNumInput, onValueChange = { streetNumInput = it }, label = { Text("Street Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(8.dp))
            TextField(value = postalCodeInput, onValueChange = { postalCodeInput = it }, label = { Text("Postal Code") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            Spacer(Modifier.height(16.dp))
            Text("Select Role", style = MaterialTheme.typography.titleMedium)
            Row {
                RadioButton(selected = selectedRole == UserRole.DRIVER, onClick = { selectedRole = UserRole.DRIVER })
                Text("Driver", modifier = Modifier.padding(end = 16.dp))
                RadioButton(selected = selectedRole == UserRole.PASSENGER, onClick = { selectedRole = UserRole.PASSENGER })
                Text("Passenger")
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
                        context,
                        name, surname, username, email, phoneNum, password,
                        com.ioannapergamali.mysmartroute.model.classes.users.UserAddress(city, streetName, streetNum, postalCode),
                        selectedRole
                    )
                }
            }) {
                Text("Sign Up")
            }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthenticationViewModel.SignUpState.Success -> {
                Toast.makeText(
                    context,
                    "User created and stored successfully",
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
