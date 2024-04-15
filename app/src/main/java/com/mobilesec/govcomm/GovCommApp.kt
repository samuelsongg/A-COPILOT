package com.mobilesec.govcomm

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.mobilesec.govcomm.repo.GovCommRepository

class GovCommApp : Application() {

    val Context.dataStore by preferencesDataStore(
        name = "GovCommPreference"
    )

    val GovCommRepository by lazy { GovCommRepository(dataStore)}

}