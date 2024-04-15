package com.mobilesec.govcomm.ui.screens.manageadmin

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAdminScreen(
    navController: NavController,
    userName: MutableState<String> = mutableStateOf("")
) {
    var username by remember { mutableStateOf("") }
    var userDetails by remember { mutableStateOf<AdminUser?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) } // For displaying error messages
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Admin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.lowercase()
                            errorMessage = null // Clear error message when user starts typing again
                        },
                        label = { Text("Username (email)") },
                        modifier = Modifier.weight(1f), // Use weight to fill available space
                        isError = errorMessage != null // Highlight the text field if there's an error
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Spacer for horizontal spacing between TextField and Button
                    Button(
                        onClick = {
                            if (username.isBlank()) {
                                errorMessage = "Username cannot be empty"
                                return@Button
                            }
                            coroutineScope.launch {
                                fetchUserDetails(username) { user ->
                                    if (user == null) {
                                        errorMessage = "User not found"
                                    } else {
                                        userDetails = user
                                        errorMessage = null // Clear error message on successful fetch
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Submit")
                    }
                }

                // Display error message if not null
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp)) // Spacer for vertical spacing after the row

                userDetails?.let { user ->
                    UserDetailsCard(user)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                setUserRole(user.userName, "Admin") {
                                    // Re-fetch user details upon successful role update
                                    fetchUserDetails(user.userName) { updatedUser ->
                                        userDetails = updatedUser
                                    }
                                }
                            },
                            enabled = user.role != "Admin",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set Admin")
                        }
                        Spacer(modifier = Modifier.width(8.dp)) // Spacer for horizontal spacing between buttons
                        Button(
                            onClick = {
                                setUserRole(user.userName, "Public") {
                                    // Re-fetch user details upon successful role update
                                    fetchUserDetails(user.userName) { updatedUser ->
                                        userDetails = updatedUser
                                    }
                                }
                            },
                            enabled = user.role != "Public",
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set Public")
                        }
                    }
                }
            }
        }
    )
}


fun fetchUserDetails(username: String, onResult: (AdminUser?) -> Unit) {
    Firebase.firestore.collection("userDetails")
        .document(username)
        .get()
        .addOnSuccessListener { document ->
            val user = document.toObject(AdminUser::class.java)
            onResult(user)
        }
        .addOnFailureListener {
            onResult(null)
        }
}

fun setUserRole(username: String, role: String, onUpdateComplete: () -> Unit) {
    val userRef = Firebase.firestore.collection("userDetails").document(username)
    userRef.update("role", role)
        .addOnSuccessListener {
            Log.d("ConfigureUserRole", "User role updated successfully to $role")
            onUpdateComplete() // Call the completion handler
        }
        .addOnFailureListener { e ->
            Log.w("ConfigureUserRole", "Error updating user role", e)
        }
}

@Composable
fun UserDetailsCard(user: AdminUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Name: ${user.firstName} ${user.lastName}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Email: ${user.userName}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Role: ${user.role}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}