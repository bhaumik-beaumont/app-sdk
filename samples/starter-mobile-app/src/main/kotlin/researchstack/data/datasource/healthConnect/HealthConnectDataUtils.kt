package researchstack.data.datasource.healthConnect

import android.annotation.SuppressLint
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.ExerciseSessionRecord.Companion.EXERCISE_TYPE_INT_TO_STRING_MAP
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import researchstack.domain.model.healthConnect.Exercise
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.domain.model.shealth.SHealthDataType
import researchstack.util.getCurrentTimeOffset
import researchstack.util.toEpochMilli
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

fun processStepsData(dataList: List<StepsRecord>): HashMap<Long, StringBuilder> {
    val stepsDataMap = HashMap<Long, StringBuilder>()
    dataList.forEach { data ->
        val startTime = data.startTime.toEpochMilli()
        val endTime = data.endTime.toEpochMilli()
        val zoneOffset = data.startZoneOffset?.toEpochMilli() ?: 0L
        val count = data.count
        val dataString = "$startTime|$endTime|$zoneOffset|$count\n"
        val startDayTime = getLocalDateStartTime(data.startTime)
        stepsDataMap.computeIfAbsent(startDayTime) { StringBuilder() }.append(dataString)
    }
    return stepsDataMap
}

fun processBloodGlucoseData(dataList: List<BloodGlucoseRecord>): HashMap<Long, StringBuilder> {
    val bloodGlucoseDataMap = HashMap<Long, StringBuilder>()
    dataList.forEach { data ->
        val time = data.time.toEpochMilli()
        val updateTime = data.metadata.lastModifiedTime.toEpochMilli()
        val glucoseLevel = data.level
        val measurementType = getSpecimenSourceString(data.specimenSource)
        val mealType = getRelationToMealString(data.relationToMeal)
        val dataSource = data.metadata.dataOrigin.packageName
        val dataUuid = data.metadata.id
        val zoneOffset = data.zoneOffset?.toEpochMilli() ?: 0L
        val startDayTime = getLocalDateStartTime(data.time)
        val dataString =
            "$time|$updateTime|$glucoseLevel|$measurementType|$mealType|$dataSource|$dataUuid|$zoneOffset\n"

        bloodGlucoseDataMap.computeIfAbsent(startDayTime) { StringBuilder() }
            .append(dataString)
    }
    return bloodGlucoseDataMap
}

fun processHeartRateData(dataList: List<HeartRateRecord>): HashMap<Long, StringBuilder> {
    val heartRateDataMap = HashMap<Long, StringBuilder>()
    dataList.forEach { data ->
        val startTime = data.startTime.toEpochMilli()
        val endTime = data.endTime.toEpochMilli()
        val updateTime = data.metadata.lastModifiedTime.toEpochMilli()
        val hearRateValues = data.samples.map { it.beatsPerMinute }
        val heartRate = hearRateValues.average()
        val minHeartRate = hearRateValues.minOrNull() ?: 0
        val maxHeartRate = hearRateValues.maxOrNull() ?: 0
        val dataSource = data.metadata.dataOrigin.packageName
        val dataUuid = data.metadata.id
        val zoneOffset = data.startZoneOffset?.toEpochMilli() ?: 0L
        val startDayTime = getLocalDateStartTime(data.startTime)
        val dataString =
            "$startTime|$endTime|$updateTime|$heartRate|$minHeartRate|$maxHeartRate|$dataSource|$dataUuid|$zoneOffset\n"

        heartRateDataMap.computeIfAbsent(startDayTime) { StringBuilder() }.append(dataString)
    }
    return heartRateDataMap
}

