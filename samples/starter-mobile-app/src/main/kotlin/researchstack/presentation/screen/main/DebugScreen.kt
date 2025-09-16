package researchstack.presentation.screen.main

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import researchstack.R
import researchstack.presentation.LocalNavController
import researchstack.presentation.viewmodel.DebugViewModel
import researchstack.presentation.viewmodel.HealthConnectPermissionViewModel

@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel(),
    healthConnectPermissionViewModel: HealthConnectPermissionViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val joinedStudies = viewModel.joinedStudies.collectAsState().value
    val grantedPermissions = viewModel.grantedPermissions.collectAsState().value
    val notGrantedPermissions = viewModel.notGrantedPermissions.collectAsState().value
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var missingHealthPermissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var samsungPermissionsGranted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        missingHealthPermissions = healthConnectPermissionViewModel.getMissingPermissions()
        samsungPermissionsGranted = healthConnectPermissionViewModel.areSamsungPermissionsGranted()
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(healthConnectPermissionViewModel.permissionsLauncher) {
            coroutineScope.launch {
                val missing = healthConnectPermissionViewModel.getMissingPermissions()
                missingHealthPermissions = missing
                if (missing.isEmpty()) {
                    (context as? Activity)?.let { activity ->
                        healthConnectPermissionViewModel.checkSamsungPermissions(activity)
                        samsungPermissionsGranted =
                            healthConnectPermissionViewModel.areSamsungPermissionsGranted()
                    }
                }
            }
        }

    Scaffold(
        containerColor = Color(0xFF222222),
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.close),
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(id = R.string.debug),
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(
                    id = R.string.debug_joined_studies,
                    if (joinedStudies.isEmpty()) "None" else joinedStudies.joinToString()
                ),
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    id = R.string.debug_granted_permissions,
                    if (grantedPermissions.isEmpty()) "None" else grantedPermissions.joinToString()
                ),
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    id = R.string.debug_not_granted_permissions,
                    if (notGrantedPermissions.isEmpty()) "None" else notGrantedPermissions.joinToString()
                ),
                color = Color.White,
                fontSize = 16.sp
            )
            if (missingHealthPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    permissionsLauncher.launch(healthConnectPermissionViewModel.allPermissions)
                }) {
                    Text(text = stringResource(id = R.string.grant_health_permissions))
                }
            }
            if (!samsungPermissionsGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    (context as? Activity)?.let { activity ->
                        healthConnectPermissionViewModel.checkSamsungPermissions(activity)
                        coroutineScope.launch {
                            samsungPermissionsGranted = healthConnectPermissionViewModel.areSamsungPermissionsGranted()
                        }
                    }
                }) {
                    Text(text = stringResource(id = R.string.grant_samsung_health_permissions))
                }
            }
        }
    }
}

