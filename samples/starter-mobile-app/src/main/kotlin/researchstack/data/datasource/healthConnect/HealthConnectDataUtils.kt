package researchstack.data.datasource.healthConnect

import android.annotation.SuppressLint
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession
import com.samsung.android.sdk.health.data.request.DataType
import researchstack.domain.model.healthConnect.Exercise
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.domain.model.shealth.SHealthDataType
import researchstack.presentation.util.toDecimalFormat
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
    samsungRecord: HealthDataPoint,
    session: ExerciseSession,
    studyId: String,
    enrollmentDatePref: EnrollmentDatePref,
    sessionIndex: Int
): Exercise {
    val exerciseTypeName = session.exerciseType?.name
        ?: samsungRecord.getValue(DataType.ExerciseType.EXERCISE_TYPE)?.name
    val exerciseOrdinal = session.exerciseType?.ordinal?.toLong()
        ?: samsungRecord.getValue(DataType.ExerciseType.EXERCISE_TYPE)?.ordinal?.toLong()
        ?: 0L
    val enrollmentDateStr = enrollmentDatePref.getEnrollmentDate(studyId)
    val sessionStartMillis = session.startTime.toEpochMilli()
    val sessionEndMillis = session.endTime.toEpochMilli()
    val weekNumber = enrollmentDateStr?.let { dateString ->
        val enrollmentMillis = LocalDate.parse(dateString)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        (((sessionStartMillis - enrollmentMillis) / (7 * 24 * 60 * 60 * 1000L)) + 1).toInt()
    } ?: 0
    val exerciseId = buildString {
        append(samsungRecord.uid ?: "${sessionStartMillis}-${sessionEndMillis}")
        append("-")
        append(sessionIndex)
    }
    val timeOffset = samsungRecord.zoneOffset?.totalSeconds?.times(1000)?.toInt()
        ?: getCurrentTimeOffset()
    return Exercise(
        id = exerciseId,
        timestamp = sessionStartMillis,
        startTime = sessionStartMillis,
        endTime = sessionEndMillis,
        exerciseType = exerciseOrdinal,
        exerciseName = session.customTitle ?: exerciseTypeName ?: "",
        calorie = session.calories.toDecimalFormat(0).toDouble(),
        duration = session.duration?.toMillis() ?: (sessionEndMillis - sessionStartMillis),
        timeOffset = timeOffset,
        weekNumber = weekNumber.toLong(),
        meanHeartRate = session.meanHeartRate?.toDouble() ?: 0.0,
        maxHeartRate = session.maxHeartRate?.toDouble() ?: 0.0,
        minHeartRate = session.minHeartRate?.toDouble() ?: 0.0,
        distance = session.distance?.toDouble() ?: 0.0,
        maxSpeed = session.maxSpeed?.toDouble() ?: 0.0,
        meanSpeed = session.meanSpeed?.toDouble() ?: 0.0,
        isResistance = isResistance(exerciseOrdinal.toInt())
    )
}

private fun isResistance(exerciseOrdinal: Int): Boolean {
    when (exerciseOrdinal) {
        DataType.ExerciseType.PredefinedExerciseType.ARCHERY.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.ARM_CURLS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.ARM_EXTENSIONS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.BACK_EXTENSIONS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.BALLET.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.BENCH_PRESS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.CIRCUIT_TRAINING.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.CRUNCH.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.DEADLIFTS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.STAIR_CLIMBING_MACHINE.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.FLYING_DISC.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.FRONT_RAISES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.HIGH_KNEES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.LAT_PULLDOWNS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.LATERAL_RAISES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.LEG_CURLS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.LEG_EXTENSIONS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.LEG_PRESSES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.LEG_RAISES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.LUNGES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.PILATES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.PLANK.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.PULL_UPS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.PUSH_UPS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.SHOULDER_PRESSES.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.SIT_UPS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.SKATERS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.SQUATS.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.STRETCHING.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.WEIGHT_MACHINE.ordinal,
        DataType.ExerciseType.PredefinedExerciseType.YOGA.ordinal->
            return true
        else -> return false
    }
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
