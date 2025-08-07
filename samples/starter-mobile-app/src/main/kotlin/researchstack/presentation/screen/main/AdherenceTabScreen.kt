import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import researchstack.presentation.viewmodel.DashboardViewModel
import researchstack.util.WEEKLY_ACTIVITY_GOAL_MINUTES
import researchstack.util.WEEKLY_RESISTANCE_SESSION_COUNT

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

    val messages = remember(
        activityMinutes, resistanceSessions, biaCount, weightCount, currentWeek, currentDay
    ) {
        val list = mutableListOf<String>()
        if (activityMinutes < WEEKLY_ACTIVITY_GOAL_MINUTES) {
            list.add("You’ve logged $activityMinutes minutes of activity. Target: $WEEKLY_ACTIVITY_GOAL_MINUTES minutes per week.")
        }
        if (resistanceSessions.size < WEEKLY_RESISTANCE_SESSION_COUNT) {
            list.add("You’ve completed ${resistanceSessions.size} resistance session(s). Goal: $WEEKLY_RESISTANCE_SESSION_COUNT sessions per week.")
        }
        val showBaseline = currentWeek == 1 && biaCount == 0 && weightCount == 0
        if (showBaseline) {
            list.add("Reminder: Complete BIA or weight reading by Day 7 as part of your clinic baseline requirement.")
        }
        if (list.isEmpty()) {
            list.add("You're all set for this week. Keep up the great work!")
        }
        list
    }

    Column(
        modifier = Modifier.padding(bottom = 80.dp) // Add bottom padding to prevent hiding behind bottom nav
    ) {
        messages.forEach { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
