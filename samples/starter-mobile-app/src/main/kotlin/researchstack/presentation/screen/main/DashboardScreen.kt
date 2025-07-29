package researchstack.presentation.screen.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import researchstack.R
import researchstack.presentation.LocalNavController
import researchstack.presentation.component.ComplianceSummaryCard
import researchstack.presentation.initiate.route.Route
import researchstack.presentation.screen.notification.NotificationViewModel
import researchstack.presentation.viewmodel.HealthConnectPermissionViewModel
import researchstack.presentation.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    healthConnectPermissionViewModel: HealthConnectPermissionViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val weeklyTab = stringResource(id = R.string.weekly)
    var activeTab by remember { mutableStateOf(weeklyTab) }
    var showSyncDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val navController = LocalNavController.current
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val hasUnread by notificationViewModel.hasUnread.collectAsState()
    val exercises by dashboardViewModel.exercises.collectAsState()
    val totalDuration by dashboardViewModel.totalDurationMinutes.collectAsState()
    val activityProgressPercent by dashboardViewModel.activityProgressPercent.collectAsState()

    val activityDurationDisplay = remember(totalDuration) {
        val hours = totalDuration / 60
        val minutes = totalDuration % 60
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(healthConnectPermissionViewModel.permissionsLauncher) {
            healthConnectPermissionViewModel.initialLoad()
        }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Fixed top row
            Row(
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxWidth()
                    .padding(all = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(Color(0xFF00CED1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = stringResource(id = R.string.bank),
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        permissionsLauncher.launch(healthConnectPermissionViewModel.permissions)
                    }) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = stringResource(id = R.string.sync),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    NotificationBell(hasUnread) {
                        navController.navigate(Route.Notifications.name)
                    }
                }
            }

            // Scrollable content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 16.dp)
                ) {
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 0.dp),
                        colors = CardDefaults.cardColors(Color(0xFFFFA500)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            stringResource(id = R.string.almost_there),
                            Modifier.padding(16.dp),
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Compliance Summary Cards (Refactored)
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Text(
                            stringResource(id = R.string.compliance_summary),
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ComplianceSummaryCard(
                                    color = Color(0xFF2B9179),
                                    title = stringResource(id = R.string.activity),
                                    stats = activityDurationDisplay,
                                    modifier = Modifier.weight(1f)
                                )
                                ComplianceSummaryCard(
                                    color = Color(0xFF287CC3),
                                    title = stringResource(id = R.string.resistance),
                                    stats = stringResource(id = R.string.sessions),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ComplianceSummaryCard(
                                    color = Color(0xFF6C43CD),
                                    title = stringResource(id = R.string.weight),
                                    stats = stringResource(id = R.string.lbs),
                                    modifier = Modifier.weight(1f)
                                )
                                ComplianceSummaryCard(
                                    color = Color(0xFFAB369F),
                                    title = stringResource(id = R.string.bia),
                                    stats = "0%",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            TabButton(
                                stringResource(id = R.string.weekly),
                                activeTab == stringResource(id = R.string.weekly)
                            ) {
                            }
                            TabButton(
                                stringResource(id = R.string.adherence),
                                activeTab == stringResource(id = R.string.adherence)
                            ) {
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        if (activeTab == stringResource(id = R.string.weekly)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.this_week), color = Color.White)
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = "Dropdown",
                                    tint = Color.White
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.padding(bottom = 80.dp) // Add bottom padding to prevent hiding behind bottom nav
                            ) {
                                ProgressBarItem(
                                    stringResource(id = R.string.activity),
                                    activityProgressPercent,
                                    Color(0xFF00A86B)
                                )
                                ProgressBarItem(
                                    stringResource(id = R.string.resistance),
                                    0,
                                    Color(0xFFFFD700)
                                )
                                ProgressBarItem(
                                    stringResource(id = R.string.weight),
                                    0,
                                    Color(0xFFFF6347)
                                )
                                ProgressBarItem(
                                    stringResource(id = R.string.bia),
                                    0,
                                    Color(0xFFFFA500)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSyncDialog) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(id = R.string.device_sync),
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(id = R.string.last_synced),
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showSyncDialog = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF3F4F6), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(id = R.string.close),
                                    tint = Color.Gray
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { /* Handle Sync */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF4169E1
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(id = R.string.sync_now), color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onClick) {
            Text(
                label,
                color = if (selected) Color.White else Color(0xFFB0B0B0),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (selected) {
            // Indicator: fixed width, centered
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .width(70.dp)
                    .height(2.dp)
                    .background(Color.White, shape = RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
fun ProgressBarItem(label: String, progressPercent: Int, color: Color) {
    val progressFraction = (progressPercent.coerceIn(0, 100)) / 100f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 15.sp)
            Text("${progressPercent}%", color = Color.White, fontSize = 15.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFF374151), RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun NotificationBell(hasUnread: Boolean, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = stringResource(id = R.string.bell),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        if (hasUnread) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
                    .align(Alignment.TopEnd)
            )
        }
    }
}
