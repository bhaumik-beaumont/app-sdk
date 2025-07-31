package researchstack.presentation.screen.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import researchstack.R
import researchstack.presentation.viewmodel.ExerciseDetailUi

@Composable
fun ExerciseDetailSheet(title: String, exercises: List<ExerciseDetailUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        if (exercises.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_activity_data_week),
                color = Color.White
            )
        } else {
            LazyColumn {
                items(exercises) { exercise ->
                    ExerciseDetailItem(exercise)
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailItem(detail: ExerciseDetailUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(detail.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${detail.startTime} - ${detail.endTime}", color = Color.White, fontSize = 14.sp)
        Text(
            text = stringResource(id = R.string.duration_minutes, detail.durationMinutes),
            color = Color.White,
            fontSize = 14.sp
        )
        Text("${stringResource(id = R.string.calories)}: ${detail.calories}", color = Color.White, fontSize = 14.sp)
        Text("${stringResource(id = R.string.min_hr)}: ${detail.minHeartRate}", color = Color.White, fontSize = 14.sp)
        Text("${stringResource(id = R.string.max_hr)}: ${detail.maxHeartRate}", color = Color.White, fontSize = 14.sp)
    }
}
