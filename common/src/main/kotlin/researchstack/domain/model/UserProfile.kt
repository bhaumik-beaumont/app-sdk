package researchstack.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import researchstack.util.getCurrentTimeOffset
import researchstack.domain.model.Timestamp

const val USER_PROFILE_TABLE_NAME = "user_profile"

@Entity(tableName = USER_PROFILE_TABLE_NAME)
data class UserProfile(
    var height: Float,
    var weight: Float,
    var yearBirth: Int,
    var gender: Gender,
    var isMetricUnit: Boolean? = null,
    @PrimaryKey override val timestamp: Long = 0,
    override val timeOffset: Int = getCurrentTimeOffset(),
) : Timestamp

fun UserProfile?.isValid(): Boolean =
    this != null && yearBirth > 0 && gender != Gender.UNKNOWN && height > 0f && weight > 0f && isMetricUnit != null

enum class Gender {
    FEMALE,
    MALE,
    UNKNOWN,
}

