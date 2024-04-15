package com.mobilesec.govcomm.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mobilesec.govcomm.repo.FirebaseUtil.db
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel() : ViewModel() {
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    // Keep track of the listener registration to properly remove it later
    private var chatListenerRegistration: ListenerRegistration? = null

    private var userName: String = ""
    private var receiverEmail: String = ""

    fun initialize(userName: String, receiverEmail: String) {
        this.userName = userName
        this.receiverEmail = receiverEmail

        // Remove existing listener if any before setting up a new one
        chatListenerRegistration?.remove()
        fetchChats(userName, receiverEmail)
    }

    private fun fetchChats(senderEmail: String, receiverEmail: String) {
        // Create a query for both conditions: current user as sender and as receiver
        val query = db.collection("Chat")
            .whereIn("senderEmail", listOf(senderEmail, receiverEmail))
            .whereIn("receiverEmail", listOf(senderEmail, receiverEmail))

        // Set up the listener
        chatListenerRegistration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ChatViewModel", "Listen failed.", e)
                return@addSnapshotListener
            }

            val chatList = snapshot?.documents?.mapNotNull { it.toObject(Chat::class.java) } ?: listOf()

            // Filter out the chats that don't involve both the sender and receiver
            val filteredChats = chatList.filter { (it.senderEmail == senderEmail && it.receiverEmail == receiverEmail) || (it.senderEmail == receiverEmail && it.receiverEmail == senderEmail) }

            // Sort the filtered list by timestamp
            _chats.value = filteredChats.sortedBy { it.timestamp }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Remove listener when ViewModel is cleared to prevent memory leaks
        chatListenerRegistration?.remove()
    }

    fun sendChat(message: Chat) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("Chat").add(message).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
