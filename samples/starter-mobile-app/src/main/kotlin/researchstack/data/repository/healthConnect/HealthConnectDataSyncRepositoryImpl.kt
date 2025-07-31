package researchstack.data.repository.healthConnect

import android.content.Context
import android.util.Log
import kotlin.random.Random
import researchstack.util.NotificationUtil
import researchstack.R
import androidx.health.connect.client.records.ExerciseSessionRecord
import kotlinx.coroutines.flow.first
import researchstack.backend.integration.GrpcHealthDataSynchronizer
import researchstack.data.datasource.healthConnect.HealthConnectDataSource
import researchstack.data.datasource.healthConnect.processExerciseData
import researchstack.data.datasource.local.room.dao.ShareAgreementDao
import researchstack.data.datasource.local.room.dao.StudyDao
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.domain.model.Study
import researchstack.domain.model.TimestampMapData
import researchstack.domain.model.healthConnect.Exercise
import researchstack.domain.model.log.DataSyncLog
import researchstack.domain.model.shealth.HealthDataModel
import researchstack.domain.model.shealth.SHealthDataType
import researchstack.domain.repository.ShareAgreementRepository
import researchstack.domain.repository.StudyRepository
import researchstack.domain.repository.healthConnect.HealthConnectDataSyncRepository
import researchstack.domain.usecase.file.UploadFileUseCase
import researchstack.domain.usecase.log.AppLogger
import researchstack.domain.usecase.profile.GetProfileUseCase
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.presentation.util.toStringResourceId
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HealthConnectDataSyncRepositoryImpl @Inject constructor(
    private val context: Context,
    private val healthConnectDataSource: HealthConnectDataSource,
    private val shareAgreementDao: ShareAgreementDao,
    private val studyRepository: StudyRepository,
    private val shareAgreementRepository: ShareAgreementRepository,
    private val uploadFileUseCase: UploadFileUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val studyDao: StudyDao,
    private val exerciseDao: ExerciseDao,
    private val enrollmentDatePref: EnrollmentDatePref,
    private val grpcHealthDataSynchronizer: GrpcHealthDataSynchronizer<HealthDataModel>
) : HealthConnectDataSyncRepository {
    override suspend fun syncHealthData() {
        AppLogger.saveLog(DataSyncLog("start syncHealthData"))
        getProfileUseCase().onSuccess { profile ->
            AppLogger.saveLog(DataSyncLog("profile loaded ${'$'}{profile.id}"))
            getRequiredHealthDataTypes().forEach { dataType ->
                AppLogger.saveLog(DataSyncLog("start sync for ${'$'}{dataType.name}"))
                val result: List<TimestampMapData>? = when (dataType) {
                    SHealthDataType.EXERCISE -> {

                        val exerciseRecords = healthConnectDataSource.getData(
                            ExerciseSessionRecord::class
                        ).filterIsInstance<ExerciseSessionRecord>()
                        AppLogger.saveLog(
                            DataSyncLog("fetched ${'$'}{exerciseRecords.size} exercise records")
                        )

                        val studyId = studyRepository.getActiveStudies().first().firstOrNull()?.id ?: ""
                        val enrollmentMillis = enrollmentDatePref.getEnrollmentDate(studyId)?.let { dateString ->
                            LocalDate.parse(dateString)
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        }

                        val items = mutableListOf<Exercise>()
                        exerciseRecords.forEach { record ->
                            AppLogger.saveLog(
                                DataSyncLog("processing exercise ${'$'}{record.metadata.id}")
                            )
                            val recordStartTime = record.startTime.toEpochMilli()
                            if (enrollmentMillis != null && recordStartTime < enrollmentMillis) {
                                Log.d(TAG, "Ignore exercise ${'$'}{record.metadata.id} before enrollment date")
                                AppLogger.saveLog(
                                    DataSyncLog("ignore exercise ${'$'}{record.metadata.id} before enrollment")
                                )
                            } else {
                                val sessionData = healthConnectDataSource.getAggregateData(record.metadata.id)
                                val exercise = processExerciseData(record, sessionData, studyId, enrollmentDatePref)
                                items.add(exercise)
                                AppLogger.saveLog(
                                    DataSyncLog("prepared exercise ${'$'}{record.metadata.id}")
                                )
                            }
                        }
                        exerciseDao.insertAll(*items.toTypedArray())
                        AppLogger.saveLog(
                            DataSyncLog("stored ${'$'}{items.size} exercise records in db")
                        )
                        items
                    }

                    else -> null
                }
                result?.let {
                    if (it.isEmpty()) {
                        Log.d(TAG, "No data to sync for ${'$'}{dataType.name}")
                        AppLogger.saveLog(
                            DataSyncLog("nothing to sync for ${'$'}{dataType.name}")
                        )
                    } else {
                        AppLogger.saveLog(
                            DataSyncLog("upload ${'$'}{it.size} records for ${'$'}{dataType.name}")
                        )
                        uploadDataToServer(dataType, it)
                    }
                }
            }
        }.onFailure {
            Log.e(TAG, "fail to load profile", it)
            AppLogger.saveLog(
                DataSyncLog("FAIL: load profile ${'$'}{it.stackTraceToString()}")
            )
        }

//        getRequiredHealthDataTypes().forEach { dataType ->
//            val result: List<TimestampMapData>? = when (dataType) {
//                SHealthDataType.STEPS -> processStepsData(
//                    healthConnectDataSource.getData(StepsRecord::class)
//                        .filterIsInstance<StepsRecord>()
//                )
//
//                SHealthDataType.BLOOD_GLUCOSE -> processBloodGlucoseData(
//                    healthConnectDataSource.getData(
//                        BloodGlucoseRecord::class
//                    ).filterIsInstance<BloodGlucoseRecord>()
//                )
//
//                SHealthDataType.HEART_RATE -> processHeartRateData(
//                    healthConnectDataSource.getData(
//                        HeartRateRecord::class
//                    ).filterIsInstance<HeartRateRecord>()
//                )
//
//                SHealthDataType.OXYGEN_SATURATION -> processOxygenSaturationData(
//                    healthConnectDataSource.getData(
//                        OxygenSaturationRecord::class
//                    ).filterIsInstance<OxygenSaturationRecord>()
//                )
//
//                SHealthDataType.BLOOD_PRESSURE -> processBloodPressureData(
//                    healthConnectDataSource.getData(
//                        BloodPressureRecord::class
//                    ).filterIsInstance<BloodPressureRecord>()
//                )
//
//                SHealthDataType.SLEEP_SESSION -> processSleepData(
//                    healthConnectDataSource.getData(
//                        SleepSessionRecord::class
//                    ).filterIsInstance<SleepSessionRecord>()
//                )
//                SHealthDataType.EXERCISE -> {
//
//                    val exerciseRecords =  healthConnectDataSource.getData(
//                        ExerciseSessionRecord::class
//                    ).filterIsInstance<ExerciseSessionRecord>()
//                    val items = mutableListOf<Exercise>()
//                    exerciseRecords.forEach { record ->
//                        val sessionData = healthConnectDataSource.getAggregateData(record.metadata.id)
//                        val exercise = processExerciseData(record,sessionData)
//                        items.add(exercise)
//                    }
//                    items
//                }
//                else -> null
//            }


    }

    private suspend fun uploadDataToServer(
        dataType: SHealthDataType,
        result: List<TimestampMapData>
    ) {
        AppLogger.saveLog(
            DataSyncLog("uploadDataToServer start ${dataType.name} ${result.size}")
        )
        val healthDataModel = toHealthDataModel(dataType, result)
        val approvedStudies = studyRepository.getActiveStudies().first().filter {
            shareAgreementRepository.getApprovalShareAgreementWithStudyAndDataType(
                it.id,
                dataType.name
            )
        }
        AppLogger.saveLog(
            DataSyncLog("approved studies ${approvedStudies.size} for ${dataType.name}")
        )
        approvedStudies.forEach { study ->
            AppLogger.saveLog(
                DataSyncLog("upload to study ${'$'}{study.id} ${dataType.name}")
            )
            grpcHealthDataSynchronizer.syncHealthData(
                listOf(study.id),
                healthDataModel
            ).onSuccess {
                Log.i(TAG, "success to upload data: $dataType")
                AppLogger.saveLog(
                    DataSyncLog("sync $dataType ${result.size} for ${'$'}{study.id}")
                )
                NotificationUtil.initialize(context)
                val dataTypeName = context.getString(dataType.toStringResourceId())
                val message = context.getString(R.string.sync_success_with_type, dataTypeName)
                NotificationUtil.getInstance().notify(
                    NotificationUtil.SYNC_NOTIFICATION,
                    Random.nextInt(),
                    System.currentTimeMillis(),
                    context.getString(R.string.app_name),
                    message
                )
            }.onFailure {
                Log.e(TAG, "fail to upload data to server")
                Log.e(TAG, it.stackTraceToString())
                AppLogger.saveLog(
                    DataSyncLog(
                        "FAIL: sync data $dataType for ${'$'}{study.id} ${'$'}{it.stackTraceToString()}"
                    )
                )
            }.getOrThrow()
        }
    }

    private fun <T : TimestampMapData> toHealthDataModel(dataType: SHealthDataType, data: List<T>): HealthDataModel {
        return HealthDataModel(dataType, data.map { it.toDataMap() })
    }

    private suspend fun getRequiredHealthDataTypes(): Set<SHealthDataType> {
        AppLogger.saveLog(DataSyncLog("start getRequiredHealthDataTypes"))
        val activeStudies = studyDao.getActiveStudies().first()
        AppLogger.saveLog(DataSyncLog("active studies ${activeStudies.size}"))
        val types = activeStudies.flatMap { (id) ->
            AppLogger.saveLog(DataSyncLog("load agreements for ${'$'}id"))
            shareAgreementDao.getAgreedShareAgreement(id).first().map { agreement ->
                runCatching { SHealthDataType.valueOf(agreement.dataType) }
                    .onSuccess { type ->
                        AppLogger.saveLog(
                            DataSyncLog("study ${'$'}id requires ${'$'}type")
                        )
                    }
                    .onFailure {
                        AppLogger.saveLog(
                            DataSyncLog("unknown data type ${'$'}{agreement.dataType}")
                        )
                    }
                    .getOrNull()
            }.filterNotNull()
        }.toSet()
        AppLogger.saveLog(DataSyncLog("required types ${'$'}{types.joinToString()}"))
        return types
    }

    companion object {
        private val TAG = HealthConnectDataSyncRepositoryImpl::class.simpleName
    }

    private fun getFilePath(
        study: Study,
        dayStartTime: Long,
        dayEndTime: Long,
        dataType: String,
        dataSourceSdk: String = "mobile"
    ): String {
        val directory = "${study.registrationId}/$dataSourceSdk/$dataType"
        val fileName = "${study.id}-${study.registrationId}-$dayStartTime-$dayEndTime-$dataType.csv"
        return "$directory/$fileName"
    }
}
