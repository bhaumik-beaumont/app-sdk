package researchstack.presentation.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import researchstack.R
import researchstack.presentation.LocalNavController
import researchstack.presentation.viewmodel.WeeklyProgressViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyProgressScreen(
    viewModel: WeeklyProgressViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val weekDays by viewModel.weekDays.collectAsState()
    val activityMinutes by viewModel.activityMinutes.collectAsState()
    val resistanceMinutes by viewModel.resistanceMinutes.collectAsState()
    val activityProgress by viewModel.activityProgressPercent.collectAsState()
    val resistanceProgress by viewModel.resistanceProgressPercent.collectAsState()
    val hasData by viewModel.hasData.collectAsState()
    val weekStart by viewModel.weekStart.collectAsState()
    val canPrev by viewModel.canNavigatePrevious.collectAsState()
    val canNext by viewModel.canNavigateNext.collectAsState()
    val activityDetails by viewModel.activityDetails.collectAsState()
    val resistanceDetails by viewModel.resistanceDetails.collectAsState()

    var detailType by remember { mutableStateOf<DetailType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val rangeFormatter = DateTimeFormatter.ofPattern("MMM d")

    if (detailType != null) {
        val exercises = if (detailType == DetailType.Resistance) {
            resistanceDetails
        } else {
            activityDetails
        }
        ModalBottomSheet(
            onDismissRequest = { detailType = null },
            sheetState = sheetState,
            containerColor = Color(0xFF222222),
            modifier = Modifier.fillMaxHeight()
        ) {
            ExerciseDetailSheet(
                title = if (detailType == DetailType.Resistance)
                    stringResource(R.string.resistance_details)
                else
                    stringResource(R.string.activity_details),
                exercises = exercises
            )
        }
    }

    Scaffold(
        containerColor = Color(0xFF222222),
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp)
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
                    text = stringResource(id = R.string.weekly_progress),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateWeek(-1) }, enabled = canPrev) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = null,
                        tint = if (canPrev) Color.White else Color.Gray
                    )
                }
                Text(
                    text = weekStart.format(rangeFormatter) + " - " + weekStart.plusDays(6).format(rangeFormatter),
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { viewModel.navigateWeek(1) }, enabled = canNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = if (canNext) Color.White else Color.Gray
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEach { date ->
                    val isToday = date == today
                    val dayName = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                    val dayNum = date.format(DateTimeFormatter.ofPattern("dd"))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isToday) Color.White else Color(0xFF333333),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = dayName,
                            color = if (isToday) Color.Black else Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = dayNum,
                            color = if (isToday) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (hasData) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProgressCard(
                        title = stringResource(id = R.string.activity),
                        minutes = activityMinutes,
                        progressPercent = activityProgress,
                        color = Color(0xFF00A86B),
                        onClick = { detailType = DetailType.Activity }
                    )
                    ProgressCard(
                        title = stringResource(id = R.string.resistance),
                        minutes = resistanceMinutes,
                        progressPercent = resistanceProgress,
                        color = Color(0xFFFFD700),
                        onClick = { detailType = DetailType.Resistance }
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.no_data_available),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    minutes: Int,
    progressPercent: Int,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(Color(0xFF333333)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VerticalProgressBar(progressPercent, color)
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    stringResource(
                        id = R.string.minutes_out_of,
                        minutes,
                        WeeklyProgressViewModel.ACTIVITY_GOAL_MINUTES
                    ),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun VerticalProgressBar(progressPercent: Int, color: Color) {
    val progress = progressPercent.coerceIn(0, 100) / 100f
    Box(
        modifier = Modifier
            .width(8.dp)
            .height(80.dp)
            .background(Color(0xFF374151), RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(progress)
                .align(Alignment.BottomCenter)
                .background(color, RoundedCornerShape(50))
        )
    }
}

private enum class DetailType { Activity, Resistance }

