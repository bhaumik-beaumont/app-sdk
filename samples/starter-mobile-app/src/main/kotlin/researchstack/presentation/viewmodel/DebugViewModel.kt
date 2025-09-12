package researchstack.presentation.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import researchstack.domain.usecase.study.GetJoinedStudiesUseCase
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    application: Application,
    private val getJoinedStudiesUseCase: GetJoinedStudiesUseCase,
) : AndroidViewModel(application) {

    private val _joinedStudies = MutableStateFlow<List<String>>(emptyList())
    val joinedStudies: StateFlow<List<String>> = _joinedStudies

    private val _grantedPermissions = MutableStateFlow<List<String>>(emptyList())
    val grantedPermissions: StateFlow<List<String>> = _grantedPermissions

    private val _notGrantedPermissions = MutableStateFlow<List<String>>(emptyList())
    val notGrantedPermissions: StateFlow<List<String>> = _notGrantedPermissions

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getJoinedStudiesUseCase().collect { studies ->
                _joinedStudies.value = studies.map { it.name }
            }
        }
        checkPermissions()
    }

    private fun checkPermissions() {
        val context = getApplication<Application>().applicationContext
        val pm = context.packageManager
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        val requested = packageInfo.requestedPermissions ?: emptyArray()
        val dangerous = requested.filter { perm ->
            try {
                val info = pm.getPermissionInfo(perm, 0)
                info.protectionLevel and PermissionInfo.PROTECTION_DANGEROUS != 0
            } catch (_: Exception) {
                false
            }
        }
        val granted = mutableListOf<String>()
        val denied = mutableListOf<String>()
        dangerous.forEach { perm ->
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm)
            } else {
                denied.add(perm)
            }
        }
        _grantedPermissions.value = granted
        _notGrantedPermissions.value = denied
    }
}

