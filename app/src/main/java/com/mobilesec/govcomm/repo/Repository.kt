package com.mobilesec.govcomm.repo

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import com.mobilesec.govcomm.ui.screens.forum.Answer
import com.mobilesec.govcomm.ui.screens.teamchat.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Locale

// Utility object for Firebase services.
object FirebaseUtil {
    // Lazy-initialized FirebaseAuth instance.
    val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    // Lazy-initialized FirebaseFirestore instance.
    val db: FirebaseFirestore by lazy {
        Firebase.firestore
    }
    val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
}

// Repository class for handling data operations related to GovComm application.
class GovCommRepository(private val dataStore: DataStore<Preferences>) {
    // Stores the user's username in DataStore preferences.
    suspend fun setUserName( userName: String) {
        dataStore.edit{ preferences ->
            preferences[stringPreferencesKey("UserName")] = userName
        }
    }

    // Clears the stored username from DataStore preferences.
    suspend fun clearUserName() {
        dataStore.edit{ preferences ->
            preferences.remove(stringPreferencesKey("UserName"))
        }
    }

    // Retrieves the DataStore preferences as a Flow.
    fun readPreference():Flow<Preferences> {
        return dataStore.data
    }
}
