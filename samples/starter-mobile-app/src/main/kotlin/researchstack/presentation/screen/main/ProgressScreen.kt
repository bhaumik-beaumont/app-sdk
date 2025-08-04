package researchstack.presentation.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.marker.markerComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.marker.Marker
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.component.shape.Shapes
import researchstack.R
import researchstack.domain.model.priv.Bia
import researchstack.presentation.LocalNavController
import researchstack.presentation.viewmodel.ProgressViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val calories = viewModel.caloriesByDate.collectAsState().value
    val bia = viewModel.biaEntries.collectAsState().value
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color(0xFF222222),
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    text = stringResource(id = R.string.insights),
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = stringResource(id = R.string.calorie_burn_over_time), color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            CalorieChart(calories)
            Spacer(Modifier.height(24.dp))
            Text(text = stringResource(id = R.string.bia_progress), color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            BiaChart(bia)
        }
    }
}

@Composable
private fun CalorieChart(data: List<Pair<String, Float>>) {
    if (data.isEmpty()) {
        Text(text = stringResource(id = R.string.no_data_available), color = Color.White)
        return
    }
    val modelProducer = remember { ChartEntryModelProducer() }
    val marker = rememberSimpleMarker()
    LaunchedEffect(data) {
        modelProducer.setEntries(data.mapIndexed { index, pair -> entryOf(index.toFloat(), pair.second) })
    }
    val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        data.getOrNull(value.toInt())?.first ?: ""
    }
    val lineColor = Color(0xFF64B5F6)
    Chart(
        chart = lineChart(lines = listOf(lineSpec(lineColor, point = shapeComponent(Shapes.pillShape, lineColor)))),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(title = stringResource(R.string.kcal_unit)),
        bottomAxis = rememberBottomAxis(valueFormatter = formatter),
        marker = marker,
    )
}

@Composable
private fun BiaChart(entries: List<Bia>) {
    if (entries.isEmpty()) {
        Text(text = stringResource(id = R.string.no_data_available), color = Color.White)
        return
    }
    val modelProducer = remember { ChartEntryModelProducer() }
    val marker = rememberSimpleMarker()
    val dayFormatter = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()) }
    LaunchedEffect(entries) {
        val muscle = entries.mapIndexed { index, item -> entryOf(index.toFloat(), item.skeletalMuscleMass) }
        val fat = entries.mapIndexed { index, item -> entryOf(index.toFloat(), item.bodyFatRatio) }
        val water = entries.mapIndexed { index, item -> entryOf(index.toFloat(), item.totalBodyWater) }
        modelProducer.setEntries(listOf(muscle, fat, water))
    }
    val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        entries.getOrNull(value.toInt())?.timestamp?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dayFormatter)
        } ?: ""
    }
    val muscleColor = Color(0xFF81C784)
    val fatColor = Color(0xFFE57373)
    val waterColor = Color(0xFF64B5F6)
    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(muscleColor, point = shapeComponent(Shapes.pillShape, muscleColor)),
                lineSpec(fatColor, point = shapeComponent(Shapes.pillShape, fatColor)),
                lineSpec(waterColor, point = shapeComponent(Shapes.pillShape, waterColor)),
            )
        ),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = formatter),
        marker = marker,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendItem(muscleColor, stringResource(R.string.skeletal_muscle_mass))
        LegendItem(fatColor, stringResource(R.string.body_fat_percent))
        LegendItem(waterColor, stringResource(R.string.total_body_water))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun rememberSimpleMarker(): Marker {
    val label = com.patrykandpatrick.vico.compose.component.textComponent(
        color = Color.White,
        background = shapeComponent(Shapes.pillShape, Color.DarkGray)
    )
    val indicator = shapeComponent(Shapes.pillShape, Color.White)
    val guideline = lineComponent(Color.White.copy(alpha = 0.2f), 2.dp)
    return remember { markerComponent(label = label, indicator = indicator, guideline = guideline) }
}
