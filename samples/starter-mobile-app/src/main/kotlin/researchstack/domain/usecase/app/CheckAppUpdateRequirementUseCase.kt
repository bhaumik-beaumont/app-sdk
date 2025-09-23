package researchstack.domain.usecase.app

import javax.inject.Inject
import researchstack.domain.repository.AppUpdateRepository
import researchstack.util.version.VersionComparator

class CheckAppUpdateRequirementUseCase @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
) {
    suspend operator fun invoke(currentVersionName: String): AppUpdateRequirement {
        val result = appUpdateRepository.fetchLatestAppVersionName()
        val latestVersion = result.getOrNull()?.trim().takeUnless { it.isNullOrEmpty() }
        val updateRequired = latestVersion?.let {
            VersionComparator.compareVersions(it, currentVersionName) > 0
        } ?: false
        return AppUpdateRequirement(updateRequired, latestVersion)
    }
}

data class AppUpdateRequirement(
    val isUpdateRequired: Boolean,
    val latestVersionName: String?,
)
