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

    private val _hasJoinedStudy = MutableStateFlow(false)
    val hasJoinedStudy: StateFlow<Boolean> = _hasJoinedStudy

    private val _allPermissionsGranted = MutableStateFlow(false)
    val allPermissionsGranted: StateFlow<Boolean> = _allPermissionsGranted

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getJoinedStudiesUseCase().collect { studies ->
                _hasJoinedStudy.value = studies.isNotEmpty()
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
        _allPermissionsGranted.value = dangerous.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }
}

