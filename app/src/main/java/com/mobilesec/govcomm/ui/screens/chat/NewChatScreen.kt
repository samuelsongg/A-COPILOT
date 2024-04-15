package com.mobilesec.govcomm.ui.screens.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.mobilesec.govcomm.ui.screens.searchgovofficial.AdminUser
import com.mobilesec.govcomm.ui.screens.searchgovofficial.fetchAdminUsers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    navController: NavController,
    userName: MutableState<String>
) {
    var searchQuery by remember { mutableStateOf("") }
    var adminUsers by remember { mutableStateOf(emptyList<AdminUser>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            fetchAdminUsers("") { users ->
                adminUsers = users
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start a Conversation") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter/Search UI omitted for brevity, add if necessary
            LazyColumn {
                items(adminUsers) { user ->
                    // Don't show current user so they can't talk to themselves
                    if (user.userName != userName.value){
                        AdminUserCard(user, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(user: AdminUser, navController: NavController) {
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

                Spacer(modifier = Modifier.padding(8.dp))

                Button(onClick = {
                    // Navigate to chat screen with user and clear the current screen from the back stack
                    navController.navigate("chatActivityScreen/${user.userName}") {
                        popUpTo("SelectChatScreen") { inclusive = false }
                    }
                }) {
                    Text("Start Chat")
                }
            }
        }
    }
}