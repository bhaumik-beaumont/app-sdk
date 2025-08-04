package researchstack.presentation.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import researchstack.presentation.viewmodel.ProgressViewModel
import researchstack.presentation.viewmodel.CaloriePoint
import researchstack.presentation.LocalNavController
import androidx.compose.ui.res.stringResource
import com.patrykandpatrick.vico.compose.m3.Chart
import com.patrykandpatrick.vico.compose.m3.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.m3.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.m3.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.chart.line.rememberLineSpec
import com.patrykandpatrick.vico.compose.m3.marker.rememberMarker
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import researchstack.R
import researchstack.domain.model.priv.Bia

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val calories by viewModel.caloriePoints.collectAsState()
    val biaList by viewModel.biaEntries.collectAsState()
    val scrollState = rememberScrollState()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

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
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(id = R.string.insights),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.calorie_burn_over_time),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CalorieChart(calories, dateFormatter)
            Text(
                text = stringResource(id = R.string.calories_unit_full),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(id = R.string.bia_progress),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BiaChart(biaList, dateFormatter)
        }
    }
}

@Composable
private fun CalorieChart(points: List<CaloriePoint>, formatter: DateTimeFormatter) {
    val modelProducer = remember { ChartEntryModelProducer() }
    val labels = points.map { it.date.format(formatter) }
    LaunchedEffect(points) {
        val entries = points.mapIndexed { index, p -> FloatEntry(index.toFloat(), p.calories.toFloat()) }
        modelProducer.setEntries(entries)
    }
    Chart(
        chart = lineChart(),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(valueFormatter = { value, _ -> value.toInt().toString() }),
        bottomAxis = rememberBottomAxis(valueFormatter = { value, _ -> labels.getOrNull(value.toInt()) ?: "" }),
        marker = rememberMarker(),
    )
}

@Composable
private fun BiaChart(list: List<Bia>, formatter: DateTimeFormatter) {
    val modelProducer = remember { ChartEntryModelProducer() }
    val labels = list.map { Instant.ofEpochMilli(it.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter) }
    LaunchedEffect(list) {
        val muscle = list.mapIndexed { index, b -> FloatEntry(index.toFloat(), b.skeletalMuscleMass) }
        val fat = list.mapIndexed { index, b -> FloatEntry(index.toFloat(), b.bodyFatRatio * 100f) }
        val water = list.mapIndexed { index, b -> FloatEntry(index.toFloat(), b.totalBodyWater) }
        modelProducer.setEntries(listOf(muscle, fat, water))
    }
    Chart(
        chart = lineChart(
            lines = listOf(
                rememberLineSpec(color = Color(0xFF4CAF50)),
                rememberLineSpec(color = Color(0xFFF44336)),
                rememberLineSpec(color = Color(0xFF03A9F4)),
            )
        ),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(valueFormatter = { value, _ -> value.toInt().toString() }),
        bottomAxis = rememberBottomAxis(valueFormatter = { value, _ -> labels.getOrNull(value.toInt()) ?: "" }),
        marker = rememberMarker(),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(Color(0xFF4CAF50), stringResource(id = R.string.skeletal_muscle_mass))
        LegendItem(Color(0xFFF44336), stringResource(id = R.string.body_fat_percent))
        LegendItem(Color(0xFF03A9F4), stringResource(id = R.string.total_body_water))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}
