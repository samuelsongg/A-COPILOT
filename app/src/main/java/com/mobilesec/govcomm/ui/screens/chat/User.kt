package com.mobilesec.govcomm.ui.screens.chat

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class User {
    companion object {
        fun fetchUserNameByEmail(email: String, onResult: (String) -> Unit) {
            val db = FirebaseFirestore.getInstance()
            db.collection("userDetails").document(email)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"
                        onResult(fullName.trim())
                    } else {
                        onResult("Unknown")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("User", "Error fetching user name: ", exception)
                    onResult("Unknown")
                }
        }
    }
}
