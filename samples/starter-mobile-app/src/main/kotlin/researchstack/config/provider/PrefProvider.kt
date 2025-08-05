package researchstack.config.provider

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import researchstack.data.datasource.local.pref.ComplianceReminderPref
import researchstack.data.datasource.local.pref.dataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrefProvider {
    @Singleton
    @Provides
    fun provideComplianceReminderPref(@ApplicationContext context: Context): ComplianceReminderPref =
        ComplianceReminderPref(context.dataStore)
}
