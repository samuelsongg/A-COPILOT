package com.mobilesec.govcomm.ui.screens.forum

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FieldValue
import com.mobilesec.govcomm.repo.FirebaseUtil
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicForumScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf(""),
    viewModel: ForumViewModel = viewModel()
) {
    var question by remember { mutableStateOf("") }

    var filterText by remember { mutableStateOf("") }
    val messages by viewModel.messagesFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forum") },
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
                Text("Question:",
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Enter your question") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp * 2)
                            .padding(end = 8.dp),
                        singleLine = false,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.askQuestion(userName.value, question) // Call the askQuestion function when the button is clicked
                            question = ""
                        },
                        enabled = question.isNotBlank()
                    ) {
                        Text("Ask")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color.Gray, modifier = Modifier
                    .fillMaxWidth()
                    .width(1.dp)
                )

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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        messages.forEach { message ->
                            MessageCard(message)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun MessageCard(message: Message) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier) // Keep for alignment
                Text(text = message.timestamp, style = MaterialTheme.typography.bodySmall)
            }

            // Display answers
            message.answers.forEach { answer ->
                AnswerItemContent(answer)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AnswerItemContent(answer: Answer) {
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
                text = answer.author,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = answer.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = answer.timestamp,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
