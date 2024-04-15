package com.mobilesec.govcomm.ui.screens.login

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobilesec.govcomm.repo.FirebaseUtil
import com.mobilesec.govcomm.repo.GovCommRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// This class handles the logic for the login screen
class LoginViewModel(private val repository: GovCommRepository) : ViewModel() {

}

// This class helps create LoginViewModel with a repository.
// Needed to initialize view model for the screen
class LoginViewModelFactory(private val repository: GovCommRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}