fun processOxygenSaturationData(dataList: List<OxygenSaturationRecord>): HashMap<Long, StringBuilder> {
    val bloodOxygenDataMap = HashMap<Long, StringBuilder>()
    dataList.forEach { data ->
        val time = data.time.toEpochMilli()
        val oxygenSaturation = data.percentage
        val dataSource = data.metadata.dataOrigin.packageName
        val dataUuid = data.metadata.id
        val zoneOffset = data.zoneOffset?.toEpochMilli() ?: 0L
        val startDayTime = getLocalDateStartTime(data.time)
        val dataString =
            "$time|$oxygenSaturation|$dataSource|$dataUuid|$zoneOffset\n"

        bloodOxygenDataMap.computeIfAbsent(startDayTime) { StringBuilder() }
            .append(dataString)
    }
    return bloodOxygenDataMap
}

fun processBloodPressureData(dataList: List<BloodPressureRecord>): HashMap<Long, StringBuilder> {
    val bloodPressureDataMap = HashMap<Long, StringBuilder>()
    dataList.forEach { data ->
        val time = data.time.toEpochMilli()
        val updateTime = data.metadata.lastModifiedTime.toEpochMilli()

        val systolic = data.systolic
        val diastolic = data.diastolic
        val bodyPosition = getBodyPosition(data.bodyPosition)
        val measurementLocation = getMeasurementLocation(data.measurementLocation)
        val dataSource = data.metadata.dataOrigin.packageName
        val dataUuid = data.metadata.id
        val zoneOffset = data.zoneOffset?.toEpochMilli() ?: 0L
        val startDayTime = getLocalDateStartTime(data.time)
        val dataString =
            "$time|$updateTime|$systolic|$diastolic|$bodyPosition|$measurementLocation|$dataSource|$dataUuid|$zoneOffset\n"

        bloodPressureDataMap.computeIfAbsent(startDayTime) { StringBuilder() }
            .append(dataString)
    }
    return bloodPressureDataMap
}

fun processSleepData(dataList: List<SleepSessionRecord>): HashMap<Long, StringBuilder> {
    val sleepDataMap = HashMap<Long, StringBuilder>()
    dataList.forEach { sleep ->
        val startTime = sleep.startTime.toEpochMilli()
        val endTime = sleep.endTime.toEpochMilli()
        val updateTime = sleep.metadata.lastModifiedTime.toEpochMilli()
        val sleepDuration = endTime - startTime
        val dataSource = sleep.metadata.dataOrigin.packageName
        val dataUuid = sleep.metadata.id
        val zoneOffset = sleep.startZoneOffset?.toEpochMilli() ?: 0L
        val startDayTime = getLocalDateStartTime(sleep.startTime)
        val dataString =
            "$startTime|$endTime|$updateTime|$sleepDuration|$dataSource|$dataUuid|$zoneOffset"
        sleepDataMap.computeIfAbsent(startDayTime) { StringBuilder() }.append(dataString)
    }
    return sleepDataMap
}

@SuppressLint("RestrictedApi")
suspend fun processExerciseData(
    record: ExerciseSessionRecord,
    sessionData: ExerciseSessionData,
    studyId: String,
    enrollmentDatePref: EnrollmentDatePref
): Exercise {

    val name = EXERCISE_TYPE_INT_TO_STRING_MAP[record.exerciseType]
    val enrollmentDateStr = enrollmentDatePref.getEnrollmentDate(studyId)
    val weekNumber = enrollmentDateStr?.let { dateString ->
        val enrollmentMillis = LocalDate.parse(dateString)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        (((record.startTime.toEpochMilli() - enrollmentMillis) / (7 * 24 * 60 * 60 * 1000L)) + 1).toInt()
    } ?: 0

    val exercise = Exercise(
        id = record.metadata.id,
        timestamp = record.startTime.toEpochMilli(),
        startTime = record.startTime.toEpochMilli(),
        endTime = record.endTime.toEpochMilli(),
        exerciseType = record.exerciseType.toLong(),
        exerciseName = name ?: "",
        calorie = (sessionData.totalEnergyBurned?.inCalories ?: 0.0).toInt()/1000*1.0,
        duration = sessionData.totalActiveTime?.toMillis() ?: 0,
        timeOffset = getCurrentTimeOffset(),
        weekNumber = weekNumber*1L,
        meanHeartRate = sessionData.avgHeartRate?.toDouble() ?: 0.00,
        maxHeartRate = sessionData.maxHeartRate?.toDouble() ?: 0.00,
        minHeartRate = sessionData.minHeartRate?.toDouble() ?: 0.00,
        distance = sessionData.totalDistance?.inMeters ?: 0.00,
        maxSpeed = sessionData.maxSpeed ?: 0.00,
        meanSpeed = sessionData.meanSpeed ?: 0.00,
    )
    return exercise
}

