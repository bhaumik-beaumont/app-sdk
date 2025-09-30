package researchstack.data.repository.wearable

import android.util.Log
import androidx.paging.PagingSource
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import researchstack.BuildConfig
import researchstack.backend.integration.GrpcHealthDataSynchronizer
import researchstack.data.datasource.healthConnect.HealthConnectDataSource
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.data.local.room.WearableAppDataBase
import researchstack.data.local.room.dao.PrivDao
import researchstack.domain.model.Timestamp
import researchstack.domain.model.TimestampMapData
import researchstack.domain.model.log.DataSyncLog
import researchstack.domain.model.priv.Accelerometer
import researchstack.domain.model.priv.Bia
import researchstack.domain.model.priv.EcgSet
import researchstack.domain.model.priv.HeartRate
import researchstack.domain.model.priv.PpgGreen
import researchstack.domain.model.priv.PpgIr
import researchstack.domain.model.priv.PpgRed
import researchstack.domain.model.priv.PrivDataType
import researchstack.domain.model.priv.SpO2
import researchstack.domain.model.priv.SweatLoss
import researchstack.domain.model.Gender
import researchstack.domain.model.UserProfile
import researchstack.domain.model.shealth.HealthDataModel
import researchstack.domain.repository.ShareAgreementRepository
import researchstack.domain.repository.StudyRepository
import researchstack.domain.repository.WearableDataReceiverRepository
import researchstack.domain.usecase.log.AppLogger
import researchstack.util.getCurrentTimeOffset
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import com.samsung.android.sdk.health.data.data.Field
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.request.DataTypes

