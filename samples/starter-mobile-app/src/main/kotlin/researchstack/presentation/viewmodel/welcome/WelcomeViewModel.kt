package researchstack.presentation.viewmodel.welcome

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import researchstack.R
import researchstack.auth.domain.model.Authentication
import researchstack.auth.domain.model.BasicAuthentication
import researchstack.auth.domain.model.OidcAuthentication
import researchstack.auth.domain.repository.AuthRepository
import researchstack.auth.domain.usecase.CheckSignInUseCase
import researchstack.auth.domain.usecase.ClearAccountUseCase
import researchstack.auth.domain.usecase.GetAccountUseCase
import researchstack.auth.domain.usecase.SignInUseCase
import researchstack.data.datasource.local.pref.EnrollmentDatePref
import researchstack.domain.exception.AlreadyExistedUserException
import researchstack.domain.model.UserProfileModel
import researchstack.domain.usecase.ReLoginUseCase
import researchstack.domain.usecase.profile.GetProfileUseCase
import researchstack.domain.usecase.profile.RegisterProfileUseCase
import researchstack.domain.validator.EmailValidator
import researchstack.domain.validator.LoginInputNormalizer
import researchstack.domain.validator.PasswordValidator
import researchstack.presentation.worker.FetchStudyWorker
import researchstack.presentation.worker.WorkerRegistrar.registerAllPeriodicWorkers
import java.time.LocalDate
import javax.inject.Inject

private val anonymousUser = UserProfileModel(
    firstName = "",
    lastName = "",
    birthday = LocalDate.now(),
    email = "",
    phoneNumber = "",
    address = ""
)

/** UI state for the login screen. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null
)

/** Events that the [LoginViewModel] can react to. */
sealed class LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent()
    data class PasswordChanged(val value: String) : LoginEvent()
    object Submit : LoginEvent()
}

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val signInUseCase: SignInUseCase,
    private val registerProfileUseCase: RegisterProfileUseCase,
    private val reLoginUseCase: ReLoginUseCase,
    private val clearAccountUseCase: ClearAccountUseCase,
    private val getAccountUseCase: GetAccountUseCase,
    private val checkSignInUseCase: CheckSignInUseCase,
    private val getProfileUseCase: GetProfileUseCase,
) : AndroidViewModel(application) {
    sealed class RegisterState
    object None : RegisterState()
    object Registering : RegisterState()
    object Success : RegisterState()
    class Fail(val message: String) : RegisterState()

    val authType = authRepository.getAuthType()

    val authProvider = authRepository.getProvider()

    private val _registerState = MutableStateFlow<RegisterState>(None)
    val registerState: StateFlow<RegisterState> = _registerState

    private val _hasAccount = MutableStateFlow<Boolean?>(null)
    val hasAccount: StateFlow<Boolean?> = _hasAccount

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()


    /** Handles user [event]. */
    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _uiState.update {
                it.copy(
                    email = event.value,
                    emailError = null
                )
            }

            is LoginEvent.PasswordChanged -> _uiState.update {
                it.copy(
                    password = event.value,
                    passwordError = null
                )
            }

            LoginEvent.Submit -> validateAndLogin()
        }
    }

    private fun validateAndLogin() {
        val normalizedEmail = LoginInputNormalizer.normalize(_uiState.value.email)
//        _uiState.update { it.copy(email = normalizedEmail) }

        val emailResult = EmailValidator.validate(normalizedEmail)
        val passwordResult = PasswordValidator.validate(_uiState.value.password)

        _uiState.update {
            it.copy(
                emailError = emailResult.errorMessage,
                passwordError = passwordResult.errorMessage
            )
        }

        val firstError = emailResult.errorMessage ?: passwordResult.errorMessage
        if (firstError == null) {
            registerUser(BasicAuthentication(normalizedEmail, _uiState.value.password))
        }
    }

    private fun handleSamsungAccountFailure(ex: Throwable) {
        if (ex == AlreadyExistedUserException) {
            // NOTE the re-login process is not supported yet
            // should get joiend study and subject id and should set data permission
            _registerState.value = Success
            return
        }
        _registerState.value = Fail(getApplication<Application>().getString(R.string.require_samsung_account_login))
    }

    private suspend fun handleFailure(ex: Throwable) {
        Log.e(WelcomeViewModel::class.simpleName, ex.stackTraceToString())
        Log.e(WelcomeViewModel::class.simpleName, "fail to register profile")
        clearAccountUseCase()
        if (ex == AlreadyExistedUserException) {
            _registerState.value = Fail(getApplication<Application>().getString(R.string.already_registered_user))
            return
        }
        _registerState.value = Fail(getApplication<Application>().getString(R.string.fail_to_signin))
    }

    fun registerUser(auth: Authentication) {
        _registerState.value = Registering
        viewModelScope.launch(Dispatchers.IO) {
            signInUseCase(auth)
                .mapCatching {
                    val email = _uiState.value.email
                    getProfileUseCase()
                        .mapCatching { profile ->
                            val enrolDate = if (profile.enrolmentDate.isNullOrEmpty()) {
                                LocalDate.now().toString()
                            } else {
                                profile.enrolmentDate
                            }

                            registerProfileUseCase(
                                anonymousUser.copy(
                                    firstName = email.substringBefore("@"),
                                    email = email,
                                    enrolmentDate = enrolDate
                                )
                            )
                        }
                        .onFailure { exception ->
                            // This block is called if getProfileUseCase() returned Result.Failure
                            // For example: first-time user, or profile fetch error

                            registerProfileUseCase(
                                anonymousUser.copy(
                                    firstName = email.substringBefore("@"),
                                    email = email,
                                    enrolmentDate = LocalDate.now().toString()
                                )
                            )
                        }
                }.onSuccess {
                    _registerState.value = Success
                    fetchStudy()
                    registerAllPeriodicWorkers(getApplication<Application>().applicationContext)
                }.onFailure { ex ->
                    if (auth is OidcAuthentication) {
                        handleSamsungAccountFailure(ex)
                    } else {
                        handleFailure(ex)
                    }
                }
        }
    }

    private fun fetchStudy() {
        WorkManager
            .getInstance(getApplication<Application>().applicationContext)
            .enqueue(
                OneTimeWorkRequestBuilder<FetchStudyWorker>()
                    .build()
            )
    }
}
