package researchstack.domain.model.healthConnect

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import researchstack.domain.model.TimestampMapData
import researchstack.util.getCurrentTimeOffset

const val EXERCISE_TABLE_NAME = "exercise"

@Entity(
    tableName = EXERCISE_TABLE_NAME
)
data class Exercise(
    @PrimaryKey
    override val timestamp: Long = 0,
    @SerializedName("start_time")
    val startTime: Long = 0,
    val id: String,
    @SerializedName("end_time")
    val endTime: Long = 0,
    @SerializedName("exercise_type")
    val exerciseType: Long = 0,
    @SerializedName("exercise_custom_type")
    val exerciseName :String = "",
    val calorie: Double = 0.0,
    val minHeartRate: Double = 0.0,
    val maxHeartRate: Double = 0.0,
    val meanHeartRate: Double = 0.0,
    val duration: Long = 0,
    val distance: Double = 0.0,
    val status: Int = 0,
    val meanSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    @SerializedName("week_number")
    val weekNumber: Long = 0,
    override val timeOffset: Int = getCurrentTimeOffset(),
) : TimestampMapData {
    override fun toDataMap(): Map<String, Any> =
        mapOf(
            "id" to id,
            "timestamp" to timestamp,
            "start_time" to startTime,
            "end_time" to endTime,
            "exercise_type" to exerciseType,
            "exercise_custom_type" to exerciseName,
            "mean_heart_rate" to meanHeartRate,
            "min_heart_rate" to minHeartRate,
            "max_heart_rate" to maxHeartRate,
            "calorie" to calorie,
            "duration" to duration,
            "distance" to distance,
            "max_speed" to maxSpeed,
            "mean_speed" to meanSpeed,
            "status" to status,
            "week_number" to weekNumber,
            "time_offset" to timeOffset,
        )
}
