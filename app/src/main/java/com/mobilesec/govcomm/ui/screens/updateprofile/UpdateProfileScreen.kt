package com.mobilesec.govcomm.ui.screens.updateprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mobilesec.govcomm.R // Make sure to replace with your actual R file path
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import coil.compose.rememberAsyncImagePainter
import com.mobilesec.govcomm.repo.FirebaseUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    navController: NavController = rememberNavController(),
    userName: MutableState<String> = mutableStateOf("")
) {

    // Input fields
    var profile by remember { mutableStateOf(Profile("", "", "", "", "", "", "")) }
    var updateInProgress by remember { mutableStateOf(false) }

    // For image
    var selectedImageFileName by remember { mutableStateOf("") }

    // Load initial profile values
    LaunchedEffect(Unit) {
        loadUserProfile(userName.value) { loadedProfile ->
            profile = loadedProfile
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Profile") }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        )
        {
            item {
                var imageUri by remember { mutableStateOf<Uri?>(null) }
                val context = LocalContext.current

                Spacer(modifier = Modifier.height(20.dp))

                // Profile Picture
                if (profile.photoUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(profile.photoUrl),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Filled.Person, "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                }

                // Only if admin
                if (profile.role == "Admin") {
                    ImagePicker(onImagePicked = { uri ->
                        imageUri = uri
                        // Extract and display the file name from the URI
                        selectedImageFileName = uri.path?.split("/")?.lastOrNull() ?: "Selected Image"
                    })

                    // Display the selected image file name if available
                    if (selectedImageFileName.isNotEmpty()) {
                        Text("New Image Selected", Modifier.padding(bottom = 8.dp))
                    } else {
                        // Optionally show a placeholder or nothing if no file is selected
                        Text("No image selected", Modifier.padding(bottom = 8.dp))
                    }
                }

                // First Name
                UpdateProfileTextField(
                    value = profile.firstName,
                    onValueChange = { updatedValue -> profile = profile.copy(firstName = updatedValue) },
                    label = "First Name"
                )

                // Last Name
                UpdateProfileTextField(
                    value = profile.lastName,
                    onValueChange = { updatedValue -> profile = profile.copy(lastName = updatedValue) },
                    label = "Last Name"
                )

                // Email/Username - Assuming it's not editable, but shown for completeness
                UpdateProfileTextField(
                    value = profile.email,
                    onValueChange = { updatedValue -> profile = profile.copy(email = updatedValue) },
                    label = "Email/Username",
                    enabled = false // Assuming email is not editable
                )

                // Role - Also assuming it's not editable, but updated for consistency
                UpdateProfileTextField(
                    value = profile.role,
                    onValueChange = { updatedValue -> profile = profile.copy(role = updatedValue) },
                    label = "Role",
                    enabled = false // Assuming role is not editable
                )

                // Job Designation - Only for Admins
                if (profile.role == "Admin") {
                    UpdateProfileTextField(
                        value = profile.jobDesignation,
                        onValueChange = { updatedValue -> profile = profile.copy(jobDesignation = updatedValue) },
                        label = "Job Designation"
                    )
                }

                // Job Description - Only for Admins
                if (profile.role == "Admin") {
                    UpdateProfileTextField(
                        value = profile.jobDescription,
                        onValueChange = { updatedValue -> profile = profile.copy(jobDescription = updatedValue) },
                        label = "Job Description"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        updateInProgress = true
                        updateProfile(profile, imageUri) { success ->
                            updateInProgress = false
                            if (success) {
                                loadUserProfile(userName.value) { updatedProfile ->
                                    profile = updatedProfile
                                    Toast.makeText(
                                        context,
                                        "Profile updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to update profile",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !updateInProgress && profile.firstName.isNotBlank() && profile.lastName.isNotBlank()
                ) {
                    Text(if (updateInProgress) "Updating..." else "Update Profile")
                }
            }
        }
    }
}

@Composable
fun UpdateProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        enabled = enabled
    )
}

data class Profile(
    var firstName: String,
    var lastName: String,
    var email: String,
    var role: String,
    var jobDesignation: String,
    var jobDescription: String,
    var photoUrl: String
)

fun loadUserProfile(userName: String, onProfileLoaded: (Profile) -> Unit) {
    FirebaseUtil.db.collection("userDetails").document(userName).get()
        .addOnSuccessListener { document ->
            val profile = Profile(
                firstName = document.getString("firstName") ?: "",
                lastName = document.getString("lastName") ?: "",
                email = document.getString("userName") ?: "",
                role = document.getString("role") ?: "",
                jobDesignation = document.getString("jobDesignation") ?: "",
                jobDescription = document.getString("jobDescription") ?: "",
                photoUrl = document.getString("photo") ?: ""
            )
            onProfileLoaded(profile)
        }
}

fun updateProfile(
    profile: Profile,
    newPhotoUri: Uri?,
    onComplete: (Boolean) -> Unit
) {
    val updates = mutableMapOf<String, Any?>(
        "firstName" to profile.firstName,
        "lastName" to profile.lastName
    )

    if (profile.role == "Admin") {
        updates["jobDesignation"] = profile.jobDesignation
        updates["jobDescription"] = profile.jobDescription
        // Handle photo update logic for Admins
        newPhotoUri?.let { uri ->
            uploadImageToFirebaseStorage(uri, profile.email) { photoUrl ->
                if (photoUrl.isNotEmpty()) {
                    updates["photo"] = photoUrl
                    performFirestoreUpdate(profile.email, updates, onComplete)
                } else {
                    // Handle upload failure
                    onComplete(false)
                }
            }
        } ?: run {
            // No new image selected; proceed with other updates
            performFirestoreUpdate(profile.email, updates, onComplete)
        }
    } else {
        // For non-Admins, skip job and photo updates
        performFirestoreUpdate(profile.email, updates, onComplete)
    }
}

fun performFirestoreUpdate(userName: String, updates: Map<String, Any?>, onComplete: (Boolean) -> Unit) {
    FirebaseUtil.db.collection("userDetails").document(userName)
        .update(updates)
        .addOnSuccessListener { onComplete(true) }
        .addOnFailureListener { onComplete(false) }
}



fun updateUserProfile(
    userName: String,
    firstName: String,
    lastName: String,
    jobDesignation: String,
    jobDescription: String,
    onComplete: (Boolean) -> Unit
) {
    val userUpdates = hashMapOf<String, Any?>(
        "firstName" to firstName,
        "lastName" to lastName,
        "jobDesignation" to jobDesignation,
        "jobDescription" to jobDescription
    )

    FirebaseUtil.db.collection("userDetails").document(userName)
        .update(userUpdates)
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener {
            onComplete(false)
        }
}

@Composable
fun ImagePicker(onImagePicked: (Uri) -> Unit) {
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { onImagePicked(it) }
        }

    Button(onClick = { launcher.launch("image/*") }) {
        Text("Change Profile Picture")
    }
}

fun uploadImageToFirebaseStorage(
    imageUri: Uri,
    userName: String,
    onUploadComplete: (String) -> Unit
) {
    val storageRef = FirebaseUtil.storage.reference.child("profilePictures/$userName.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                onUploadComplete(uri.toString())
            }
        }
        .addOnFailureListener {
            onUploadComplete("")
        }
}

@Preview(showBackground = true)
@Composable
fun UpdateProfileScreenPreview() {
    UpdateProfileScreen()
}