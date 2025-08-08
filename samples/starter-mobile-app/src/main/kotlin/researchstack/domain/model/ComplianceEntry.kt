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
        ::timeOffset.name to timeOffset,
    )
}
