package researchstack.presentation.viewmodel

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _requestHealthConnectInstall = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val requestHealthConnectInstall: SharedFlow<Unit> =
        _requestHealthConnectInstall.asSharedFlow()

    fun setStartRouteDestination(shouldPromptInstall: Boolean = true): Boolean {
        if (!ensureHealthConnectAvailable(shouldPromptInstall)) {
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

    private fun ensureHealthConnectAvailable(shouldPromptInstall: Boolean): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        val isAvailable = status == HealthConnectClient.SDK_AVAILABLE
        if (!isAvailable && shouldPromptInstall) {
            _requestHealthConnectInstall.tryEmit(Unit)
        }
        return isAvailable
    }
}
