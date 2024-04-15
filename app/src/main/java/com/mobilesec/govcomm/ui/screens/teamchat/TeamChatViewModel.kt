package com.mobilesec.govcomm.ui.screens.teamchat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.mobilesec.govcomm.repo.FirebaseUtil
import com.mobilesec.govcomm.repo.GovCommRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class TeamChatViewModel() : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    init {
        viewModelScope.launch {
            getMessagesFlow().collect { messagesList ->
                _messages.value = messagesList
            }
        }
    }

    // Method to fetch chat messages as a Flow
    fun getMessagesFlow() = callbackFlow {
        val listenerRegistration = FirebaseUtil.db.collection("teamChat")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TeamChatViewModel", "Listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }

                snapshot?.let { snap ->
                    viewModelScope.launch {
                        val messagesList = snap.documents.mapNotNull { doc ->
                            val message = doc.toObject(Message::class.java)
                            message?.let {
                                try {
                                    // Fetch user details for each message
                                    val userDetails = FirebaseUtil.db.collection("userDetails")
                                        .document(message.user)
                                        .get()
                                        .await()
                                    message.firstName = userDetails.getString("firstName") ?: ""
                                    message.lastName = userDetails.getString("lastName") ?: ""
                                    message
                                } catch (e: Exception) {
                                    Log.e("TeamChatViewModel", "Error fetching user details", e)
                                    null
                                }
                            }
                        }
                        // Emit the list of messages with user details added
                        trySend(messagesList)
                    }
                }
            }

        // Await close, remove the listener when the Flow collection is stopped
        awaitClose { listenerRegistration.remove() }
    }



    fun sendMessage(userName: String, messageContent: String) {
        val timestamp = Timestamp.now() // Firestore Timestamp representing the current moment

        val message = hashMapOf(
            "user" to userName,
            "content" to messageContent,
            "timestamp" to timestamp // Use Firestore Timestamp
        )

        // Adds the message to the "teamChat" collection in Firestore.
        FirebaseUtil.db.collection("teamChat")
            .add(message)
            .addOnSuccessListener { Log.d("SendMessage", "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w("SendMessage", "Error writing document", e) }
    }

    fun shareLocation(context: Context, userName: String) {
        // Ensure context is used in a way that doesn't lead to memory leaks, e.g., use it directly without storing.
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationMessage = "Location: ${location.latitude}, ${location.longitude}"
                    sendMessage(userName, locationMessage)
                }
            }
        } else {
            // Log an error or handle the case where permissions are not granted
        }
    }

    fun uploadImageAndSendMessage(uri: Uri, userName: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")
        storageRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result.toString()
                sendMessage(userName, "Image: $downloadUri")
            } else {
                // Handle failures, e.g., log an error or update UI via state
            }
        }
    }





}