class WearableDataReceiverRepositoryImpl @Inject constructor(
    private val studyRepository: StudyRepository,
    private val shareAgreementRepository: ShareAgreementRepository,
    private val wearableAppDataBase: WearableAppDataBase,
    private val grpcHealthDataSynchronizer: GrpcHealthDataSynchronizer<HealthDataModel>,
    private val healthConnectDataSource: HealthConnectDataSource,
    private val enrollmentDatePref: EnrollmentDatePref,
) : WearableDataReceiverRepository {

    private val gson = Gson()

    val objectMapper = jacksonObjectMapper().apply {
        class ListDeserializer : JsonDeserializer<List<*>>(), ContextualDeserializer {
            private val objectMapper = ObjectMapper()
            private val types: MutableMap<String, JavaType> = mutableMapOf()

            override fun createContextual(
                ctxt: DeserializationContext,
                property: BeanProperty
            ): JsonDeserializer<List<*>> {
                types[property.name] = property.type
                return this
            }

            @Throws(IOException::class)
            override fun deserialize(
                jsonParser: JsonParser,
                deserializationContext: DeserializationContext
            ): List<*> {
                val node: JsonNode = jsonParser.codec.readTree(jsonParser)
                val jsonStr = node.toString().drop(1).dropLast(1).replace("\\\"", "\"")
                return objectMapper.readValue(jsonStr, types[jsonParser.parsingContext.currentName])
            }
        }

        val listModule = SimpleModule().addDeserializer(List::class.java, ListDeserializer())
        registerModule(listModule)
        registerModule(JavaTimeModule())
    }

    override fun saveWearableData(jsonObject: JsonObject) {
        val dataType = gson.fromJson(jsonObject.get("dataType"), PrivDataType::class.java)
        Log.i(TAG, "received datatype: $dataType")
        logDataSync("Received wearable JSON payload for data type $dataType")
        when (dataType) {
            PrivDataType.WEAR_ACCELEROMETER -> saveData<Accelerometer>(
                jsonObject,
                wearableAppDataBase.accelerometerDao()
            )

            PrivDataType.WEAR_BIA -> saveData<Bia>(jsonObject, wearableAppDataBase.biaDao())
            PrivDataType.WEAR_ECG -> saveData<EcgSet>(jsonObject, wearableAppDataBase.ecgDao())
            PrivDataType.WEAR_PPG_GREEN -> saveData<PpgGreen>(jsonObject, wearableAppDataBase.ppgGreenDao())
            PrivDataType.WEAR_PPG_IR -> saveData<PpgIr>(jsonObject, wearableAppDataBase.ppgIrDao())
            PrivDataType.WEAR_PPG_RED -> saveData<PpgRed>(jsonObject, wearableAppDataBase.ppgRedDao())
            PrivDataType.WEAR_SPO2 -> saveData<SpO2>(jsonObject, wearableAppDataBase.spO2Dao())
            PrivDataType.WEAR_SWEAT_LOSS -> saveData<SweatLoss>(jsonObject, wearableAppDataBase.sweatLossDao())
            PrivDataType.WEAR_HEART_RATE -> saveData<HeartRate>(jsonObject, wearableAppDataBase.heartRateDao())
            PrivDataType.WEAR_USER_PROFILE -> saveUserProfile(jsonObject)
        }
    }

    override fun saveWearableData(dataType: PrivDataType, csvInputStream: InputStream) {
        logDataSync("Received wearable CSV stream for data type $dataType")
        when (dataType) {
            PrivDataType.WEAR_ACCELEROMETER -> saveData<Accelerometer>(
                readCsv<Accelerometer>(csvInputStream),
                wearableAppDataBase.accelerometerDao()
            )

            PrivDataType.WEAR_BIA -> saveData<Bia>(readCsv<Bia>(csvInputStream), wearableAppDataBase.biaDao())
            PrivDataType.WEAR_ECG -> saveData<EcgSet>(readCsv<EcgSet>(csvInputStream), wearableAppDataBase.ecgDao())

            PrivDataType.WEAR_PPG_GREEN -> saveData<PpgGreen>(
                readCsv<PpgGreen>(csvInputStream),
                wearableAppDataBase.ppgGreenDao()
            )

            PrivDataType.WEAR_PPG_IR -> saveData<PpgIr>(readCsv<PpgIr>(csvInputStream), wearableAppDataBase.ppgIrDao())
            PrivDataType.WEAR_PPG_RED -> saveData<PpgRed>(
                readCsv<PpgRed>(csvInputStream),
                wearableAppDataBase.ppgRedDao()
            )

            PrivDataType.WEAR_SPO2 -> saveData<SpO2>(readCsv<SpO2>(csvInputStream), wearableAppDataBase.spO2Dao())
            PrivDataType.WEAR_SWEAT_LOSS -> saveData<SweatLoss>(
                readCsv<SweatLoss>(csvInputStream),
                wearableAppDataBase.sweatLossDao()
            )

            PrivDataType.WEAR_HEART_RATE -> saveData<HeartRate>(
                readCsv<HeartRate>(csvInputStream),
                wearableAppDataBase.heartRateDao()
            )

            PrivDataType.WEAR_USER_PROFILE -> saveUserProfiles(
                readCsv<UserProfile>(csvInputStream),
            )
        }
    }

    inline fun <reified T> readCsv(inputStream: InputStream): List<T> {
        logDataSync("Parsing CSV input stream into ${T::class.java.simpleName}")
        val csvMapper = CsvMapper().apply {
            enable(CsvParser.Feature.TRIM_SPACES)
            enable(CsvParser.Feature.SKIP_EMPTY_LINES)
        }

        val schema = CsvSchema.emptySchema().withHeader().withColumnSeparator('|')

        val data = csvMapper.readerFor(Map::class.java)
            .with(schema)
            .readValues<Map<String, String>>(inputStream)
            .readAll()
            .map {
                objectMapper.convertValue(it, T::class.java)
            }

        Log.i(
            WearableDataReceiverRepositoryImpl::class.simpleName,
            "data synced from wearOS: ${T::class.java.simpleName}, size: ${data.size}"
        )
        logDataSync("Parsed ${data.size} ${T::class.java.simpleName} entries from CSV stream")

        return data
    }

    private fun <T> fromJson(jsonObject: JsonObject, typeOfT: Type): T =
        gson.fromJson(
            jsonObject.get("data"),
            typeOfT
        )

    private fun <T> JsonObject.isArrayItem() =
        runCatching { fromJson<ArrayList<T>>(this, object : TypeToken<ArrayList<T>>() {}.type) }.getOrNull() != null

    private inline fun <reified T : Timestamp> saveData(jsonObject: JsonObject, privDao: PrivDao<T>) {
        logDataSync("Saving JSON wearable payload into ${T::class.java.simpleName} table")
        if (jsonObject.isArrayItem<T>()) {
            val data: List<T> = fromJson(
                jsonObject,
                object : TypeToken<ArrayList<T>>() {}.type
            )
            logDataSync("Detected array payload with ${data.size} ${T::class.java.simpleName} items")
            privDao.insertAll(data.map { ensureId(it) })
        } else {
            val data: T = fromJson(jsonObject, object : TypeToken<T>() {}.type)
            logDataSync("Detected single payload item for ${T::class.java.simpleName}")
            privDao.insert(ensureId(data))
        }
        logDataSync("Completed saving JSON payload for ${T::class.java.simpleName}")
    }

    private inline fun <reified T : Timestamp> saveData(data: List<T>, privDao: PrivDao<T>) {
        logDataSync("Saving ${data.size} ${T::class.java.simpleName} items parsed from CSV")
        privDao.insertAll(data.map { ensureId(it) })
        logDataSync("Completed CSV save for ${T::class.java.simpleName}")
    }

    private fun saveUserProfile(jsonObject: JsonObject) {
        logDataSync("Saving wearable user profile JSON payload")
        val profiles: List<UserProfile> =
            if (jsonObject.isArrayItem<UserProfile>()) {
                logDataSync("Detected array payload for user profiles")
                fromJson(jsonObject, object : TypeToken<ArrayList<UserProfile>>() {}.type)
            } else {
                logDataSync("Detected single payload item for user profile")
                listOf(fromJson(jsonObject, object : TypeToken<UserProfile>() {}.type))
            }
        saveUserProfiles(profiles)
    }

    private fun saveUserProfiles(profiles: List<UserProfile>) {
        logDataSync("Saving ${profiles.size} user profiles")
        val userProfileDao = wearableAppDataBase.userProfileDao()
        var lastWeight = runBlocking { userProfileDao.getLatest().first()?.weight }
        logDataSync("Latest stored user profile weight: ${lastWeight ?: "none"}")
        val validProfiles = profiles.filter { profile ->
            val weight = profile.weight
            if (weight <= 0f) {
                logDataSync("Skipping user profile with non-positive weight: $weight")
                return@filter false
            }
            if (lastWeight != null && weight == lastWeight) {
                logDataSync("Skipping user profile with duplicate weight: $weight")
                return@filter false
            }
            lastWeight = weight
            true
        }
        logDataSync("Filtered user profiles count: ${validProfiles.size}")
        if (validProfiles.isNotEmpty()) {
            userProfileDao.insertAll(validProfiles)
            logDataSync("Inserted ${validProfiles.size} user profiles into database")
        } else {
            logDataSync("No valid user profiles to insert")
        }
    }

    private inline fun <reified T : Timestamp> ensureId(data: T): T =
        if (data is Bia && data.id.isBlank()) {
            val newId = UUID.randomUUID().toString()
            logDataSync("Generated new UUID for BIA entry: $newId")
            data.copy(id = newId) as T
        } else {
            data
        }

    private suspend fun importBiaFromSamsungHealth(studyId: String?) {
        logDataSync("Importing BIA data from Samsung Health for studyId=$studyId")
        val biaDataPoints = runCatching { healthConnectDataSource.getBiaData() }
            .onFailure { Log.e(TAG, "Failed to load BIA data from Samsung Health", it) }
            .getOrNull()
            .orEmpty()
        logDataSync("Fetched ${biaDataPoints.size} BIA data points from Samsung Health")

        if (biaDataPoints.isEmpty()) {
            logDataSync("No BIA data points available to import")
            return
        }

        val userProfileDao = wearableAppDataBase.userProfileDao()
        val latestProfile = runCatching { userProfileDao.getLatest().firstOrNull() }
            .onFailure { Log.e(TAG, "Failed to load latest user profile", it) }
            .getOrNull()
        logDataSync("Latest wearable user profile found: ${latestProfile != null}")

        val enrollmentDate = studyId?.let { id ->
            runCatching { enrollmentDatePref.getEnrollmentDate(id) }
                .getOrNull()
                ?.let { dateString -> runCatching { LocalDate.parse(dateString) }.getOrNull() }
        }
        logDataSync("Enrollment date resolved for BIA import: ${enrollmentDate ?: "none"}")

        val bias = biaDataPoints.mapNotNull { it.toBia(enrollmentDate) }
        val userProfiles = biaDataPoints.mapNotNull {
            it.toUserProfile(latestProfile ?: DEFAULT_USER_PROFILE)
        }
        logDataSync("Parsed ${bias.size} BIA entries and ${userProfiles.size} user profiles from Samsung Health data")

        if (bias.isNotEmpty()) {
            wearableAppDataBase.biaDao().insertAll(bias)
            logDataSync("Inserted ${bias.size} BIA entries into database")
        }

        if (userProfiles.isNotEmpty()) {
            saveUserProfiles(userProfiles)
        }

        if (bias.isEmpty() && userProfiles.isEmpty()) {
            Log.i(TAG, "No BIA or user profile data parsed from Samsung Health")
            logDataSync("No BIA or user profile data parsed from Samsung Health during import")
        }
    }

    private fun HealthDataPoint.toBia(enrollmentDate: LocalDate?): Bia? {
        logDataSync("Converting HealthDataPoint ${uid ?: "unknown"} to BIA model")
        val measurementStartTime = startTime ?: return null
        val timestamp = measurementStartTime.toEpochMilli()
        val timeOffsetMillis = zoneOffset?.totalSeconds?.times(1000)?.toInt() ?: getCurrentTimeOffset()

        fun getFloatField(name: String): Float {
            return getFloatFieldOrNull(name) ?: 0f
        }

        val weekNumber = enrollmentDate?.let { enrollment ->
            val measurementDate = measurementStartTime.atZone(ZoneId.systemDefault()).toLocalDate()
            ChronoUnit.WEEKS.between(enrollment, measurementDate).toInt() + 1
        } ?: 0
        logDataSync("Calculated week number $weekNumber for BIA measurement ${uid ?: "unknown"}")

        return Bia(
            timestamp = timestamp,
            basalMetabolicRate = getFloatField("basal_metabolic_rate"),
            bodyFatMass = getFloatField("body_fat_mass"),
            bodyFatRatio = getFloatField("body_fat"),
            fatFreeMass = getFloatField("fat_free_mass"),
            fatFreeRatio = getFloatField("fat_free"),
            skeletalMuscleMass = getFloatField("skeletal_muscle_mass")
                .takeIf { it != 0f } ?: getFloatField("muscle_mass"),
            skeletalMuscleRatio = getFloatField("skeletal_muscle"),
            totalBodyWater = getFloatField("total_body_water"),
            measurementProgress = 1f,
            status = 0,
            weekNumber = weekNumber,
            timeOffset = timeOffsetMillis,
            id = uid.orEmpty().ifBlank { UUID.randomUUID().toString() },
        )
    }

    private fun HealthDataPoint.toUserProfile(fallback: UserProfile): UserProfile? {
        logDataSync("Converting HealthDataPoint ${uid ?: "unknown"} to wearable user profile")
        val measurementStartTime = startTime ?: return null
        val timestamp = measurementStartTime.toEpochMilli()
        val timeOffsetMillis = zoneOffset?.totalSeconds?.times(1000)?.toInt() ?: getCurrentTimeOffset()

        val weight = getFloatFieldOrNull("weight")?.takeIf { it > 0f } ?: fallback.weight
        val height = getFloatFieldOrNull("height")?.takeIf { it > 0f } ?: fallback.height
        val yearBirth = getIntFieldOrNull("birth_year")?.takeIf { it > 0 } ?: fallback.yearBirth
        val gender = getIntFieldOrNull("gender")
            ?.takeIf { it in Gender.values().indices }
            ?.let { Gender.values()[it] }
            ?: fallback.gender
        val isMetricUnit = getBooleanFieldOrNull("is_metric_unit") ?: fallback.isMetricUnit
            ?: DEFAULT_IS_METRIC_UNIT

        return UserProfile(
            height = height,
            weight = weight,
            yearBirth = yearBirth,
            gender = gender,
            isMetricUnit = isMetricUnit,
            timestamp = timestamp,
            timeOffset = timeOffsetMillis,
        )
    }

    private fun HealthDataPoint.getFloatFieldOrNull(name: String): Float? {
        val field = bodyCompositionFields[name] ?: return null
        val value = runCatching { getValue(field) }.getOrNull()
        if (value == null) {
            logDataSync("Float field '$name' missing for HealthDataPoint ${uid ?: "unknown"}")
        }
        return when (value) {
            is Number -> value.toFloat()
            else -> null
        }
    }

    private fun HealthDataPoint.getIntFieldOrNull(name: String): Int? {
        val field = bodyCompositionFields[name] ?: return null
        val value = runCatching { getValue(field) }.getOrNull()
        if (value == null) {
            logDataSync("Int field '$name' missing for HealthDataPoint ${uid ?: "unknown"}")
        }
        return when (value) {
            is Number -> value.toInt()
            else -> null
        }
    }

    private fun HealthDataPoint.getBooleanFieldOrNull(name: String): Boolean? {
        val field = bodyCompositionFields[name] ?: return null
        val value = runCatching { getValue(field) }.getOrNull()
        if (value == null) {
            logDataSync("Boolean field '$name' missing for HealthDataPoint ${uid ?: "unknown"}")
        }
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> null
        }
    }

    private val bodyCompositionFields: Map<String, Field<*>> by lazy {
        logDataSync("Initializing body composition field map")
        DataTypes.BODY_COMPOSITION.allFields.associateBy { it.name }.also {
            logDataSync("Body composition field map contains ${it.size} entries")
        }
    }

    override suspend fun syncWearableData() {
        logDataSync("Starting wearable data synchronization for all data types")
        val activeStudies = studyRepository.getActiveStudies().first()
        logDataSync("Loaded ${activeStudies.size} active studies for wearable sync")
        importBiaFromSamsungHealth(activeStudies.firstOrNull()?.id)

        activeStudies
            .associate { (studyId) -> getAgreedWearableDataTypes(studyId).first() }
            .reverse()
            .forEach { (dataType, studyIds) ->
                logDataSync("Syncing data type $dataType for studies ${studyIds.joinToString()}")
                syncRoomToServer(getDao(dataType), dataType, studyIds)
            }
        logDataSync("Completed wearable data synchronization for all data types")
    }

    override suspend fun syncWearableData(
        studyIds: List<String>,
        dataType: PrivDataType,
        csvInputStream: InputStream
    ) {
        logDataSync("Starting manual wearable data sync for $dataType and studies ${studyIds.joinToString()}")
        val data = when (dataType) {
            PrivDataType.WEAR_ACCELEROMETER -> readCsv<Accelerometer>(csvInputStream)
            PrivDataType.WEAR_BIA -> readCsv<Bia>(csvInputStream)
            PrivDataType.WEAR_ECG -> readCsv<EcgSet>(csvInputStream)
            PrivDataType.WEAR_PPG_GREEN -> readCsv<PpgGreen>(csvInputStream)
            PrivDataType.WEAR_PPG_IR -> readCsv<PpgIr>(csvInputStream)
            PrivDataType.WEAR_PPG_RED -> readCsv<PpgRed>(csvInputStream)
            PrivDataType.WEAR_SPO2 -> readCsv<SpO2>(csvInputStream)
            PrivDataType.WEAR_SWEAT_LOSS -> readCsv<SweatLoss>(csvInputStream)
            PrivDataType.WEAR_HEART_RATE -> readCsv<HeartRate>(csvInputStream)
            PrivDataType.WEAR_USER_PROFILE -> readCsv<UserProfile>(csvInputStream)
        }

        logDataSync("Uploading ${data.size} ${dataType.name} records to server via manual sync")
        grpcHealthDataSynchronizer.syncHealthData(
            studyIds,
            toHealthDataModel(dataType, data)
        ).onSuccess {
            Log.i(TAG, "success to upload data: $dataType")
            logDataSync("Successfully uploaded ${data.size} ${dataType.name} records via manual sync")
            AppLogger.saveLog(DataSyncLog("sync $dataType ${data.size}"))
        }.onFailure {
            Log.e(TAG, "fail to upload data to server")
            Log.e(TAG, it.stackTraceToString())
            logDataSync("Failed manual upload for $dataType: ${it.message}", it)
            AppLogger.saveLog(DataSyncLog("FAIL: sync data $dataType ${it.stackTraceToString()}"))
        }.getOrThrow()
    }

    private fun getAgreedWearableDataTypes(studyId: String) =
        shareAgreementRepository.getAgreedWearableDataTypes(studyId)
            .map { dataTypes ->
                logDataSync(
                    "Resolved ${dataTypes.size} agreed wearable data types for study $studyId: ${dataTypes.joinToString()}"
                )
                studyId to dataTypes
            }

    private fun <T1, T2> Map<T1, List<T2>>.reverse(): Map<T2, List<T1>> {
        val reversed = mutableMapOf<T2, MutableList<T1>>()

        forEach { (key, value) ->
            value.forEach {
                if (!reversed.contains(it)) reversed[it] = mutableListOf()
                reversed[it]?.add(key)
            }
        }

        logDataSync(
            "Reversed mapping result: ${
                reversed.entries.joinToString { (type, studies) -> "$type -> ${studies.joinToString()}" }
            }"
        )
        return reversed
    }

    private fun getDao(dataType: PrivDataType) =
        when (dataType) {
            PrivDataType.WEAR_ACCELEROMETER -> wearableAppDataBase.accelerometerDao()
            PrivDataType.WEAR_BIA -> wearableAppDataBase.biaDao()
            PrivDataType.WEAR_ECG -> wearableAppDataBase.ecgDao()
            PrivDataType.WEAR_PPG_GREEN -> wearableAppDataBase.ppgGreenDao()
            PrivDataType.WEAR_PPG_IR -> wearableAppDataBase.ppgIrDao()
            PrivDataType.WEAR_PPG_RED -> wearableAppDataBase.ppgRedDao()
            PrivDataType.WEAR_SPO2 -> wearableAppDataBase.spO2Dao()
            PrivDataType.WEAR_SWEAT_LOSS -> wearableAppDataBase.sweatLossDao()
            PrivDataType.WEAR_HEART_RATE -> wearableAppDataBase.heartRateDao()
            PrivDataType.WEAR_USER_PROFILE -> wearableAppDataBase.userProfileDao()
        }.also { logDataSync("Resolved DAO for $dataType: ${it::class.simpleName}") }

    private suspend fun <T : TimestampMapData> syncRoomToServer(
        dao: PrivDao<T>,
        dataType: PrivDataType,
        studyIds: List<String>,
    ) {
        Log.i(TAG, "syncWearableData Start $dataType")
        logDataSync("Starting room-to-server sync for $dataType and studies ${studyIds.joinToString()}")
        var cnt = 0
        var check = false
        while (!check && cnt < 30) {
            Log.i(TAG, "current try: $cnt")
            logDataSync("Attempt $cnt for syncing $dataType")
            cnt++
            check = true
            kotlin.runCatching {
                when (dataType) {
                    PrivDataType.WEAR_PPG_GREEN, PrivDataType.WEAR_ACCELEROMETER -> {
                        logDataSync("Executing batch sync strategy for $dataType")
                        val batchHealthData = mutableListOf<List<T>>()
                        var lastTimestamp = -1L
                        val pageSource = dao.getGreaterThan(0)
                        logDataSync("Initialized paging source for $dataType")

                        var loadResult =
                            pageSource.load(
                                PagingSource.LoadParams.Refresh(
                                    null,
                                    BuildConfig.BATCH_HEALTH_DATA_SIZE,
                                    false
                                )
                            )

                        var nextPage: Int? = -1
                        do {
                            when (val copiedLoadResult = loadResult) {
                                is PagingSource.LoadResult.Page -> {
                                    if (copiedLoadResult.data.isEmpty() && nextPage == -1) {
                                        Log.e(TAG, "data is empty: $dataType")
                                        AppLogger.saveLog(DataSyncLog("nothing to sync $dataType"))
                                        logDataSync("No data found for initial page of $dataType; finishing sync")
                                        finishSync(dao, lastTimestamp)
                                        break
                                    }

                                    nextPage = copiedLoadResult.nextKey
                                    logDataSync(
                                        "Loaded page with ${copiedLoadResult.data.size} $dataType records; nextKey=$nextPage"
                                    )

                                    if (copiedLoadResult.data.size == BuildConfig.BATCH_HEALTH_DATA_SIZE) {
                                        batchHealthData.add(copiedLoadResult.data)
                                        logDataSync(
                                            "Accumulated batch segment with ${copiedLoadResult.data.size} $dataType records; bat"
                                                + "ches=${batchHealthData.size}"
                                        )
                                    }

                                    if (batchHealthData.size != 0 && (nextPage == null || copiedLoadResult.data.size != BuildConfig.BATCH_HEALTH_DATA_SIZE || batchHealthData.size == BuildConfig.NUM_BATCH_HEALTH_DATA)) {
                                        logDataSync(
                                            "Flushing ${batchHealthData.size} batches for $dataType to server"
                                        )
                                        grpcHealthDataSynchronizer.syncBatchHealthData(
                                            studyIds,
                                            batchHealthData.map {
                                                toHealthDataModel(dataType, it)
                                            },
                                        ).onSuccess {
                                            Log.i(TAG, "success to upload data: $dataType")
                                            AppLogger.saveLog(
                                                DataSyncLog(
                                                    "sync $dataType ${
                                                        batchHealthData.map { it.size }.sum()
                                                    }"
                                                )
                                            )
                                            logDataSync(
                                                "Successfully uploaded batch payload for $dataType with ${
                                                    batchHealthData.map { it.size }.sum()
                                                } records"
                                            )
                                            lastTimestamp = batchHealthData.last().last().timestamp
                                            logDataSync("Updated last timestamp for $dataType to $lastTimestamp")
                                            batchHealthData.clear()
                                        }.onFailure {
                                            Log.e(TAG, "fail to upload data to server")
                                            Log.e(TAG, it.stackTraceToString())
                                            AppLogger.saveLog(DataSyncLog("FAIL: sync data $dataType ${it.stackTraceToString()}"))
                                            logDataSync(
                                                "Failed to upload batch payload for $dataType: ${it.message}",
                                                it
                                            )
                                            finishSync(dao, lastTimestamp)
                                        }.getOrThrow()
                                    }
                                }

                                is PagingSource.LoadResult.Error -> {
                                    Log.e(TAG, copiedLoadResult.throwable.stackTraceToString())
                                    AppLogger.saveLog(DataSyncLog("FAIL: sync data $dataType ${copiedLoadResult.throwable.stackTraceToString()}"))
                                    logDataSync(
                                        "Paging error while syncing $dataType: ${copiedLoadResult.throwable.message}",
                                        copiedLoadResult.throwable
                                    )
                                    finishSync(dao, lastTimestamp)
                                    throw copiedLoadResult.throwable
                                }

                                is PagingSource.LoadResult.Invalid -> {
                                    Log.e(TAG, "Invalid page")
                                    AppLogger.saveLog(DataSyncLog("FAIL: sync data $dataType Invalid page"))
                                    logDataSync("Encountered invalid page while syncing $dataType")
                                    finishSync(dao, lastTimestamp)
                                    throw IOException("Invalid page")
                                }
                            }

                            if (nextPage == null) {
                                finishSync(dao, lastTimestamp)
                                logDataSync("Finished batch sync loop for $dataType with lastTimestamp=$lastTimestamp")
                            } else {
                                loadResult = pageSource.load(
                                    PagingSource.LoadParams.Append(nextPage, BuildConfig.BATCH_HEALTH_DATA_SIZE, false)
                                )
                            }
                        } while (nextPage != null)
                    }

                    else -> {
                        logDataSync("Executing standard sync strategy for $dataType")
                        var lastTimestamp = 0L
                        val pageSource = dao.getGreaterThan(0)

                        var loadResult =
                            pageSource.load(PagingSource.LoadParams.Refresh(null, PAGE_LOAD_SIZE, false))

                        var nextPage: Int? = -1
                        do {
                            when (val copiedLoadResult = loadResult) {
                                is PagingSource.LoadResult.Page -> {
                                    if (copiedLoadResult.data.isEmpty() && nextPage == -1) {
                                        Log.e(TAG, "data is empty: $dataType")
                                        AppLogger.saveLog(DataSyncLog("nothing to sync $dataType"))
                                        logDataSync("No data found for $dataType; ending sync loop")
                                        break
                                    }

                                    grpcHealthDataSynchronizer.syncHealthData(
                                        studyIds,
                                        toHealthDataModel(dataType, copiedLoadResult.data)
                                    ).onSuccess {
                                        Log.i(TAG, "success to upload data: $dataType")
                                        AppLogger.saveLog(DataSyncLog("sync $dataType ${copiedLoadResult.data.size}"))
                                        logDataSync(
                                            "Successfully uploaded ${copiedLoadResult.data.size} records for $dataType"
                                        )
                                        lastTimestamp = copiedLoadResult.data.last().timestamp
                                        logDataSync("Updated last timestamp for $dataType to $lastTimestamp")
                                        nextPage = copiedLoadResult.nextKey
                                    }.onFailure {
                                        Log.e(TAG, "fail to upload data to server")
                                        Log.e(TAG, it.stackTraceToString())
                                        AppLogger.saveLog(DataSyncLog("FAIL: sync data $dataType ${it.stackTraceToString()}"))
                                        logDataSync(
                                            "Failed to upload ${copiedLoadResult.data.size} records for $dataType: ${it.message}",
                                            it
                                        )
                                        finishSync(dao, lastTimestamp)
                                    }.getOrThrow()
                                }

                                is PagingSource.LoadResult.Error -> {
                                    Log.e(TAG, copiedLoadResult.throwable.stackTraceToString())
                                    AppLogger.saveLog(DataSyncLog("FAIL: sync data $dataType ${copiedLoadResult.throwable.stackTraceToString()}"))
                                    logDataSync(
                                        "Paging error while syncing $dataType: ${copiedLoadResult.throwable.message}",
                                        copiedLoadResult.throwable
                                    )
                                    finishSync(dao, lastTimestamp)
                                    throw copiedLoadResult.throwable
                                }

                                is PagingSource.LoadResult.Invalid -> {
                                    Log.e(TAG, "Invalid page")
                                    AppLogger.saveLog(DataSyncLog("FAIL: sync data $dataType Invalid page"))
                                    logDataSync("Encountered invalid page while syncing $dataType")
                                    finishSync(dao, lastTimestamp)
                                    throw IOException("Invalid page")
                                }
                            }

                            if (nextPage == null) {
                                finishSync(dao, lastTimestamp)
                                logDataSync("Finished standard sync loop for $dataType with lastTimestamp=$lastTimestamp")
                            } else {
                                loadResult = pageSource.load(
                                    PagingSource.LoadParams.Append(nextPage!!, PAGE_LOAD_SIZE, false)
                                )
                            }
                        } while (nextPage != null)
                    }
                }
            }.onFailure {
                check = false
                Log.e(TAG, it.stackTraceToString())
                logDataSync("Retrying $dataType sync due to failure: ${it.message}", it)
            }
        }
        logDataSync("Completed room-to-server sync routine for $dataType")
    }

    private fun <T : TimestampMapData> toHealthDataModel(dataType: PrivDataType, data: List<T>): HealthDataModel {
        logDataSync("Converting ${data.size} $dataType records to HealthDataModel")
        val mapped = data.map { it.toDataMap() }
        logDataSync("Converted $dataType records to map payload with size ${mapped.size}")
        return HealthDataModel(dataType, mapped)
    }

    private fun <T : Timestamp> finishSync(dao: PrivDao<T>, lastSyncTime: Long) {
        dao.deleteLEThan(lastSyncTime)
        logDataSync("Deleted records with timestamp <= $lastSyncTime during sync cleanup")
    }

    private fun logDataSync(message: String, throwable: Throwable? = null) {
        val tag = TAG ?: "WearableDataReceiverRepositoryImpl"
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
        runBlocking {
            AppLogger.saveLog(DataSyncLog(fullMessage))
        }
    }

    companion object {
        private const val PAGE_LOAD_SIZE = 1000
        private val TAG = this::class.simpleName
        private const val DEFAULT_HEIGHT = 170f
        private const val DEFAULT_WEIGHT = 70f
        private const val DEFAULT_YEAR_BIRTH = 1990
        private val DEFAULT_GENDER = Gender.UNKNOWN
        private const val DEFAULT_IS_METRIC_UNIT = true
        private val DEFAULT_USER_PROFILE = UserProfile(
            height = DEFAULT_HEIGHT,
            weight = DEFAULT_WEIGHT,
            yearBirth = DEFAULT_YEAR_BIRTH,
            gender = DEFAULT_GENDER,
            isMetricUnit = DEFAULT_IS_METRIC_UNIT,
        )
    }
}
