package researchstack.presentation.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import researchstack.presentation.viewmodel.DashboardViewModel

@Composable
fun AdherenceTabScreen(
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val activityMinutes by dashboardViewModel.totalDurationMinutes.collectAsState()
    val resistanceSessions by dashboardViewModel.resistanceExercises.collectAsState()
    val biaCount by dashboardViewModel.biaCount.collectAsState()
    val weightCount by dashboardViewModel.weightCount.collectAsState()
    val currentWeek by dashboardViewModel.currentWeek.collectAsState()
    val currentDay by dashboardViewModel.currentDay.collectAsState()
    val messages = remember(activityMinutes, resistanceSessions, biaCount, weightCount, currentWeek, currentDay) {
        val list = mutableListOf<String>()
        if (activityMinutes < DashboardViewModel.ACTIVITY_GOAL_MINUTES) {
            list.add("You’ve logged ${activityMinutes} minutes of activity. Target: 150 minutes per week.")
        }
        if (resistanceSessions.size < DashboardViewModel.RESISTANCE_SESSION_GOAL) {
            list.add("You’ve completed ${resistanceSessions.size} resistance session(s). Goal: 2 sessions per week.")
        }
        val showBaseline = currentWeek == 1 && currentDay <= 7 && biaCount == 0 && weightCount == 0
        if (showBaseline) {
            list.add("Reminder: Complete BIA or weight reading by Day 7 as part of your clinic baseline requirement.")
        }
        if (list.isEmpty()) {
            list.add("You're all set for this week. Keep up the great work!")
        }
        list
    }

    LazyColumn(
        modifier = Modifier.padding(bottom = 80.dp)
    ) {
        items(messages) { message ->
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(Color(0xFF333333)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

