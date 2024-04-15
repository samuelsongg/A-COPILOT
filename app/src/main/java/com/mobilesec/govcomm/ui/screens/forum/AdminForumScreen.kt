package com.mobilesec.govcomm.ui.screens.forum

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FieldValue
import com.mobilesec.govcomm.repo.FirebaseUtil
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminForumScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf(""),
    viewModel: ForumViewModel = viewModel()
) {
    var filterText by remember { mutableStateOf("") }
    val messages by viewModel.messagesFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forum (Admin)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
            )
        }) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        label = { Text("Filter By Question/Answer") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.applyFilter(filterText) }
                    ) {
                        Text("Filter")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    items(messages.size) { index ->
                        val message = messages[index]
                        MessageCard(
                            message = message,
                            onAnswerSubmitted = {
                                message, answerContent ->
                                viewModel.answerQuestion(message.id, userName.value, answerContent)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageCard(message: Message, onAnswerSubmitted: (Message, String) -> Unit) {
    var isAnswering by remember { mutableStateOf(false) }
    var answerText by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = message.user,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.timestamp,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Display answers
            message.answers.forEach { answer ->
                AnswerItem(answer)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Allow answering
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAnswering) {
                    Column {
                        OutlinedTextField(
                            value = answerText,
                            onValueChange = { answerText = it },
                            label = { Text("Your answer") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onAnswerSubmitted(message, answerText)
                                isAnswering = false
                            },
                            enabled = answerText.isNotBlank()
                        ) {
                            Text("Submit")
                        }
                    }
                } else {
                    Button(
                        onClick = { isAnswering = true }
                    ) {
                        Text("Answer")
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerItem(answer: Answer) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "${answer.author}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${answer.content}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${answer.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
