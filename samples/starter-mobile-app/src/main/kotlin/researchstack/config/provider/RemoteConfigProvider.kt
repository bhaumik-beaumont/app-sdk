package researchstack.config.provider

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import researchstack.BuildConfig
import researchstack.data.remoteconfig.REMOTE_CONFIG_LATEST_APP_VERSION_KEY

@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigProvider {
    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        val remoteConfig = Firebase.remoteConfig
        val fetchInterval = if (BuildConfig.DEBUG) 0L else 6 * 60 * 60L
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = fetchInterval
            }
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                REMOTE_CONFIG_LATEST_APP_VERSION_KEY to BuildConfig.VERSION_NAME,
            )
        )
        return remoteConfig
    }
}
