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
import researchstack.util.MINIMUM_BIA_ENTRIES_PER_WEEK
import researchstack.util.MINIMUM_WEIGHT_ENTRIES_PER_WEEK

@Composable
fun AdherenceTabScreen(
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val activityMinutes by dashboardViewModel.totalDurationMinutes.collectAsState()
    val resistanceSessions by dashboardViewModel.resistanceExercises.collectAsState()
    val biaCount by dashboardViewModel.biaCount.collectAsState()
    val weightCount by dashboardViewModel.weightCount.collectAsState()
    val currentWeek by dashboardViewModel.currentWeek.collectAsState()

    val messages = remember(
        activityMinutes, resistanceSessions, biaCount, weightCount, currentWeek
    ) {
        data class ComplianceMessage(val order: Int, val compliant: Boolean, val message: String)

        val items = mutableListOf<ComplianceMessage>()

        val activityMessage =
            "You’ve logged $activityMinutes minutes of activity. Target: $WEEKLY_ACTIVITY_GOAL_MINUTES minutes per week."
        items += ComplianceMessage(
            order = 1,
            compliant = activityMinutes >= WEEKLY_ACTIVITY_GOAL_MINUTES,
            message = activityMessage
        )

        val resistanceMessage =
            "You’ve completed ${resistanceSessions.size} resistance session(s). Goal: $WEEKLY_RESISTANCE_SESSION_COUNT sessions per week."
        items += ComplianceMessage(
            order = 2,
            compliant = resistanceSessions.size >= WEEKLY_RESISTANCE_SESSION_COUNT,
            message = resistanceMessage
        )

        val biaMessage =
            "You’ve logged $biaCount BIA measurement(s). Goal: $MINIMUM_BIA_ENTRIES_PER_WEEK per week."
        items += ComplianceMessage(
            order = 3,
            compliant = biaCount >= MINIMUM_BIA_ENTRIES_PER_WEEK,
            message = biaMessage
        )

        val weightMessage =
            "You’ve logged $weightCount weight entry(ies). Goal: $MINIMUM_WEIGHT_ENTRIES_PER_WEEK per week."
        items += ComplianceMessage(
            order = 4,
            compliant = weightCount >= MINIMUM_WEIGHT_ENTRIES_PER_WEEK,
            message = weightMessage
        )

        if (currentWeek == 1 && biaCount == 0 && weightCount == 0) {
            items += ComplianceMessage(
                order = 0,
                compliant = false,
                message = "Reminder: Complete BIA or weight reading by Day 7 as part of your clinic baseline requirement."
            )
        }

        items
            .sortedWith(compareBy<ComplianceMessage> { it.compliant }.thenBy { it.order })
            .map { it.message }
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
