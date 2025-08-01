package researchstack.data.datasource.healthConnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.reflect.KClass

class HealthConnectDataSource @Inject constructor(private val healthConnectClient: HealthConnectClient) {
    suspend fun <T : Record> getData(recordClass: KClass<out T>, duration: Long = 30): List<Record> {
        val currentDate: LocalDateTime = LocalDateTime.now().with(LocalTime.MIDNIGHT)
        val startTime: Instant =
            currentDate.minusDays(duration).atZone(ZoneId.systemDefault()).toInstant()
        val endTime: Instant = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()

        val result = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordClass,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
        return result.records
    }

    suspend fun getAggregateData(uid: String): ExerciseSessionData {
        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        // Use the start time and end time from the session, for reading raw and aggregate data.
        val timeRangeFilter = TimeRangeFilter.between(
            startTime = exerciseSession.record.startTime,
            endTime = exerciseSession.record.endTime
        )
        val aggregateDataTypes = setOf(
            ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
            StepsRecord.COUNT_TOTAL,
            SpeedRecord.SPEED_AVG,
            DistanceRecord.DISTANCE_TOTAL,
            TotalCaloriesBurnedRecord.ENERGY_TOTAL,
            HeartRateRecord.BPM_AVG,
            HeartRateRecord.BPM_MAX,
            HeartRateRecord.BPM_MIN,
        )
        // Limit the data read to just the application that wrote the session. This may or may not
        // be desirable depending on the use case: In some cases, it may be useful to combine with
        // data written by other apps.
        val dataOriginFilter = setOf(exerciseSession.record.metadata.dataOrigin)
        val aggregateRequest = AggregateRequest(
            metrics = aggregateDataTypes,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = dataOriginFilter
        )
        val aggregateData = healthConnectClient.aggregate(aggregateRequest)

        return ExerciseSessionData(
            uid = uid,
            totalActiveTime = aggregateData[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL],
            totalSteps = aggregateData[StepsRecord.COUNT_TOTAL],
            totalDistance = aggregateData[DistanceRecord.DISTANCE_TOTAL],
            totalEnergyBurned = aggregateData[TotalCaloriesBurnedRecord.ENERGY_TOTAL],
            minHeartRate = aggregateData[HeartRateRecord.BPM_MIN],
            maxHeartRate = aggregateData[HeartRateRecord.BPM_MAX],
            avgHeartRate = aggregateData[HeartRateRecord.BPM_AVG],
            maxSpeed = aggregateData[SpeedRecord.SPEED_MAX]?.inMilesPerHour ?: 0.0,
            meanSpeed = aggregateData[SpeedRecord.SPEED_AVG]?.inMilesPerHour ?: 0.0,
        )
    }
}