fun getLocalDateStartTime(time: Instant): Long {
    val localDateTime = LocalDateTime.ofInstant(time, ZoneId.systemDefault())
    return localDateTime.toLocalDate().atStartOfDay().toEpochMilli()
}

val specimenSourceMap = mapOf(
    BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID to "Interstitial Fluid",
    BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD to "Capillary Blood",
    BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA to "Plasma",
    BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM to "Serum",
    BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS to "Tears",
    BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD to "Whole Blood"
)

fun getSpecimenSourceString(specimenSource: Int): String {
    return specimenSourceMap[specimenSource] ?: "Unknown Specimen Source"
}

val relationToMealMap = mapOf(
    BloodGlucoseRecord.RELATION_TO_MEAL_GENERAL to "General",
    BloodGlucoseRecord.RELATION_TO_MEAL_FASTING to "Fasting",
    BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL to "Before Meal",
    BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL to "After Meal"
)

fun getRelationToMealString(relationToMeal: Int): String {
    return relationToMealMap[relationToMeal] ?: "Unknown Relation to Meal"
}

val bodyPositionMap = mapOf(
    BloodPressureRecord.BODY_POSITION_STANDING_UP to "Standing up",
    BloodPressureRecord.BODY_POSITION_SITTING_DOWN to "Sitting down",
    BloodPressureRecord.BODY_POSITION_LYING_DOWN to "Lying down",
    BloodPressureRecord.BODY_POSITION_RECLINING to "Reclining"
)

fun getBodyPosition(position: Int): String {
    return bodyPositionMap[position] ?: "Unknown body position"
}

val measurementLocationMap = mapOf(
    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_WRIST to "Left wrist",
    BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_WRIST to "Right wrist",
    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to "Left upper arm",
    BloodPressureRecord.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM to "Right upper arm"
)

fun getMeasurementLocation(measurementLocation: Int): String {
    return measurementLocationMap[measurementLocation] ?: "Unknown measurement location"
}

// TODO:: remove hardcoded string declare these string in separate constant file
fun getColumnHeader(dataType: SHealthDataType): String {
    return when (dataType) {
        SHealthDataType.STEPS -> "start_time|end_time|time_offset|count\n"
        SHealthDataType.SLEEP_SESSION ->
            "start_time|end_time|update_time|sleep_duration|data_source|datauuid|time_offset\n"

        SHealthDataType.BLOOD_GLUCOSE ->
            "time|update_time|glucose_level|measurement_type|meal_type|data_source|datauuid|time_offset\n"

        SHealthDataType.BLOOD_PRESSURE ->
            "time|update_time|systolic|diastolic|body_position|measurement_location|data_source|datauuid|time_offset\n"

        SHealthDataType.EXERCISE ->
            "start_time|end_time|update_time|exercise_type|title|duration|calories|data_source|datauuid|time_offset\n"

        SHealthDataType.HEART_RATE ->
            "start_time|end_time|update_time|heart_rate|min_heart_rate|max_heart_rate|data_source|datauuid|time_offset\n"

        SHealthDataType.OXYGEN_SATURATION ->
            "time|oxygen_saturation|data_source|datauuid|time_offset\n"

        else ->
            ""
    }
}
