package com.mobilesec.govcomm.ui.screens.menu

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mobilesec.govcomm.GovCommApp
import com.mobilesec.govcomm.repo.FirebaseUtil
import com.mobilesec.govcomm.ui.theme.GovCommTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf("")
) {
    // Initialize ViewModel
    val app = LocalContext.current.applicationContext as GovCommApp
    val viewModel: MenuViewModel = viewModel(factory = MenuViewModelFactory(app.GovCommRepository))
    var userRole by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Main Menu") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary),
                actions = {
                    IconButton(onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            // Sign out logic
                            FirebaseUtil.auth.signOut()
                            userName.value = ""
                            navController.navigate("LoginScreen") {
                                popUpTo("LoginScreen") { inclusive = true }
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally)
            {
                item {
                    var firstName by remember { mutableStateOf("") }
                    var lastName by remember { mutableStateOf("") }

                    // User details retrieval logic
                    if (userName.value.isNotEmpty()) {
                        viewModel.getUserDetails(userName.value) { retrievedFirstName, retrievedLastName, retrievedRole ->
                            firstName = retrievedFirstName
                            lastName = retrievedLastName
                            userRole = retrievedRole
                        }
                    }

                    // Greeting text
                    Text(
                        text = "Hello $firstName $lastName",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Only show if user is admin:
                    if (userRole == "Admin") {
                        // Admin Group Buttons
                        GroupButtons(
                            title = "Admin Tools",
                            buttons = listOf(
                                ButtonItem(Icons.Filled.AccountBox, "Manage Admin Accounts", { navController.navigate("ListAdminScreen") }),
                                ButtonItem(Icons.AutoMirrored.Filled.Chat, "Admin Team Chat", { navController.navigate("TeamChatScreen") }),
                                ButtonItem(Icons.Filled.QuestionAnswer, "Answer Q&A", { navController.navigate("AdminForumScreen") })
                            )
                        )
                    }

                    // All Users Group Buttons
                    GroupButtons(
                        title = "General",
                        buttons = listOf(
                            ButtonItem(Icons.Filled.Lock, "Change Password", { navController.navigate("ChangePasswordScreen") }),
                            ButtonItem(Icons.Filled.Edit, "Update Profile", { navController.navigate("UpdateProfileScreen") }),
                            ButtonItem(Icons.AutoMirrored.Filled.Message, "Direct Message", { navController.navigate("SelectChatScreen") }),
                            ButtonItem(Icons.Filled.Search, "Search Government Officials", { navController.navigate("SearchGovOfficialScreen") }),
                            ButtonItem(Icons.Filled.Public, "Public Q&A", { navController.navigate("PublicForumScreen") }),
                            ButtonItem(Icons.AutoMirrored.Filled.Logout, "Logout") {
                                CoroutineScope(Dispatchers.Main).launch {
                                    FirebaseUtil.auth.signOut()
                                    userName.value = ""
                                    navController.navigate("LoginScreen") {
                                        popUpTo("LoginScreen") { inclusive = true }
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
    }
}


data class ButtonItem(val icon: ImageVector, val text: String, val onClick: () -> Unit)

@Composable
fun GroupButtons(title: String, buttons: List<ButtonItem>) {
    Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            buttons.forEach { button ->
                Button(
                    onClick = button.onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(button.icon, contentDescription = button.text)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(button.text)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Preview
@Composable
fun MenuScreenPreview() {
    GovCommTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MenuScreen()
        }
    }
}