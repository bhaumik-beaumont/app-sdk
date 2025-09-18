package researchstack.presentation.viewmodel

import android.content.Context
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

    fun setStartRouteDestination(): Boolean {
        if (!ensureHealthConnectAvailable()) {
            _isReady.postValue(true)
            _routeDestination.postValue(null)
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

    private fun ensureHealthConnectAvailable(): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        val isAvailable = status == HealthConnectClient.SDK_AVAILABLE
        _healthConnectAvailable.postValue(isAvailable)
        return isAvailable
    }
}
