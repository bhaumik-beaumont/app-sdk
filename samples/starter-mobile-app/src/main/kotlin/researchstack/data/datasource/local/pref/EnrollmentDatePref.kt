package researchstack.data.datasource.local.pref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import researchstack.util.logDataSync

private const val TAG = "EnrollmentDatePref"

class EnrollmentDatePref(private val dataStore: DataStore<Preferences>) {
    suspend fun saveEnrollmentDate(studyId: String, date: String) {
        logDataSync("Saving enrollment date for studyId=$studyId with date=$date", tag = TAG)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(studyId)] = date
        }
        logDataSync("Enrollment date saved for studyId=$studyId", tag = TAG)
    }

    suspend fun getEnrollmentDate(studyId: String): String? {
        logDataSync("Fetching enrollment date for studyId=$studyId", tag = TAG)
        val date = dataStore.data.firstOrNull()?.let { pref ->
            pref[stringPreferencesKey(studyId)]
        }
        logDataSync("Enrollment date lookup for studyId=$studyId returned ${date ?: "none"}", tag = TAG)
        return date
    }
}
