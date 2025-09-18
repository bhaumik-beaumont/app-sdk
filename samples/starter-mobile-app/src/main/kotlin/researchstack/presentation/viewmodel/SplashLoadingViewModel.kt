package researchstack.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import researchstack.auth.domain.usecase.CheckSignInUseCase
import researchstack.domain.usecase.study.GetJoinedStudiesUseCase
import researchstack.presentation.initiate.route.Route
import javax.inject.Inject

@HiltViewModel
class SplashLoadingViewModel @Inject constructor(
    private val checkSignInUseCase: CheckSignInUseCase,
    private val getJoinedStudiesUseCase: GetJoinedStudiesUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private var _isReady: MutableLiveData<Boolean> = MutableLiveData(false)
    val isReady: LiveData<Boolean>
        get() = _isReady

    private var _routeDestination: MutableLiveData<Route?> = MutableLiveData(null)
    val routeDestination: LiveData<Route?>
        get() = _routeDestination

    var startMainPage = MutableLiveData<Int>(0)
        private set

    private val _healthConnectAvailable = MutableLiveData<Boolean?>(null)
    val healthConnectAvailable: LiveData<Boolean?>
        get() = _healthConnectAvailable

    private val _samsungHealthAvailable = MutableLiveData<Boolean?>(null)
    val samsungHealthAvailable: LiveData<Boolean?>
        get() = _samsungHealthAvailable

    fun setStartRouteDestination(): Boolean {
        if (!ensureRequiredHealthAppsAvailable()) {
            _routeDestination.postValue(Route.HealthConnectUnavailable)
            _isReady.postValue(true)
            return false
        }
        viewModelScope.launch {
            val startRoute = if (checkSignInUseCase().getOrDefault(false)) Route.Main
            else Route.Intro

            _routeDestination.postValue(startRoute)
            _isReady.postValue(true)
        }
        return true
    }

    fun setStartMainPage() {
        viewModelScope.launch {
            startMainPage.value = if (getJoinedStudiesUseCase().first().isEmpty()) 1
            else 0
        }
    }

    private fun ensureRequiredHealthAppsAvailable(): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        val isHealthConnectAvailable = status == HealthConnectClient.SDK_AVAILABLE
        val isSamsungHealthAvailable = isSamsungHealthInstalled()

        _healthConnectAvailable.postValue(isHealthConnectAvailable)
        _samsungHealthAvailable.postValue(isSamsungHealthAvailable)

        return isHealthConnectAvailable && isSamsungHealthAvailable
    }

    private fun isSamsungHealthInstalled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    SAMSUNG_HEALTH_PACKAGE_NAME,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(SAMSUNG_HEALTH_PACKAGE_NAME, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        private const val SAMSUNG_HEALTH_PACKAGE_NAME = "com.sec.android.app.shealth"
    }
}
