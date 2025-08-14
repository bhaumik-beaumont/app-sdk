package researchstack.data.datasource.local.pref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull

class EnrollmentDatePref(private val dataStore: DataStore<Preferences>) {
    suspend fun saveEnrollmentDate(studyId: String, date: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(studyId)] = date
        }
    }

    suspend fun getEnrollmentDate(studyId: String): String? =
        dataStore.data.firstOrNull()?.let { pref ->
//            "2025-07-01"
            pref[stringPreferencesKey(studyId)]
        }
}
