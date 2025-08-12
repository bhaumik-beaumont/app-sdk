package researchstack.presentation.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.component.marker.markerComponent
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.marker.MarkerLabelFormatter
import researchstack.R
import researchstack.presentation.LocalNavController
import researchstack.presentation.viewmodel.ChartEntry
import researchstack.presentation.viewmodel.ProgressViewModel
import researchstack.presentation.util.toDecimalFormat

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val calories = viewModel.caloriesByDate.collectAsState().value
    val muscleData = viewModel.muscleMassByDate.collectAsState().value
    val fatMassData = viewModel.fatMassByDate.collectAsState().value
    val fatFreeData = viewModel.fatFreeMassByDate.collectAsState().value
    val weight = viewModel.weightByDate.collectAsState().value
    val isMetric = viewModel.isMetricUnit.collectAsState().value
    val scrollState = rememberScrollState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color(0xFF222222),
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(16.dp)
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
//            Spacer(Modifier.height(8.dp))
//            CalorieChart(calories)
            Spacer(Modifier.height(24.dp))
            Text(text = stringResource(id = R.string.bia_progress), color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            val unit = if (isMetric) stringResource(R.string.kg_unit) else stringResource(R.string.lbs_unit)
            BiaMetricChart(
                title = stringResource(R.string.skeletal_muscle_mass) + " ($unit)",
                data = muscleData,
                unit = unit,
                lineColor = Color(0xFF81C784)
            )
            Spacer(Modifier.height(16.dp))
            BiaMetricChart(
                title = stringResource(R.string.body_fat_mass) + " ($unit)",
                data = fatMassData,
                unit = unit,
                lineColor = Color(0xFFE57373)
            )
            Spacer(Modifier.height(16.dp))
            BiaMetricChart(
                title = stringResource(R.string.fat_free_mass) + " ($unit)",
                data = fatFreeData,
                unit = unit,
                lineColor = Color(0xFF64B5F6)
            )
            Spacer(Modifier.height(24.dp))
//            Text(text = stringResource(id = R.string.weight_progress), color = Color.White, fontSize = 18.sp)
//            Spacer(Modifier.height(8.dp))
//            BiaMetricChart(
//                title = stringResource(R.string.weight) + " ($unit)",
//                data = weight,
//                unit = unit,
//                lineColor = Color(0xFFFFB74D)
//            )
        }
    }
}

@Composable
private fun CalorieChart(data: List<ChartEntry>) {
    if (data.isEmpty()) {
        Text(text = stringResource(id = R.string.no_data_available), color = Color.White)
        return
    }
    val modelProducer = remember { ChartEntryModelProducer() }
    val marker = rememberSimpleMarker()
    LaunchedEffect(data) {
        modelProducer.setEntries(data.mapIndexed { index, entry -> entryOf(index.toFloat(), entry.value) })
    }
    val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        data.getOrNull(value.toInt())?.label ?: ""
    }
    val lineColor = Color(0xFF64B5F6)
    Chart(
        chart = lineChart(lines = listOf(lineSpec(lineColor, point = shapeComponent(Shapes.pillShape, lineColor)))),
        chartModelProducer = modelProducer,
        startAxis = rememberStartAxis(title = stringResource(R.string.kcal_unit)),
        bottomAxis = rememberBottomAxis(valueFormatter = formatter, labelRotationDegrees = 90f),
        marker = marker,
    )
}

@Composable
private fun BiaMetricChart(title: String, data: List<ChartEntry>, unit: String, lineColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (data.isEmpty()) {
                Text(text = stringResource(id = R.string.no_data_available), color = Color.White)
            } else {
                val modelProducer = remember { ChartEntryModelProducer() }
                val marker = rememberBiaMarker(data, unit)
                LaunchedEffect(data) {
                    modelProducer.setEntries(data.mapIndexed { index, entry -> entryOf(index.toFloat(), entry.value) })
                }
                val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    data.getOrNull(value.toInt())?.label ?: ""
                }
                Chart(
                    chart = lineChart(lines = listOf(lineSpec(lineColor, point = shapeComponent(Shapes.pillShape, lineColor)))),
                    chartModelProducer = modelProducer,
                    startAxis = rememberStartAxis(title = unit),
                    bottomAxis = rememberBottomAxis(valueFormatter = formatter, labelRotationDegrees = 90f),
                    marker = marker,
                )
            }
        }
    }
}

@Composable
private fun rememberSimpleMarker(): Marker {
    val label = textComponent(
        color = Color.White,
        background = shapeComponent(Shapes.pillShape, Color.DarkGray)
    )
    val indicator = shapeComponent(Shapes.pillShape, Color.White)
    val guideline = lineComponent(Color.White.copy(alpha = 0.2f), 2.dp)
    return markerComponent(
        label = label,
        indicator = indicator,
        guideline = guideline
    )
}

@Composable
private fun rememberBiaMarker(data: List<ChartEntry>, unit: String): Marker {
    val label = textComponent(
        color = Color.White,
        background = shapeComponent(Shapes.pillShape, Color.DarkGray)
    )
    val indicator = shapeComponent(Shapes.pillShape, Color.White)
    val guideline = lineComponent(Color.White.copy(alpha = 0.2f), 2.dp)
    return markerComponent(
        label = label,
        indicator = indicator,
        guideline = guideline,
    ).apply {
        labelFormatter = MarkerLabelFormatter { markedEntries, _ ->
            val index = markedEntries.firstOrNull()?.entry?.x?.toInt() ?: 0
            val label = data.getOrNull(index)?.label ?: ""
            val value = markedEntries.firstOrNull()?.entry?.y?.toFloat() ?: 0f
            "$label ${value.toDecimalFormat(2)} $unit"
        }
    }
}
