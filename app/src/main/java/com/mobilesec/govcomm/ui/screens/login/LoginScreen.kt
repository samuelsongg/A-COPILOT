package com.mobilesec.govcomm.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mobilesec.govcomm.GovCommApp
import com.mobilesec.govcomm.R
import com.mobilesec.govcomm.repo.FirebaseUtil
import com.mobilesec.govcomm.Screen
import com.mobilesec.govcomm.ui.theme.GovCommTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf("")
) {
    // Initializes the ViewModel and retrieves any saved username from the preferences.
    val app = LocalContext.current.applicationContext as GovCommApp
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(app.GovCommRepository))

    // Attempts to auto-login
    LaunchedEffect(Unit) { // Unit ensures this block runs once at composition
        FirebaseUtil.auth.currentUser?.let {
            // If there is a current user, navigate to the MenuScreen
            navController.navigate("MenuScreen") {
                popUpTo("LoginScreen") { inclusive = true }
            }
            userName.value = FirebaseUtil.auth.currentUser?.email ?: ""
        }
    }

    // UI state for password input.
    val password = remember { mutableStateOf("") }

    // State for managing snackbar notifications (Login Failure)
    val snackbarHostState = remember { SnackbarHostState() }

    // References to system services for controlling the keyboard and focus.
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Defines the action to perform login.
    val performLogin = {
        // Hide the keyboard and clear focus from the input fields.
        focusManager.clearFocus()
        keyboardController?.hide()

        signIn(userName.value, password.value) { success ->
            if (success) {
                // If login is successful, navigate to the MenuScreen
                navController.navigate("MenuScreen") {
                    popUpTo("LoginScreen") { inclusive = true }
                }
                userName.value = FirebaseUtil.auth.currentUser?.email ?: ""
            } else {
                // Show snackbar if login fails
                CoroutineScope(Dispatchers.Main).launch {
                    snackbarHostState.showSnackbar("Login failed. Please try again.")
                }
            }
        }
    }


    // Layout for the login screen, including text fields and buttons.
    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "GovComm Logo",
                modifier = Modifier.width(250.dp)
            )

            Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Username Field
            OutlinedTextField(
                value = userName.value,
                onValueChange = { userName.value = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Password Field
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false,
                    imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (userName.value.isNotEmpty() && password.value.isNotEmpty()) {
                        performLogin()
                    }
                })
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Buttons for submitting login form or navigating to sign-up screen.
            Button(
                onClick = { performLogin() },
                modifier = Modifier.fillMaxWidth(),
                enabled = userName.value.isNotEmpty() && password.value.isNotEmpty()
            ) { Text("Login") }

            Button(
                onClick = { navController.navigate(Screen.SignUpScreen.route) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign Up") }
        }

        // Snackbar for displaying login errors or messages.
        CustomSnackbar(snackbarHostState = snackbarHostState)
    }
}

// Custom Snackbar composable for displaying error messages.
@Composable
fun CustomSnackbar(snackbarHostState: SnackbarHostState) {
    SnackbarHost(hostState = snackbarHostState) { data ->
        // Custom Snackbar layout
        Snackbar(
            modifier = Modifier.padding(8.dp),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error icon",
                    tint = Color.Red
                )
                Spacer(Modifier.width(8.dp))
                Text(text = data.visuals.message)
            }
        }
    }
}

private fun signIn(email: String, password: String, callback: (Boolean) -> Unit) {
    FirebaseUtil.auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        }
}

@Preview
@Composable
fun LoginScreenPreview() {
    GovCommTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LoginScreen()
        }
    }
}