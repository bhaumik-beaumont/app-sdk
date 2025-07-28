package researchstack.config.provider

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import researchstack.backend.integration.GrpcHealthDataSynchronizer
import researchstack.data.datasource.healthConnect.HealthConnectDataSource
import researchstack.data.datasource.local.room.dao.ShareAgreementDao
import researchstack.data.datasource.local.room.dao.StudyDao
import researchstack.data.repository.healthConnect.HealthConnectDataSyncRepositoryImpl
import researchstack.domain.model.shealth.HealthDataModel
import researchstack.domain.repository.ShareAgreementRepository
import researchstack.domain.repository.StudyRepository
import researchstack.domain.repository.healthConnect.HealthConnectDataSyncRepository
import researchstack.domain.usecase.file.UploadFileUseCase
import researchstack.domain.usecase.profile.GetProfileUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HealthConnectProvider {
    @Singleton
    @Provides
    fun provideHealthConnectDataSource(@ApplicationContext context: Context): HealthConnectDataSource =
        HealthConnectDataSource(HealthConnectClient.getOrCreate(context))

    @Singleton
    @Provides
    fun provideHealthConnectDataSyncRepository(
        healthConnectDataSource: HealthConnectDataSource,
        shareAgreementDao: ShareAgreementDao,
        studyRepository: StudyRepository,
        shareAgreementRepository: ShareAgreementRepository,
        uploadFileUseCase: UploadFileUseCase,
        getProfileUseCase: GetProfileUseCase,
        studyDao: StudyDao,
        grpcHealthDataSynchronizer: GrpcHealthDataSynchronizer<HealthDataModel>
    ): HealthConnectDataSyncRepository = HealthConnectDataSyncRepositoryImpl(
        healthConnectDataSource, shareAgreementDao, studyRepository,
        shareAgreementRepository, uploadFileUseCase, getProfileUseCase,studyDao,grpcHealthDataSynchronizer
    )
}
