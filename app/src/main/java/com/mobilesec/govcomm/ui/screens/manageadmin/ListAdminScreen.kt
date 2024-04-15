package com.mobilesec.govcomm.ui.screens.manageadmin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

data class AdminUser(
    val firstName: String = "",
    val lastName: String = "",
    val userName: String = "",
    val role: String = "",
)

fun fetchAdminUsers(onResult: (List<AdminUser>) -> Unit) {
    Firebase.firestore.collection("userDetails")
        .whereEqualTo("role", "Admin")
        .get()
        .addOnSuccessListener { snapshot ->
            val users = snapshot.documents.mapNotNull { document ->
                val user = document.toObject(AdminUser::class.java)
                if (user != null && user.role == "Admin") user else null
            }
            onResult(users)
        }
        .addOnFailureListener {
            onResult(emptyList())
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListAdminScreen(
    navController: NavController,
    userName: MutableState<String> = mutableStateOf("")
) {
    var adminUsers by remember { mutableStateOf(emptyList<AdminUser>()) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch all admin users when the screen is initially composed
    LaunchedEffect(key1 = true) {
        coroutineScope.launch {
            fetchAdminUsers { users ->
                adminUsers = users
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("List of Admins") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                // Padding for Button is moved inside its own modifier
                Button(
                    onClick = { navController.navigate("ManageAdminScreen") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Add/Remove Admin")
                }

                // LazyColumn takes up the remaining space without additional padding here
                // to avoid doubling up padding between elements
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(adminUsers) { user ->
                        AdminUserCard(user)
                    }
                }
            }
        }
    )
}

@Composable
fun AdminUserCard(user: AdminUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Apply vertical padding to space out items in the list
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            // If you're displaying an image, the Image composable would go here

            Column {
                Text(text = "${user.firstName} ${user.lastName}", fontWeight = FontWeight.Bold)
                Text(text = user.userName)
            }
        }
    }
}