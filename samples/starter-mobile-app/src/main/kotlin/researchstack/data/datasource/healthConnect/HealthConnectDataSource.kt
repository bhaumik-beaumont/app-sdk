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
import researchstack.domain.model.log.DataSyncLog
import researchstack.domain.usecase.log.AppLogger
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
        AppLogger.saveLog(DataSyncLog("start getAggregateData $uid"))

        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        AppLogger.saveLog(DataSyncLog("read session ${'$'}{exerciseSession.record.metadata.id}"))

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
        AppLogger.saveLog(DataSyncLog("aggregated data for $uid"))

        fun <T> logValue(name: String, value: T?): T? {
            return value?.also {
                AppLogger.saveLog(DataSyncLog("$name $it"))
            } ?: run {
                AppLogger.saveLog(DataSyncLog("nothing to sync for $name"))
                null
            }
        }

        val totalActiveTime = logValue(
            "totalActiveTime",
            aggregateData[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
        )
        val totalSteps = logValue("totalSteps", aggregateData[StepsRecord.COUNT_TOTAL])
        val totalDistance = logValue(
            "totalDistance",
            aggregateData[DistanceRecord.DISTANCE_TOTAL]
        )
        val totalEnergyBurned = logValue(
            "totalEnergyBurned",
            aggregateData[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
        )
        val minHeartRate = logValue("minHeartRate", aggregateData[HeartRateRecord.BPM_MIN])
        val maxHeartRate = logValue("maxHeartRate", aggregateData[HeartRateRecord.BPM_MAX])
        val avgHeartRate = logValue("avgHeartRate", aggregateData[HeartRateRecord.BPM_AVG])
        val maxSpeed = logValue(
            "maxSpeed",
            aggregateData[SpeedRecord.SPEED_MAX]?.inMilesPerHour
        ) ?: 0.0
        val meanSpeed = logValue(
            "meanSpeed",
            aggregateData[SpeedRecord.SPEED_AVG]?.inMilesPerHour
        ) ?: 0.0

        return ExerciseSessionData(
            uid = uid,
            totalActiveTime = totalActiveTime,
            totalSteps = totalSteps,
            totalDistance = totalDistance,
            totalEnergyBurned = totalEnergyBurned,
            minHeartRate = minHeartRate,
            maxHeartRate = maxHeartRate,
            avgHeartRate = avgHeartRate,
            maxSpeed = maxSpeed,
            meanSpeed = meanSpeed,
        )
    }
}
