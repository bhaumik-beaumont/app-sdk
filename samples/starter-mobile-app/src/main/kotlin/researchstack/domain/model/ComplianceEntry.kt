package researchstack.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import researchstack.domain.model.TimestampMapData
import researchstack.util.getCurrentTimeOffset

const val COMPLIANCE_ENTRY_TABLE_NAME = "compliance_entry"

@Entity(tableName = COMPLIANCE_ENTRY_TABLE_NAME)
data class ComplianceEntry(
    @PrimaryKey val weekNumber: Int,
    override val timestamp: Long,
    val startDate: String,
    val id: String,
    val endDate: String,
    val totalActivityMinutes: Int,
    val resistanceSessionCount: Int,
    val biaRecordCount: Int,
    val weightRecordCount: Int,
    @ColumnInfo(defaultValue = "0")
    val avgWeight: Float = 0F,
    @ColumnInfo(defaultValue = "0")
    val height: Float = 0F,
    @ColumnInfo(defaultValue = "0")
    val age: Int = 0,
    @ColumnInfo(defaultValue = "2")
    val gender: Int = 2,
    override val timeOffset: Int = getCurrentTimeOffset(),
) : TimestampMapData {
    override fun toDataMap(): Map<String, Any> = mapOf(
        ::weekNumber.name to weekNumber,
        ::timestamp.name to timestamp,
        ::startDate.name to startDate,
        ::endDate.name to endDate,
        ::id.name to id,
        ::totalActivityMinutes.name to totalActivityMinutes,
        ::resistanceSessionCount.name to resistanceSessionCount,
        ::biaRecordCount.name to biaRecordCount,
        ::weightRecordCount.name to weightRecordCount,
        ::avgWeight.name to avgWeight,
        ::height.name to height,
        ::age.name to age,
        ::gender.name to gender,
        ::timeOffset.name to timeOffset,
    )
}
