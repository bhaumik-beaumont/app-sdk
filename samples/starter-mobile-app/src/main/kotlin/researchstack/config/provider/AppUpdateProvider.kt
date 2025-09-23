package researchstack.config.provider

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import researchstack.data.repository.AppUpdateRepositoryImpl
import researchstack.domain.repository.AppUpdateRepository

@Module
@InstallIn(SingletonComponent::class)
object AppUpdateProvider {
    @Provides
    @Singleton
    fun provideAppUpdateRepository(
        firebaseRemoteConfig: FirebaseRemoteConfig,
    ): AppUpdateRepository = AppUpdateRepositoryImpl(firebaseRemoteConfig)
}
