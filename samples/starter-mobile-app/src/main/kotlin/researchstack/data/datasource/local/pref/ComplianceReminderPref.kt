package researchstack.data.datasource.local.pref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull

class ComplianceReminderPref(private val dataStore: DataStore<Preferences>) {
    enum class Type { ACTIVITY, RESISTANCE, BIA }

    private val activityKey = stringPreferencesKey("compliance_activity")
    private val resistanceKey = stringPreferencesKey("compliance_resistance")
    private val biaKey = stringPreferencesKey("compliance_bia")

    private fun key(type: Type) = when (type) {
        Type.ACTIVITY -> activityKey
        Type.RESISTANCE -> resistanceKey
        Type.BIA -> biaKey
    }

    suspend fun getLastReminderDate(type: Type): String? =
        dataStore.data.firstOrNull()?.let { it[key(type)] }

    suspend fun saveReminderDate(type: Type, date: String) {
        dataStore.edit { prefs ->
            prefs[key(type)] = date
        }
    }

    suspend fun clear(type: Type) {
        dataStore.edit { prefs ->
            prefs.remove(key(type))
        }
    }
}
