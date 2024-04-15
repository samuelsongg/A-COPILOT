package com.mobilesec.govcomm.ui.screens.signup

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.mobilesec.govcomm.Screen
import com.mobilesec.govcomm.repo.FirebaseUtil
import com.mobilesec.govcomm.ui.theme.GovCommTheme

@Composable
fun SignUpScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf("")
) {
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    var validationSuccess by remember { mutableStateOf(true) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (!validationSuccess) {
            ValidationFailedMessage()
        }

        OutlinedTextField(
            value = firstName.value,
            onValueChange = {firstName.value = it },
            label = { Text(text = "First Name")},
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = lastName.value,
            onValueChange = {lastName.value = it },
            label = { Text(text = "Last Name")},
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = userName.value,
            onValueChange = { userName.value = it },
            label = { Text(text = "Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(text = "Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = { confirmPassword.value = it },
            label = { Text(text = "Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                imeAction = ImeAction.Done
            )
        )

        Button(
            // Validates all fields on client-side.
            onClick = { validationSuccess = validateName(firstName.value, lastName.value) && validateEmail(userName.value)
                                            && validatePassword(password.value, confirmPassword.value)

                // If validation successful, add user to DB and navigate to login screen.
                if (validationSuccess) {
                    signUpUser(userName.value, password.value)
                    addUserDB(firstName.value, lastName.value, userName.value)
                    navController.navigate(Screen.LoginScreen.route)
                }
            },
            enabled = firstName.value.isNotEmpty() && userName.value.isNotEmpty() &&
                    password.value.isNotEmpty() && confirmPassword.value.isNotEmpty()
            ) {
            Text(text = "Sign Up")
        }
        
        Button(
            onClick = { navController.navigate(Screen.LoginScreen.route) }
        ) {
            Text(text = "Back to Login")
        }
    }
}

fun validateName(firstName: String, lastName: String): Boolean {
    val nameRegex = "[a-zA-Z]+".toRegex()
    return firstName.matches(nameRegex) && lastName.matches(nameRegex)
}

fun validateEmail(email: String): Boolean {
    val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
    return email.matches(emailRegex)
}

fun validatePassword(password: String, confirmPassword: String): Boolean {
    return password == confirmPassword
}

@Composable
fun ValidationFailedMessage() {
    Text(text = "There is an error with one of your details.\nPlease try again.",
        textAlign = TextAlign.Center,
        color = Color.Red
    )
}

// Function to add new user to Firebase Auth.
fun signUpUser(email: String, password: String) {
    // Initialise Firebase Auth
    val auth: FirebaseAuth = Firebase.auth

    auth.createUserWithEmailAndPassword(email.lowercase(), password)
        .addOnCompleteListener { task ->
            // If sign up is successful.
            if (task.isSuccessful) {
                Log.d(TAG, "createUserWithEmail:success")
                val user = auth.currentUser
                // Todo: Add success message.
            } else {
                // If sign up fails.
                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                // Todo: Add error message.
            }
        }
}

// Function to add new user's details to Firestore (DB).
fun addUserDB(firstName: String, lastName: String = "", userName: String) {
    // Create a new user.
    val user = hashMapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "userName" to userName,
        "role" to "Public"
    )

    // Add user to userDetails collection.
    // Auto create collection if doesn't exist.
    FirebaseUtil.db.collection("userDetails").document(userName.lowercase())
        .set(user)
        .addOnSuccessListener { documentReference ->
            Log.d(TAG, "DocumentSnapshot added with ID: ${userName.lowercase()}")
        }
        .addOnFailureListener{ e ->
            Log.w(TAG, "Error adding document", e)
        }
}

@Preview
@Composable
fun SignUpScreenPreview() {
    GovCommTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SignUpScreen()
        }
    }
}