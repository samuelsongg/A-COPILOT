package com.mobilesec.govcomm.ui.screens.menu

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobilesec.govcomm.repo.FirebaseUtil
import com.mobilesec.govcomm.repo.GovCommRepository
import kotlinx.coroutines.launch

class MenuViewModel(private val repository: GovCommRepository) : ViewModel() {
    // Function to get user details from Firestore.
    fun getUserDetails(userName: String, onUserDetailsReceived: (String, String, String) -> Unit) {
        val docRef = FirebaseUtil.db.collection("userDetails").document(userName)

        docRef.get()
            .addOnSuccessListener { result ->
                val retrievedFirstName = result.getString("firstName") ?: ""
                val retrievedLastName = result.getString("lastName") ?: ""
                val retrievedRole = result.getString("role") ?: ""

                // Call the callback with the retrieved firstName and lastName
                onUserDetailsReceived(retrievedFirstName, retrievedLastName, retrievedRole)

                Log.d(ContentValues.TAG, "$userName => ${result.data}")
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents.", exception)
            }
    }
}

class MenuViewModelFactory(private val repository: GovCommRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}