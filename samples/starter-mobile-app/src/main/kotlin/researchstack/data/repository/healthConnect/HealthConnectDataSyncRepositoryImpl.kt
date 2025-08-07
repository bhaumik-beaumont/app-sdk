package researchstack.data.repository.healthConnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.records.ExerciseSessionRecord
import kotlinx.coroutines.flow.first
import researchstack.R
import researchstack.backend.integration.GrpcHealthDataSynchronizer
import researchstack.data.datasource.healthConnect.HealthConnectDataSource
import researchstack.data.datasource.healthConnect.processExerciseData
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.datasource.local.room.dao.ExerciseDao
import researchstack.data.datasource.local.room.dao.ComplianceEntryDao
import researchstack.data.datasource.local.room.dao.ShareAgreementDao
import researchstack.data.datasource.local.room.dao.StudyDao
import researchstack.data.local.room.dao.BiaDao
import researchstack.data.local.room.dao.UserProfileDao
import researchstack.domain.model.Study
import researchstack.domain.model.TimestampMapData
import researchstack.domain.model.healthConnect.Exercise
import researchstack.domain.model.ComplianceEntry
import researchstack.domain.model.shealth.HealthDataModel
import researchstack.domain.model.shealth.SHealthDataType
import researchstack.domain.repository.ShareAgreementRepository
import researchstack.domain.repository.StudyRepository
import researchstack.domain.repository.healthConnect.HealthConnectDataSyncRepository
import researchstack.domain.usecase.file.UploadFileUseCase
import researchstack.domain.usecase.profile.GetProfileUseCase
import researchstack.presentation.util.toStringResourceId
import researchstack.util.NotificationUtil
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

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
    private val complianceEntryDao: ComplianceEntryDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
    private val enrollmentDatePref: EnrollmentDatePref,
    private val grpcHealthDataSynchronizer: GrpcHealthDataSynchronizer<HealthDataModel>
) : HealthConnectDataSyncRepository {
    override suspend fun syncHealthData() {
        getProfileUseCase().onSuccess { profile ->
            getRequiredHealthDataTypes().forEach { dataType ->
                val result: List<TimestampMapData>? = when (dataType) {
                    SHealthDataType.EXERCISE -> {
                        val exerciseRecords = healthConnectDataSource.getData(
                            ExerciseSessionRecord::class
                        ).filterIsInstance<ExerciseSessionRecord>()
                        val samsungRecords = healthConnectDataSource.getExerciseData()
                        val studyId = studyRepository.getActiveStudies().first().firstOrNull()?.id ?: ""
                        val enrollmentMillis = enrollmentDatePref.getEnrollmentDate(studyId)?.let { dateString ->
                            LocalDate.parse(dateString)
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        }
                        val items = mutableListOf<Exercise>()
                        exerciseRecords.forEach { record ->
                            val recordStartTime = record.startTime.toEpochMilli()
                            if (enrollmentMillis != null && recordStartTime < enrollmentMillis) {
                                Log.d(TAG, "Ignore exercise ${record.metadata.id} before enrollment date")
                            } else {
                                val sessionData = healthConnectDataSource.getAggregateData(record.metadata.id)
                                val matchingSamsungRecord = samsungRecords.firstOrNull { samsung ->
                                    samsung.startTime.toEpochMilli() == record.startTime.toEpochMilli() &&
                                        samsung.endTime?.toEpochMilli() == record.endTime.toEpochMilli()
                                }
                                val exercise = processExerciseData(record, sessionData, studyId, enrollmentDatePref,matchingSamsungRecord)
                                items.add(exercise)
                            }
                        }
                        exerciseDao.insertAll(*items.toTypedArray())
                        items
                    }

                    else -> null
                }
                result?.let {
                    if (it.isEmpty()) {
                        Log.d(TAG, "No data to sync for ${dataType.name}")
                    } else {
                        uploadDataToServer(dataType, it)
                    }
                }
            }
        }.onFailure {
            Log.e(TAG, "fail to load profile", it)
        }
        val entries = generateWeeklyCompliance()
        complianceEntryDao.clear()
        complianceEntryDao.insertAll(*entries.toTypedArray())
        if (entries.isNotEmpty()) {
            uploadDataToServer(SHealthDataType.USER_COMPLIANCE, entries)
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
        val healthDataModel = toHealthDataModel(dataType, result)
        val approvedStudies = studyRepository.getActiveStudies().first().filter {
            shareAgreementRepository.getApprovalShareAgreementWithStudyAndDataType(
                it.id,
                dataType.name
            ) || dataType == SHealthDataType.USER_COMPLIANCE
        }
        approvedStudies.forEach { study ->
            grpcHealthDataSynchronizer.syncHealthData(
                listOf(study.id),
                healthDataModel
            ).onSuccess {
                Log.i(TAG, "success to upload data: $dataType")
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
            }.getOrThrow()
        }
    }

    private fun <T : TimestampMapData> toHealthDataModel(dataType: SHealthDataType, data: List<T>): HealthDataModel {
        return HealthDataModel(dataType, data.map { it.toDataMap() })
    }

    private suspend fun getRequiredHealthDataTypes(): Set<SHealthDataType> {
        val activeStudies = studyDao.getActiveStudies().first()
        val types = activeStudies.flatMap { (id) ->
            shareAgreementDao.getAgreedShareAgreement(id).first().mapNotNull { agreement ->
                runCatching { SHealthDataType.valueOf(agreement.dataType) }
                    .onSuccess { type ->
                    }
                    .onFailure {
                    }
                    .getOrNull()
            }
        }.toSet()
        return types
    }

    private suspend fun generateWeeklyCompliance(): List<ComplianceEntry> {
        val studyId = studyRepository.getActiveStudies().first().firstOrNull()?.id ?: return emptyList()
        val enrollmentDateStr = enrollmentDatePref.getEnrollmentDate(studyId) ?: return emptyList()
        val enrollmentDate = LocalDate.parse(enrollmentDateStr)
        val today = LocalDate.now()
        val entries = mutableListOf<ComplianceEntry>()
        var weekStart = enrollmentDate
        var weekNumber = 1
        while (!weekStart.isAfter(today)) {
            val weekEnd = weekStart.plusDays(6)
            val periodEnd = if (weekEnd.isAfter(today)) today else weekEnd
            val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = periodEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            val exercises = exerciseDao.getExercisesBetween(startMillis, endMillis).first()
            val activityMinutes = TimeUnit.MILLISECONDS.toMinutes(
                exercises.filter { !it.isResistance }.sumOf { it.duration }
            ).toInt()
            val resistanceCount = exercises.count { it.isResistance }
            val biaCount = biaDao.countBetween(startMillis, endMillis).first()
            val weightCount = userProfileDao.countBetween(startMillis, endMillis).first()
            entries.add(
                ComplianceEntry(
                    weekNumber = weekNumber,
                    timestamp = endMillis,
                    startDate = weekStart.toString(),
                    endDate = periodEnd.toString(),
                    totalActivityMinutes = activityMinutes,
                    resistanceSessionCount = resistanceCount,
                    biaRecordCount = biaCount,
                    weightRecordCount = weightCount,
                )
            )
            weekStart = weekStart.plusWeeks(1)
            weekNumber++
        }
        return entries
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
