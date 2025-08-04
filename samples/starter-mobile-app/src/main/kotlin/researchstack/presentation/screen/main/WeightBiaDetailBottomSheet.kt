package researchstack.presentation.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import researchstack.R
import researchstack.presentation.viewmodel.BiaDetailUi
import researchstack.presentation.viewmodel.WeightDetailUi

@Composable
fun WeightDetailSheet(title: String, entries: List<WeightDetailUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        if (entries.isEmpty()) {
            EmptyState(text = stringResource(id = R.string.no_weight_data_week))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries) { entry ->
                    WeightDetailItem(entry)
                }
            }
        }
    }
}

@Composable
fun BiaDetailSheet(title: String, entries: List<BiaDetailUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        if (entries.isEmpty()) {
            EmptyState(text = stringResource(id = R.string.no_bia_data_week))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries) { entry ->
                    BiaDetailItem(entry)
                }
            }
        }
    }
}

@Composable
private fun WeightDetailItem(detail: WeightDetailUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.timestamp_label), color = Color.LightGray, fontSize = 16.sp)
                Text(detail.timestamp, color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.weight), color = Color.LightGray, fontSize = 16.sp)
                Text(detail.weight, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun BiaDetailItem(detail: BiaDetailUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.timestamp_label), color = Color.LightGray, fontSize = 16.sp)
                Text(detail.timestamp, color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.skeletal_muscle_mass), color = Color.LightGray, fontSize = 16.sp)
                Text("${detail.skeletalMuscleMass} kg", color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.body_fat_percent), color = Color.LightGray, fontSize = 16.sp)
                Text("${detail.bodyFatPercent} %", color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.total_body_water), color = Color.LightGray, fontSize = 16.sp)
                Text("${detail.totalBodyWater} kg", color = Color.White, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(id = R.string.bmr), color = Color.LightGray, fontSize = 16.sp)
                Text("${detail.basalMetabolicRate} ${stringResource(id = R.string.kcal_unit)}", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.FitnessCenter,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.height(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(text = text, color = Color.White)
    }
}
