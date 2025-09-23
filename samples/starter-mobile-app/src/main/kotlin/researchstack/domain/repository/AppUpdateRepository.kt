package researchstack.domain.repository

interface AppUpdateRepository {
    suspend fun fetchLatestAppVersionName(): Result<String?>
}
