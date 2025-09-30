package researchstack.data.repository.healthConnect

import android.content.Context
import android.util.Log
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.request.DataType
import kotlinx.coroutines.flow.first
import researchstack.R
import researchstack.auth.data.datasource.local.pref.BasicAuthenticationPref
import researchstack.auth.data.datasource.local.pref.dataStore
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
import researchstack.domain.model.log.DataSyncLog
import researchstack.domain.repository.ShareAgreementRepository
import researchstack.domain.repository.StudyRepository
import researchstack.domain.repository.healthConnect.HealthConnectDataSyncRepository
import researchstack.domain.usecase.profile.GetProfileUseCase
import researchstack.domain.usecase.profile.UpdateProfileUseCase
import researchstack.domain.usecase.log.AppLogger
import researchstack.presentation.util.toDecimalFormat
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
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val studyDao: StudyDao,
    private val exerciseDao: ExerciseDao,
    private val complianceEntryDao: ComplianceEntryDao,
    private val biaDao: BiaDao,
    private val userProfileDao: UserProfileDao,
    private val enrollmentDatePref: EnrollmentDatePref,
    private val grpcHealthDataSynchronizer: GrpcHealthDataSynchronizer<HealthDataModel>
) : HealthConnectDataSyncRepository {
    private val authenticationPref = BasicAuthenticationPref(context.dataStore)

    override suspend fun syncHealthData() {
        logDataSync("Starting health data synchronization flow")
        val activeStudies = studyRepository.getActiveStudies().first()
        val studyId = activeStudies.firstOrNull()?.id ?: ""
        logDataSync(
            "Loaded ${activeStudies.size} active studies; using studyId='$studyId' for enrollment checks"
        )

        val profileResult = getProfileUseCase()
        if (profileResult.isSuccess) {
            val profile = profileResult.getOrNull()
            if (profile != null) {
                logDataSync("Successfully retrieved profile data for synchronization")
                val wearableProfile = userProfileDao.getLatest().first()
                logDataSync(
                    "Wearable profile lookup completed; has wearable profile=${wearableProfile != null}"
                )
                val updatedProfileResult = updateProfileUseCase(
                    profile.copy(gender = wearableProfile?.gender?.ordinal ?: 2)
                )
                if (updatedProfileResult.isSuccess) {
                    logDataSync("Updated profile gender information based on wearable data")
                } else {
                    val updateError = updatedProfileResult.exceptionOrNull()
                    logDataSync(
                        "Failed to update profile gender: ${updateError?.message ?: "unknown error"}",
                        updateError
                    )
                    updateError?.let { Log.e(TAG, "Failed to update profile gender", it) }
                }

                val enrollmentDate = profile.enrolmentDate ?: LocalDate.now().toString()
                enrollmentDatePref.saveEnrollmentDate(studyId, enrollmentDate)
                logDataSync("Enrollment date '$enrollmentDate' saved for studyId '$studyId'")

                val requiredHealthDataTypes = getRequiredHealthDataTypes()
                logDataSync(
                    "Required health data types resolved: ${
                        if (requiredHealthDataTypes.isEmpty()) "none" else requiredHealthDataTypes.joinToString { it.name }
                    }"
                )
                for (dataType in requiredHealthDataTypes) {
                    logDataSync("Processing data type ${dataType.name}")
                    val result: List<TimestampMapData>? = when (dataType) {
                        SHealthDataType.EXERCISE -> {
                            val samsungRecords = healthConnectDataSource.getExerciseData()
                            logDataSync("Fetched ${samsungRecords.size} Samsung Health exercise records")

                            val enrollmentMillis = enrollmentDatePref.getEnrollmentDate(studyId)?.let { dateString ->
                                LocalDate.parse(dateString)
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                            }
                            logDataSync(
                                "Enrollment threshold millis for exercise filtering: ${enrollmentMillis ?: "none"}"
                            )

                            val items = mutableListOf<Exercise>()
                            samsungRecords.forEachIndexed { index, record ->
                                val sessions = runCatching {
                                    record.getValue(DataType.ExerciseType.SESSIONS)
                                }.getOrElse { throwable ->
                                    logDataSync(
                                        "Failed to read sessions from Samsung exercise record ${record.uid ?: index.toString()}: ${throwable.message}",
                                        throwable
                                    )
                                    emptyList<ExerciseSession>()
                                }
                                if (sessions?.isEmpty() == true) {
                                    logDataSync("Samsung exercise record ${record.uid} contains no sessions")
                                }
                                sessions?.forEachIndexed { sessionIndex, session ->
                                    val sessionStartTime = session.startTime.toEpochMilli()
                                    if (enrollmentMillis != null && sessionStartTime < enrollmentMillis) {
                                        Log.d(TAG, "Ignore Samsung exercise ${record.uid ?: "unknown"} session $sessionIndex before enrollment date")
                                        logDataSync(
                                            "Skipping Samsung exercise ${record.uid ?: "unknown"} session $sessionIndex before enrollment threshold"
                                        )
                                    } else {
                                        logDataSync(
                                            "Processing Samsung exercise ${record.uid ?: "unknown"} session $sessionIndex from ${session.startTime} to ${session.endTime}"
                                        )
                                        val exercise = processExerciseData(
                                            record,
                                            session,
                                            studyId,
                                            enrollmentDatePref,
                                            sessionIndex
                                        )
                                        logDataSync(
                                            "Processed exercise ${exercise.id} with duration=${exercise.duration} and distance=${exercise.distance}"
                                        )
                                        items.add(exercise)
                                    }
                                }
                            }
                            exerciseDao.insertAll(*items.toTypedArray())
                            logDataSync("Inserted ${items.size} exercise entries into local storage")
                            items
                        }

                        else -> {
                            logDataSync("No processing implemented for data type ${dataType.name}")
                            null
                        }
                    }
                    if (result == null) {
                        continue
                    }
                    if (result.isEmpty()) {
                        Log.d(TAG, "No data to sync for ${dataType.name}")
                        logDataSync("No data to sync for ${dataType.name}")
                    } else {
                        logDataSync("Uploading ${result.size} records for ${dataType.name}")
                        uploadDataToServer(dataType, result)
                    }
                }
            } else {
                logDataSync("Profile result returned null data")
            }
        } else {
            val throwable = profileResult.exceptionOrNull()
            logDataSync(
                "Failed to load profile: ${throwable?.message ?: "unknown error"}",
                throwable
            )
            Log.e(TAG, "fail to load profile", throwable)
        }
        val entries = generateWeeklyCompliance()
        logDataSync("Weekly compliance generation produced ${entries.size} entries")
        complianceEntryDao.clear()
        logDataSync("Cleared compliance entries prior to insertion")
        complianceEntryDao.insertAll(*entries.toTypedArray())
        logDataSync("Inserted ${entries.size} compliance entries into database")
        if (entries.isNotEmpty()) {
            logDataSync("Uploading compliance data entries to server")
            uploadDataToServer(SHealthDataType.USER_COMPLIANCE, entries)
        } else {
            logDataSync("No compliance entries to upload to server")
        }
        logDataSync("Completed health data synchronization flow")
    }

    private suspend fun uploadDataToServer(
        dataType: SHealthDataType,
        result: List<TimestampMapData>
    ) {
        logDataSync("Preparing to upload ${result.size} ${dataType.name} records to server")
        val healthDataModel = toHealthDataModel(dataType, result)
        logDataSync(
            "Converted ${result.size} ${dataType.name} records into HealthDataModel with ${healthDataModel.dataList.size} entries"
        )
        val activeStudies = studyRepository.getActiveStudies().first()
        logDataSync(
            "Found ${activeStudies.size} active studies during ${dataType.name} upload preparation"
        )
        val approvedStudies = activeStudies.filter {
            shareAgreementRepository.getApprovalShareAgreementWithStudyAndDataType(
                it.id,
                dataType.name
            ) || dataType == SHealthDataType.USER_COMPLIANCE
        }
        logDataSync(
            "Approved studies for ${dataType.name}: ${
                if (approvedStudies.isEmpty()) "none" else approvedStudies.joinToString { it.id }
            }"
        )
        for (study in approvedStudies) {
            logDataSync("Uploading ${dataType.name} data for study ${study.id}")
            val syncResult = grpcHealthDataSynchronizer.syncHealthData(
                listOf(study.id),
                healthDataModel
            )
            if (syncResult.isSuccess) {
                Log.i(TAG, "success to upload data: $dataType")
                logDataSync(
                    "Successfully uploaded ${dataType.name} for study ${study.id}"
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
                logDataSync(
                    "Triggered sync success notification for study ${study.id} and data type ${dataType.name}"
                )
            } else {
                val throwable = syncResult.exceptionOrNull()
                Log.e(TAG, "fail to upload data to server")
                throwable?.let { Log.e(TAG, it.stackTraceToString()) }
                logDataSync(
                    "Failed to upload ${dataType.name} for study ${study.id}: ${throwable?.message ?: "unknown error"}",
                    throwable
                )
            }
            syncResult.getOrThrow()
        }
        if (approvedStudies.isEmpty()) {
            logDataSync("No approved studies available for ${dataType.name}; skipping upload")
        }
    }

    private suspend fun <T : TimestampMapData> toHealthDataModel(
        dataType: SHealthDataType,
        data: List<T>
    ): HealthDataModel {
        logDataSync("Transforming ${data.size} records into HealthDataModel for ${dataType.name}")
        val mappedData = data.map { it.toDataMap() }
        logDataSync(
            "Transformation complete for ${dataType.name}; payload size=${mappedData.size}"
        )
        return HealthDataModel(dataType, mappedData)
    }

    private suspend fun getRequiredHealthDataTypes(): Set<SHealthDataType> {
        logDataSync("Fetching required health data types from share agreements")
        val activeStudies = studyDao.getActiveStudies().first()
        logDataSync("Share agreement evaluation triggered for ${activeStudies.size} active studies")
        val types = mutableSetOf<SHealthDataType>()
        for (study in activeStudies) {
            val studyId = study.id
            logDataSync("Evaluating share agreements for study $studyId")
            val agreements = shareAgreementDao.getAgreedShareAgreement(studyId).first()
            logDataSync("Study $studyId has ${agreements.size} share agreements")
            for (agreement in agreements) {
                val typeResult = runCatching { SHealthDataType.valueOf(agreement.dataType) }
                if (typeResult.isSuccess) {
                    val type = typeResult.getOrNull()
                    if (type != null) {
                        logDataSync("Agreement for study $studyId allows data type ${type.name}")
                        types.add(type)
                    }
                } else {
                    logDataSync("Failed to parse data type '${agreement.dataType}' for study $studyId")
                }
            }
        }
        logDataSync(
            "Calculated required data types set: ${
                if (types.isEmpty()) "none" else types.joinToString { it.name }
            }"
        )
        return types
    }

    private suspend fun generateWeeklyCompliance(): List<ComplianceEntry> {
        logDataSync("Generating weekly compliance entries")
        val activeStudy = studyRepository.getActiveStudies().first().firstOrNull()
        if (activeStudy == null) {
            logDataSync("No active study found while generating weekly compliance")
            return emptyList()
        }
        val studyId = activeStudy.id
        val enrollmentDateStr = enrollmentDatePref.getEnrollmentDate(studyId)
        if (enrollmentDateStr == null) {
            logDataSync("No enrollment date stored for study $studyId; skipping compliance generation")
            return emptyList()
        }
        val enrollmentDate = LocalDate.parse(enrollmentDateStr)
        logDataSync("Enrollment date for study $studyId parsed as $enrollmentDate")
        val today = LocalDate.now()
        val entries = mutableListOf<ComplianceEntry>()
        var weekStart = enrollmentDate
        var weekNumber = 1
        while (!weekStart.isAfter(today)) {
            val weekEnd = weekStart.plusDays(6)
            val periodEnd = if (weekEnd.isAfter(today)) today else weekEnd
            val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = periodEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            logDataSync("Collecting compliance metrics for week $weekNumber between $weekStart and $periodEnd")
            val exercises = exerciseDao.getExercisesBetween(startMillis, endMillis).first()
            logDataSync("Retrieved ${exercises.size} exercises for week $weekNumber window")
            val activityMinutes = TimeUnit.MILLISECONDS.toMinutes(
                exercises.filter { !it.isResistance }.sumOf { it.duration }
            ).toInt()
            val resistanceCount = exercises.count { it.isResistance }
            val biaCount = biaDao.countBetween(startMillis, endMillis).first()
            val weightEntries = userProfileDao.getBetween(startMillis, endMillis).first()
            val weightCount = weightEntries.size
            val avgWeight = if (weightEntries.isEmpty()) 0f else weightEntries.map { it.weight.toDecimalFormat(2) }.first()
            val height = if (weightEntries.isEmpty()) 0f else weightEntries.map { it.height.toInt() }.first()
            val age = if (weightEntries.isEmpty()) 0 else {
                val yearBirth = weightEntries.map { it.yearBirth }.first()
                val calculated = today.year - yearBirth
                if (calculated < 0) 0 else calculated
            }
            val gender = if (weightEntries.isEmpty()) 2 else weightEntries.map { it.gender.ordinal }.first()
            logDataSync(
                "Week $weekNumber metrics -> activityMinutes=$activityMinutes, resistanceCount=$resistanceCount, biaCount=$biaCount, weightCount=$weightCount, avgWeight=$avgWeight, height=$height, age=$age, gender=$gender"
            )
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
                    avgWeight = avgWeight,
                    height = height.toFloat(),
                    age = age,
                    gender = gender,
                    id = authenticationPref.getAuthInfo()?.id + weekNumber.toString()
                )
            )
            logDataSync("Stored compliance entry for week $weekNumber with timestamp=$endMillis")
            weekStart = weekStart.plusWeeks(1)
            weekNumber++
        }
        logDataSync("Finished generating weekly compliance entries; total=${entries.size}")
        return entries
    }

    private suspend fun logDataSync(message: String, throwable: Throwable? = null) {
        val tag = TAG ?: "HealthConnectDataSyncRepositoryImpl"
        val fullMessage = buildString {
            append("[")
            append(tag)
            append("] ")
            append(message)
            if (throwable != null) {
                append(' ')
                append(throwable.stackTraceToString())
            }
        }
        AppLogger.saveLog(DataSyncLog(fullMessage))
    }

    companion object {
        private val TAG = HealthConnectDataSyncRepositoryImpl::class.simpleName
    }

    private suspend fun getFilePath(
        study: Study,
        dayStartTime: Long,
        dayEndTime: Long,
        dataType: String,
        dataSourceSdk: String = "mobile"
    ): String {
        val directory = "${study.registrationId}/$dataSourceSdk/$dataType"
        val fileName = "${study.id}-${study.registrationId}-$dayStartTime-$dayEndTime-$dataType.csv"
        val path = "$directory/$fileName"
        logDataSync("Generated file path for study ${study.id}, dataType=$dataType -> $path")
        return path
    }
}
