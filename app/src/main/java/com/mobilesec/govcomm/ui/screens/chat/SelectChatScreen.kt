package com.mobilesec.govcomm.ui.screens.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SelectChatScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf(""),
    viewModel: SelectChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val latestConversations by viewModel.latestMessages.collectAsState()

    LaunchedEffect(userName) {
        viewModel.initialize(userName.value)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Message") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(onClick = {
                        navController.navigate("NewChatScreen")
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        // Moved the ConversationList call here and passed paddingValues to it
        ConversationList(latestConversations, navController, paddingValues, userName)
    }
}

@Composable
fun ConversationList(conversations: List<ChatMessage>, navController: NavController, paddingValues: PaddingValues, userName: MutableState<String>) {
    LazyColumn(modifier = Modifier.padding(paddingValues)) {
        items(conversations) { conversation ->
            ConversationItem(conversation, userName.value, onClick = {
                if (conversation.senderEmail == userName.value) {
                    navController.navigate("chatActivityScreen/${conversation.receiverEmail}")
                } else {
                    navController.navigate("chatActivityScreen/${conversation.senderEmail}")
                }
            })
        }
    }
}

@Composable
fun ConversationItem(conversation: ChatMessage, userName: String, onClick: () -> Unit) {
    var otherPersonName by remember { mutableStateOf("") }
    val otherPersonEmail = if (conversation.senderEmail == userName) {
        conversation.receiverEmail
    } else {
        conversation.senderEmail
    }

    // Fetch the other person's name based on their email
    LaunchedEffect(otherPersonEmail) {
        User.fetchUserNameByEmail(otherPersonEmail) { name ->
            otherPersonName = name
        }
    }

    val lastMessagePrefix = if (conversation.senderEmail == userName) "You: " else "They: "

    // Use small gray font for the timestamp
    val timestampTextStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = otherPersonName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "$lastMessagePrefix${conversation.message}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(conversation.timestamp)),
                style = timestampTextStyle,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

