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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val activityCalories by viewModel.activityCalories.collectAsState()
    val resistanceCalories by viewModel.resistanceCalories.collectAsState()
    val activityProgress by viewModel.activityProgressPercent.collectAsState()
    val resistanceProgress by viewModel.resistanceProgressPercent.collectAsState()
    val hasData by viewModel.hasData.collectAsState()
    val weekStart by viewModel.weekStart.collectAsState()
    val canPrev by viewModel.canNavigatePrevious.collectAsState()
    val canNext by viewModel.canNavigateNext.collectAsState()
    val activityDetails by viewModel.activityDetails.collectAsState()
    val resistanceDetails by viewModel.resistanceDetails.collectAsState()
    val daysWithExercise by viewModel.daysWithExercise.collectAsState()

    var detailType by remember { mutableStateOf<DetailType?>(null) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault())
    val rangeFormatter = DateTimeFormatter.ofPattern("MMM d")
    val swipeThreshold = 100f
    var dragOffset by remember { mutableStateOf(0f) }
    val scrollState = rememberScrollState()

    if (detailType != null || selectedDay != null) {
        val exercises = when {
            selectedDay != null -> {
                val formatted = selectedDay!!.format(dayFormatter)
                (activityDetails + resistanceDetails).filter { it.date == formatted }
            }
            detailType == DetailType.Resistance -> resistanceDetails
            else -> activityDetails
        }
        val title = when {
            selectedDay != null -> stringResource(R.string.exercises_on_date, selectedDay!!.format(dayFormatter))
            detailType == DetailType.Resistance -> stringResource(R.string.resistance_details)
            else -> stringResource(R.string.activity_details)
        }
        ModalBottomSheet(
            onDismissRequest = {
                detailType = null
                selectedDay = null
            },
            sheetState = sheetState,
            containerColor = Color(0xFF222222),
            modifier = Modifier.fillMaxHeight()
        ) {
            ExerciseDetailSheet(
                title = title,
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
                .pointerInput(canPrev, canNext) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount
                        },
                        onDragEnd = {
                            when {
                                dragOffset > swipeThreshold && canPrev -> viewModel.navigateWeek(-1)
                                dragOffset < -swipeThreshold && canNext -> viewModel.navigateWeek(1)
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f }
                    )
                }
                .verticalScroll(scrollState)
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
            SwipeHint()
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
                    val hasExercise = daysWithExercise.contains(date)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isToday) Color.White else Color(0xFF333333))
                            .clickable {
                                selectedDay = date
                                detailType = null
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayName,
                                color = if (isToday) Color.Black else Color.White,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = dayNum,
                                color = if (isToday) Color.Black else Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (hasExercise) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00A86B), CircleShape)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
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
                        calories = activityCalories,
                        progressPercent = activityProgress,
                        color = Color(0xFF00A86B),
                        onClick = {
                            detailType = DetailType.Activity
                            selectedDay = null
                        }
                    )
                    ProgressCard(
                        title = stringResource(id = R.string.resistance),
                        minutes = resistanceMinutes,
                        calories = resistanceCalories,
                        progressPercent = resistanceProgress,
                        color = Color(0xFF3B82F6),
                        onClick = {
                            detailType = DetailType.Resistance
                            selectedDay = null
                        }
                    )
                }
            } else {
                EmptyState()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwipeHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = stringResource(id = R.string.swipe_to_change_week),
            color = Color.LightGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.EventBusy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = stringResource(id = R.string.no_activities_this_week),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
            Text(
                text = stringResource(id = R.string.try_sync_or_check_later),
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    minutes: Int,
    calories: Int,
    progressPercent: Int,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(Color(0xFF333333)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VerticalProgressBar(progressPercent, color, modifier = Modifier.fillMaxHeight())
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(
                            id = R.string.minutes_out_of,
                            minutes,
                            WeeklyProgressViewModel.ACTIVITY_GOAL_MINUTES
                        ),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${stringResource(id = R.string.calories)}: $calories ${stringResource(id = R.string.kcal_unit)}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(id = R.string.weekly_summary_helper),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(stringResource(id = R.string.show_details), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun VerticalProgressBar(progressPercent: Int, color: Color, modifier: Modifier = Modifier) {
    val progress = progressPercent.coerceIn(0, 100) / 100f
    Box(
        modifier = modifier
            .width(8.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF374151))
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

