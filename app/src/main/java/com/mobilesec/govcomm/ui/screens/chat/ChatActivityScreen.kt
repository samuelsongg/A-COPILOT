package com.mobilesec.govcomm.ui.screens.chat

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ChatActivityScreen(
    navController: NavController = rememberNavController(),
    viewModel: ChatViewModel = viewModel(),
    userName: MutableState<String> = mutableStateOf(""), // Current/Logged in user email
    receiverEmail: String // The email of the chat partner
) {
    val context = LocalContext.current
    val textState = remember { mutableStateOf("") }
    val chats by viewModel.chats.collectAsState()
    val listState = rememberLazyListState()

    // Allows launching coroutines within this composable
    val coroutineScope = rememberCoroutineScope()

    // Receiver name
    var receiverName by remember { mutableStateOf("") }

    // Initialize ViewModel with userName and receiverEmail
    LaunchedEffect(key1 = userName, key2 = receiverEmail) {
        viewModel.initialize(userName.value, receiverEmail)
        User.fetchUserNameByEmail(receiverEmail) { name ->
            receiverName = name
        }
    }

    LaunchedEffect(chats.size) {
        // Checks if there are any messages to display.
        if (chats.isNotEmpty()) {
            coroutineScope.launch {
                // Scrolls to the bottom of the list to show the newest message.
                listState.animateScrollToItem(chats.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Chat with ${receiverName}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(chats) { chat ->
                        ChatMessage(chat, currentUserEmail = userName.value)
                    }
                }

                ChatInputField(textState, listState, onMessageSent = { message ->
                    viewModel.sendChat(
                        Chat(
                            senderEmail = userName.value,
                            receiverEmail = receiverEmail,
                            message = message,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    textState.value = ""
                })
            }
        }
    )
}

@Composable
fun ChatMessage(chat: Chat, currentUserEmail: String) {
    val isCurrentUser = chat.senderEmail == currentUserEmail
    // Define light shades of green and blue
    val LightShadeOfGreen = Color(0xFFB2F2BB) // Example light green color for current user
    val LightShadeOfBlue = Color(0xFFA0C4FF) // Example light blue color for other user
    val backgroundColor = if (isCurrentUser) LightShadeOfGreen else LightShadeOfBlue


    // Use a Box for conditional alignment of the message bubble
    Box(
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Surface(
            color = backgroundColor,
            shape = MaterialTheme.shapes.medium, // Rounded corners for the message bubble
            modifier = Modifier
                .widthIn(max = 300.dp) // Ensures the message bubble does not exceed this width
        ) {
            Column(modifier = Modifier.padding(all = 8.dp)) {
                Text(
                    text = chat.message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                )

                Text(
                    text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(chat.timestamp)),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(if (isCurrentUser) Alignment.End else Alignment.Start)
                )
            }
        }
    }
}

@Composable
fun ChatInputField(
    textState: MutableState<String>,
    listState: LazyListState,
    onMessageSent: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = textState.value,
            onValueChange = { newText -> textState.value = newText },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    if (it.isFocused) {
                        coroutineScope.launch {
                            // Workaround to scroll to the bottom when the keyboard is shown
                            listState.animateScrollToItem(0)
                            delay(300) // Adjust delay as needed
                            listState.animateScrollToItem(index = Int.MAX_VALUE)
                        }
                    }
                },
            placeholder = { Text("Type a message...") }
        )
        Button(
            onClick = {
                if (textState.value.isNotBlank()) {
                    onMessageSent(textState.value)
                    keyboardController?.hide()
                }
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Send")
        }
    }

}