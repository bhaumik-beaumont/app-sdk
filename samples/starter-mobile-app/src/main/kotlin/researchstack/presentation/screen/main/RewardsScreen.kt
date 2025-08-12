package researchstack.presentation.screen.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import researchstack.R
import researchstack.presentation.LocalNavController
import researchstack.presentation.viewmodel.RewardsViewModel

@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val activityWeeks by viewModel.activityRewardWeeks.collectAsState()
    val resistanceWeeks by viewModel.resistanceRewardWeeks.collectAsState()
    val biaWeeks by viewModel.biaRewardWeeks.collectAsState()
    val weightWeeks by viewModel.weightRewardWeeks.collectAsState()
    val championWeeks by viewModel.championRewardWeeks.collectAsState()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing,
        containerColor = Color(0xFF222222),
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(16.dp)
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
                    text = stringResource(id = R.string.rewards),
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
                .padding(vertical = 16.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            RewardSection(
                title = stringResource(id = R.string.champion_rewards),
                weeks = championWeeks,
                badgeRes = R.drawable.champion
            )
            RewardSection(
                title = stringResource(id = R.string.activity_rewards),
                weeks = activityWeeks,
                badgeRes = R.drawable.activity
            )
            RewardSection(
                title = stringResource(id = R.string.resistance_rewards),
                weeks = resistanceWeeks,
                badgeRes = R.drawable.resistance
            )
            RewardSection(
                title = stringResource(id = R.string.bia_rewards),
                weeks = biaWeeks,
                badgeRes = R.drawable.bia
            )
            RewardSection(
                title = stringResource(id = R.string.weight_rewards),
                weeks = weightWeeks,
                badgeRes = R.drawable.bia
            )
        }
    }
}

@Composable
fun RewardSection(
    title: String,
    weeks: List<Int>,
    @DrawableRes badgeRes: Int
) {
    if (weeks.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B2B)), // lighter than background
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(weeks) { week ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {


                        Image(
                            painter = painterResource(id = badgeRes),
                            contentDescription = null,
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Week $week",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

