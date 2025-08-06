package researchstack.domain.model

data class ComplianceEntry(
    val weekNumber: Int,
    val startDate: String,
    val endDate: String,
    val totalActivityMinutes: Int,
    val resistanceSessionCount: Int,
    val biaRecordCount: Int,
    val weightRecordCount: Int,
)
