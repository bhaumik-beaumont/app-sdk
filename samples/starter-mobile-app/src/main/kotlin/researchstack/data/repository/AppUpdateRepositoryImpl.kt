package researchstack.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import researchstack.data.remoteconfig.REMOTE_CONFIG_LATEST_APP_VERSION_KEY
import researchstack.domain.repository.AppUpdateRepository

class AppUpdateRepositoryImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
) : AppUpdateRepository {

    override suspend fun fetchLatestAppVersionName(): Result<String?> = runCatching {
        remoteConfig.fetchAndActivateSuspending()
        remoteConfig.getString(REMOTE_CONFIG_LATEST_APP_VERSION_KEY).takeIf { it.isNotBlank() }
    }

    private suspend fun FirebaseRemoteConfig.fetchAndActivateSuspending(): Boolean =
        suspendCancellableCoroutine { continuation ->
            fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (continuation.isActive) {
                        continuation.resume(task.result)
                    }
                } else {
                    val exception = task.exception ?: IllegalStateException("Remote config fetch failed")
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
            }

            continuation.invokeOnCancellation {
                // No additional cleanup required
            }
        }
}
