package researchstack.config.provider

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import researchstack.backend.integration.GrpcHealthDataSynchronizer
import researchstack.data.datasource.healthConnect.HealthConnectDataSource
import researchstack.data.datasource.local.room.dao.ShareAgreementDao
import researchstack.data.datasource.local.room.dao.StudyDao
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.repository.healthConnect.HealthConnectDataSyncRepositoryImpl
import researchstack.domain.model.shealth.HealthDataModel
import researchstack.domain.repository.ShareAgreementRepository
import researchstack.domain.repository.StudyRepository
import researchstack.domain.repository.healthConnect.HealthConnectDataSyncRepository
import researchstack.domain.usecase.file.UploadFileUseCase
import researchstack.domain.usecase.profile.GetProfileUseCase
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.pref.dataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HealthConnectProvider {
    @Singleton
    @Provides
    fun provideHealthConnectDataSource(@ApplicationContext context: Context): HealthConnectDataSource =
        HealthConnectDataSource(HealthConnectClient.getOrCreate(context),HealthDataService.getStore(context))

    @Provides
    @Singleton
    fun provideHealthDataStore(@ApplicationContext context: Context): HealthDataStore {
        return HealthDataService.getStore(context)
    }

    @Singleton
    @Provides
    fun provideHealthConnectDataSyncRepository(
        @ApplicationContext context: Context,
        healthConnectDataSource: HealthConnectDataSource,
        shareAgreementDao: ShareAgreementDao,
        studyRepository: StudyRepository,
        shareAgreementRepository: ShareAgreementRepository,
        uploadFileUseCase: UploadFileUseCase,
        getProfileUseCase: GetProfileUseCase,
        studyDao: StudyDao,
        exerciseDao: ExerciseDao,
        grpcHealthDataSynchronizer: GrpcHealthDataSynchronizer<HealthDataModel>
    ): HealthConnectDataSyncRepository = HealthConnectDataSyncRepositoryImpl(
        context,
        healthConnectDataSource,
        shareAgreementDao,
        studyRepository,
        shareAgreementRepository,
        uploadFileUseCase,
        getProfileUseCase,
        studyDao,
        exerciseDao,
        EnrollmentDatePref(context.dataStore),
        grpcHealthDataSynchronizer
    )
}
