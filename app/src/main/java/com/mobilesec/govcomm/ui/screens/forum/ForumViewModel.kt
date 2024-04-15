package com.mobilesec.govcomm.ui.screens.forum

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.Query
import com.mobilesec.govcomm.repo.FirebaseUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose

data class Message(
    val id: String,
    val user: String,
    val content: String,
    val timestamp: String,
    val answers: List<Answer>
)

data class Answer(
    val author: String,
    val content: String,
    val timestamp: String
)


class ForumViewModel : ViewModel() {
    private val _allMessages = MutableStateFlow<List<Message>>(emptyList()) // Holds all messages unfiltered
    private val _filteredMessages = MutableStateFlow<List<Message>>(emptyList()) // Holds filtered messages
    private var questionsListenerRegistration: ListenerRegistration? = null
    private val answersListeners = mutableMapOf<String, ListenerRegistration>()
    private var currentFilter = "" // Tracks the current filter text

    // Expose only the filtered messages to the UI
    val messagesFlow: StateFlow<List<Message>> = _filteredMessages.asStateFlow()

    init {
        listenToQuestionsAndAnswers()
        _allMessages.onEach { applyFilter(currentFilter) }.launchIn(viewModelScope)
    }

    private fun listenToQuestionsAndAnswers() = viewModelScope.launch(Dispatchers.IO) {
        // Listener for questions
        questionsListenerRegistration = FirebaseUtil.db.collection("forum")
            .orderBy("datetime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ForumViewModel", "Questions listen failed.", e)
                    return@addSnapshotListener
                }

                refreshAllMessages()

                snapshot?.documents?.forEach { document ->
                    val questionId = document.id

                    if (!answersListeners.containsKey(questionId)) {
                        val answersListener = FirebaseUtil.db.collection("forum")
                            .document(questionId)
                            .collection("answers")
                            .orderBy("datetime", Query.Direction.ASCENDING)
                            .addSnapshotListener { _, answersException ->
                                if (answersException != null) {
                                    Log.w("ForumViewModel", "Answers listen failed.", answersException)
                                    return@addSnapshotListener
                                }

                                refreshAllMessages()
                            }

                        answersListeners[questionId] = answersListener
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupListeners()
    }

    private fun cleanupListeners() {
        questionsListenerRegistration?.remove()
        answersListeners.values.forEach { it.remove() }
        answersListeners.clear()
    }

    private fun refreshAllMessages() = viewModelScope.launch(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        try {
            val questionsSnapshot = FirebaseUtil.db.collection("forum")
                .orderBy("datetime", Query.Direction.DESCENDING)
                .get()
                .await()

            val messages = questionsSnapshot.documents.mapNotNull { document ->
                async {
                    val questionId = document.id
                    val author = document.getString("author") ?: ""
                    val content = document.getString("content") ?: ""
                    val datetime = document.getTimestamp("datetime")?.toDate()?.let { dateFormat.format(it) } ?: ""

                    // Fetch answers for this question
                    val answersSnapshot = FirebaseUtil.db.collection("forum")
                        .document(questionId)
                        .collection("answers")
                        .orderBy("datetime", Query.Direction.ASCENDING)
                        .get()
                        .await()

                    val answers = answersSnapshot.documents.mapNotNull { answerDoc ->
                        val answerAuthor = answerDoc.getString("author") ?: ""
                        val answerContent = answerDoc.getString("content") ?: ""
                        val answerDatetime = answerDoc.getTimestamp("datetime")?.toDate()?.let { dateFormat.format(it) } ?: ""

                        val userDetails = FirebaseUtil.db.collection("userDetails")
                            .document(answerAuthor)
                            .get()
                            .await()
                        val firstName = userDetails.getString("firstName") ?: ""
                        val lastName = userDetails.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"

                        Answer(fullName, answerContent, answerDatetime)
                    }

                    val userDetails = FirebaseUtil.db.collection("userDetails")
                        .document(author)
                        .get()
                        .await()
                    val firstName = userDetails.getString("firstName") ?: ""
                    val lastName = userDetails.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName"

                    Message(questionId, fullName, content, datetime, answers)
                }
            }.awaitAll()

            // Post value to StateFlow
            _allMessages.value = messages
        } catch (e: Exception) {
            Log.w("ForumViewModel", "Error refreshing messages", e)
        }
    }

    fun applyFilter(filter: String) {
        currentFilter = filter // Update the current filter
        viewModelScope.launch {
            val filtered = if (filter.isBlank()) {
                _allMessages.value // If filter is blank, use all messages
            } else {
                _allMessages.value.filter { message ->
                    message.content.contains(filter, ignoreCase = true) ||
                            message.answers.any { answer -> answer.content.contains(filter, ignoreCase = true) }
                }
            }
            _filteredMessages.value = filtered // Update the flow with filtered results
        }
    }

    fun askQuestion(author: String, content: String) {
        if (content.isNotBlank()) {
            // Prepare the data to be saved in Firestore
            val forumData = hashMapOf(
                "author" to author,
                "public" to "Yes",  // Assuming "public" is a required field
                "content" to content,
                "datetime" to FieldValue.serverTimestamp()  // Use server timestamp for the current time
            )

            // Save the question to Firestore
            FirebaseUtil.db.collection("forum")
                .add(forumData)
                .addOnSuccessListener { documentReference ->
                    Log.d("ForumViewModel", "Question added with ID: ${documentReference.id}")
                    // Optionally, you can perform actions post-success, like emitting an event or updating a LiveData/StateFlow
                }
                .addOnFailureListener { e ->
                    Log.w("ForumViewModel", "Error adding question", e)
                    // Handle error if needed
                }
        }
    }

    fun answerQuestion(questionId: String, author: String, answerContent: String) {
        if (answerContent.isNotBlank()) {
            // Prepare the data to be saved in Firestore
            val answerData = hashMapOf(
                "author" to author,
                "content" to answerContent,
                "datetime" to FieldValue.serverTimestamp() // Use server timestamp for the current time
            )

            // Save the answer to Firestore under the 'answers' subcollection of the question
            FirebaseUtil.db.collection("forum")
                .document(questionId) // Use the provided question ID
                .collection("answers") // Subcollection for answers
                .add(answerData)
                .addOnSuccessListener { documentReference ->
                    Log.d("ForumViewModel", "Answer added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("ForumViewModel", "Error adding answer", e)
                }
        }
    }
}
