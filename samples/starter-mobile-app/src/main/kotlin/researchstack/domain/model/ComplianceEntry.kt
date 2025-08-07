package researchstack.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

const val COMPLIANCE_ENTRY_TABLE_NAME = "compliance_entry"

@Entity(tableName = COMPLIANCE_ENTRY_TABLE_NAME)
data class ComplianceEntry(
    @PrimaryKey val weekNumber: Int,
    val startDate: String,
    val endDate: String,
    val totalActivityMinutes: Int,
    val resistanceSessionCount: Int,
    val biaRecordCount: Int,
    val weightRecordCount: Int,
)
