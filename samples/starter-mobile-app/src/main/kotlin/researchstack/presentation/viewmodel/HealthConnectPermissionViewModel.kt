package researchstack.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.os.RemoteException
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import researchstack.presentation.util.HealthConnectManager
import researchstack.presentation.worker.WorkerRegistrar
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HealthConnectPermissionViewModel @Inject constructor(
    application: Application,
    @ApplicationContext private val context: Context,
    val healthConnectManager: HealthConnectManager
) : AndroidViewModel(application) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private val healthConnectCompatibleApps = healthConnectManager.healthConnectCompatibleApps
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        )




    var permissionsGranted = mutableStateOf(false)
        private set

    var backgroundReadAvailable = mutableStateOf(false)
        private set

    var backgroundReadGranted = mutableStateOf(false)
        private set

    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()


    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    fun initialLoad() {
        viewModelScope.launch {
            tryWithPermissionsCheck {
                WorkerRegistrar.registerOneTimeDataSyncWorker(context)
            }
        }
    }


    /**
     * Provides permission check and error handling for Health Connect suspend function calls.
     *
     * Permissions are checked prior to execution of [block], and if all permissions aren't granted
     * the [block] won't be executed, and [permissionsGranted] will be set to false, which will
     * result in the UI showing the permissions button.
     *
     * Where an error is caught, of the type Health Connect is known to throw, [uiState] is set to
     * [UiState.Error], which results in the snackbar being used to show the error message.
     */
    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
        backgroundReadAvailable.value = healthConnectManager.isFeatureAvailable(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
        )
        backgroundReadGranted.value = healthConnectManager.hasAllPermissions(
            setOf(PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
        )
        uiState = try {
            if (permissionsGranted.value) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    sealed class UiState {
        object Uninitialized : UiState()
        object Done : UiState()

        // A random UUID is used in each Error object to allow errors to be uniquely identified,
        // and recomposition won't result in multiple snackbars.
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }


    private val _showToast = MutableStateFlow(false)
    val showToast: StateFlow<Boolean> = _showToast

    fun requestPermission(healthConnectPermissionsLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>) {
        viewModelScope.launch {
            if (hasAllPermissionsEnabled()) {
                showToast()
            } else {
                healthConnectPermissionsLauncher.launch(permissions)
            }
        }
    }
    private suspend fun hasAllPermissionsEnabled(): Boolean {
        val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
        return grantedPermissions
            .containsAll(permissions)
    }

    private fun showToast() {
        _showToast.value = true
    }

    fun onToastShown() {
        _showToast.value = false
    }
}
