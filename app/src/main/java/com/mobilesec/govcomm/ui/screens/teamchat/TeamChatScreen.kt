package com.mobilesec.govcomm.ui.screens.teamchat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mobilesec.govcomm.ui.theme.GovCommTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.google.firebase.Timestamp
import com.mobilesec.govcomm.R
import kotlin.math.*

// Data structure for a message
data class Message(
    val user: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,
    var firstName: String = "",
    var lastName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamChatScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf(""),
    viewModel: TeamChatViewModel = viewModel()
) {
    // Holds the list of messages to display on the screen, initially empty
    val messages by viewModel.messages.collectAsState()

    // Manage the position of the messages list on the screen
    val listState = rememberLazyListState()

    // Allows launching coroutines within this composable
    val coroutineScope = rememberCoroutineScope()

    // Determine if the last item is visible
    val isLastItemVisible = remember {
        derivedStateOf {
            val lastItemIndex = messages.lastIndex
            listState.layoutInfo.visibleItemsInfo.any { it.index == lastItemIndex }
        }
    }

    val context = LocalContext.current // Capture the context outside clickable
    val customImageLoader = remember { createCustomImageLoader(context) }

    // Observes changes in the size of the messages list to
    // automatically scroll to the latest message.
    LaunchedEffect(messages.size) {
        // Checks if there are any messages to display.
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                // Scrolls to the bottom of the list to show the newest message.
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Sets up the UI layout of the chat screen
    // including a top bar and a message input field.
    Column {
        TopAppBar(
            title = { Text("Team Message") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            )
        )

        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = messages, key = { message ->
                    message.timestamp.toString() + message.user }) { message ->
                    MessageCard(message = message, currentUser = userName.value, customImageLoader = customImageLoader)
                }
            }

            // Conditionally show the button
            if (!isLastItemVisible.value) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index = messages.lastIndex)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll to bottom")
                }
            }
        }

        // MessageInput is now outside the Box and placed directly within the Column
        MessageInput(
            userName = userName.value,
            listState = listState
        )
    }
}

// A component that displays a single message in the chat.
@Composable
fun MessageCard(message: Message, currentUser: String, customImageLoader: ImageLoader) { // Use a regular String if no need to observe changes
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("GMT+8")

    val context = LocalContext.current // Capture the context outside clickable

    // Safely call toDate() on timestamp if it's not null
    val dateString = message.timestamp?.toDate()?.let { sdf.format(it) } ?: "Unknown date"

    // Define light shades of green and blue
    val LightShadeOfGreen = Color(0xFFB2F2BB) // Example light green color
    val LightShadeOfBlue = Color(0xFFA0C4FF) // Example light blue color

    // Determine if the message is from the current user
    val isCurrentUser = message.user == currentUser

    // Adjust alignment and colors based on the user
    val backgroundColor = if (isCurrentUser) LightShadeOfGreen else LightShadeOfBlue
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    // Use a Box to control the alignment of the Surface
    Box(
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium, // Rounded corners
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = 300.dp) // Max width for messages; adjust as needed
                .wrapContentWidth(if (isCurrentUser) Alignment.End else Alignment.Start),
            color = backgroundColor,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "${message.firstName} ${message.lastName}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (message.content.startsWith("Location: ")) {
                    val location = message.content.removePrefix("Location: ")
                    val (latitude, longitude) = location.split(", ").map { it.trim() }

                    // Get openstreetmap tile info
                    val zoom = 17
                    val (x, y) = latLonToTileXY(latitude.toDouble(), longitude.toDouble(), zoom)

                    // Construct the URL for the static map image
                    val staticMapUrl = "https://api.maptiler.com/maps/streets-v2/${zoom}/${x}/${y}.png?key=YOUR_API_KEY"

                    Text(
                        text = message.content,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Image(
                        painter = rememberAsyncImagePainter(
                            model = staticMapUrl,
                            imageLoader = customImageLoader
                        ),
                        contentDescription = "Map Location",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable {
                                // Construct a Google Maps URL for the location
                                val mapsUrl =
                                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                // No need to set the package, so it opens in a web browser or the Maps app if it intercepts the URL
                                context.startActivity(intent)
                            }
                    )

                } else if (message.content.startsWith("Image: ")) {
                    // Display the image
                    val imageUrl = message.content.removePrefix("Image: ")
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = imageUrl,
                            imageLoader = customImageLoader
                        ),
                        contentDescription = "Sent image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl))
                                context.startActivity(intent)
                            }
                    )
                } else {
                    // Display regular text message
                    Text(
                        text = message.content,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sdf.format(message.timestamp?.toDate() ?: Date()),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                    modifier = Modifier.align(if (isCurrentUser) Alignment.End else Alignment.Start)
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    userName: String,
    viewModel: TeamChatViewModel = viewModel(),
    listState: LazyListState
) {
    val context = LocalContext.current
    var showOptionsDialog by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.uploadImageAndSendMessage(it, userName)
        }
    }

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Send...") },
            text = {
                Column {
                    TextButton(onClick = {
                        // Trigger image picking
                        pickImageLauncher.launch("image/*")
                        showOptionsDialog = false
                    }) {
                        Text("Image")
                    }
                    TextButton(onClick = {
                        viewModel.shareLocation(context, userName)
                        showOptionsDialog = false
                    }) {
                        Text("Location")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showOptionsDialog = false }) { Text("Cancel") } }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { newText -> text = newText }, // Update to accept String
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
            placeholder = { Text("Type a message") }
        )

        IconButton(modifier = Modifier.padding(0.dp), onClick = { showOptionsDialog = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        Button(onClick = {
            if (text.isNotBlank()) {
                viewModel.sendMessage(userName, text)
                text = "" // Clear the text field
                keyboardController?.hide() // Optionally hide the keyboard
            }
        }) {
            Text("Send")
        }


    }
}

fun latLonToTileXY(latitude: Double, longitude: Double, zoom: Int): Pair<Int, Int> {
    val latRad = Math.toRadians(latitude)
    val n = 2.0.pow(zoom.toDouble())
    val xTile = floor((longitude + 180.0) / 360.0 * n).toInt()
    val yTile = floor((1.0 - ln(tan(latRad) + 1 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
    return Pair(xTile, yTile)
}

fun createCustomImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .build()
}

@Preview(showBackground = true)
@Composable
fun PreviewTeamChatScreen() {
    GovCommTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TeamChatScreen()
        }
    }
}