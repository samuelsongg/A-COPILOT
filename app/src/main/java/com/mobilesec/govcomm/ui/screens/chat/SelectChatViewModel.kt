package com.mobilesec.govcomm.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

class SelectChatViewModel() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _latestMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val latestMessages: StateFlow<List<ChatMessage>> = _latestMessages
    private var userName: String = ""
    private var listenerRegistrations = mutableListOf<ListenerRegistration>()

    fun initialize(userName: String) {
        this.userName = userName
        // Clear previous listeners
        clearListeners()
        fetchLatestConversations(userName)
    }

    private fun clearListeners() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    private fun fetchLatestConversations(currentUserEmail: String) {
        // Fetch messages sent by the current user
        val sentMessagesListener = db.collection("Chat")
            .whereEqualTo("senderEmail", currentUserEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Listen failed: $e")
                    return@addSnapshotListener
                }

                processSnapshot(snapshot, currentUserEmail)
            }
        listenerRegistrations.add(sentMessagesListener)

        // Fetch messages received by the current user
        val receivedMessagesListener = db.collection("Chat")
            .whereEqualTo("receiverEmail", currentUserEmail)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Listen failed: $e")
                    return@addSnapshotListener
                }

                processSnapshot(snapshot, currentUserEmail)
            }
        listenerRegistrations.add(receivedMessagesListener)
    }

    private fun processSnapshot(snapshot: QuerySnapshot?, currentUserEmail: String) {
        val allMessages = snapshot?.toObjects(ChatMessage::class.java) ?: listOf()
        val conversationMap = _latestMessages.value.associateBy {
            if (it.senderEmail == currentUserEmail) it.receiverEmail else it.senderEmail
        }.toMutableMap()

        // Update the conversation map with the latest messages
        allMessages.forEach { message ->
            val otherPartyEmail = if (message.senderEmail == currentUserEmail) message.receiverEmail else message.senderEmail
            val existingMessage = conversationMap[otherPartyEmail]
            if (existingMessage == null || message.timestamp > existingMessage.timestamp) {
                conversationMap[otherPartyEmail] = message
            }
        }

        _latestMessages.value = conversationMap.values.toList().sortedByDescending { it.timestamp }
    }

    override fun onCleared() {
        super.onCleared()
        // Make sure to remove listeners when the ViewModel is cleared to prevent memory leaks
        clearListeners()
    }
}
