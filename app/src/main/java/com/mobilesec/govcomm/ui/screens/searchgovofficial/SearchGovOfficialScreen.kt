package com.mobilesec.govcomm.ui.screens.searchgovofficial

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.mobilesec.govcomm.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchGovOfficialScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf("")
) {
    var searchQuery by remember { mutableStateOf("") }
    var adminUsers by remember { mutableStateOf(emptyList<AdminUser>()) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch all results by default when the screen launches
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            fetchAdminUsers("") { users -> // Pass an empty string to fetch all users by default
                adminUsers = users
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Government Official") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Filter by name") },
                    modifier = Modifier.weight(1f) // Takes up the remaining space
                )
                Spacer(modifier = Modifier.width(8.dp)) // Add space between the text field and button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            fetchAdminUsers(searchQuery) { users ->
                                adminUsers = users
                            }
                        }
                    },
                    // Adjust modifier as needed, e.g., for size
                ) {
                    Text("Filter")
                }
            }
            LazyColumn {
                items(adminUsers) { user ->
                    AdminUserCard(user)
                }
            }
        }
    }
}

data class AdminUser(
    val firstName: String = "",
    val lastName: String = "",
    val userName: String = "",
    val jobDesignation: String = "",
    val jobDescription: String = "",
    val photo: String = ""
)

fun fetchAdminUsers(filter: String = "", onResult: (List<AdminUser>) -> Unit) {
    Firebase.firestore.collection("userDetails")
        .whereEqualTo("role", "Admin")
        .get()
        .addOnSuccessListener { snapshot ->
            val users = snapshot.documents.mapNotNull { document ->
                val user = document.toObject(AdminUser::class.java)
                user?.takeIf {
                    filter.isEmpty() || it.firstName.contains(filter, ignoreCase = true) ||
                            it.lastName.contains(filter, ignoreCase = true)
                }
            }
            onResult(users)
        }
        .addOnFailureListener {
            onResult(emptyList())
        }
}

@Composable
fun AdminUserCard(user: AdminUser) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Correctly specify elevation
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (user.photo.isNullOrEmpty()) {
                Icon(
                    Icons.Filled.Person, "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = user.photo),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text(text = "${user.userName}\n") //email

                Text(text = "Role:", fontWeight = FontWeight.Bold)
                Text(text = if (user.jobDesignation.isEmpty()) "No Content." else user.jobDesignation)

                Text(text = "\nRole Description:", fontWeight = FontWeight.Bold)
                Text(text = if (user.jobDescription.isEmpty()) "No Content." else user.jobDescription)
            }
        }
    }
}