package researchstack.domain.repository.healthConnect

import researchstack.domain.model.ComplianceEntry

interface HealthConnectDataSyncRepository {
    suspend fun syncHealthData(): List<ComplianceEntry>
}
