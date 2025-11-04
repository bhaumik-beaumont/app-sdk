package researchstack.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import researchstack.R
import researchstack.presentation.main.component.AlwaysVisiblePositionIndicator
import researchstack.presentation.main.screen.HomeScreenItem
import researchstack.presentation.theme.HealthWearableTheme
import researchstack.presentation.theme.TextColor

class MedicalInfoActivity : ComponentActivity() {
    companion object {
        const val EXTRA_HOME_ITEM = "medical_info_home_item"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selectedItem = intent?.getStringExtra(EXTRA_HOME_ITEM)?.let { itemName ->
            runCatching { HomeScreenItem.valueOf(itemName) }.getOrNull()
        } ?: HomeScreenItem.BLOOD_OXYGEN
        setContent {
            HealthWearableTheme {
                MedicalInfoScreen(selectedItem)
            }
        }
    }

    @Composable
    private fun Section(titleRes: Int, messageRes: Int) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = stringResource(id = titleRes),
                color = TextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = messageRes),
                color = TextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    private fun HomeScreenItem.messageRes(): Int {
        return when (this) {
            HomeScreenItem.BLOOD_OXYGEN -> R.string.medical_info_blood_oxygen
            HomeScreenItem.ECG -> R.string.medical_info_ecg
            HomeScreenItem.BODY_COMPOSITION -> R.string.medical_info_body_composition
            HomeScreenItem.PPG_RED -> R.string.medical_info_ppg_red
            HomeScreenItem.PPG_IR -> R.string.medical_info_ppg_ir
        }
    }

    @Composable
    fun MedicalInfoScreen(selectedItem: HomeScreenItem) {
        val titleRes = selectedItem.titleRes()
        val messageRes = selectedItem.messageRes()
        val listState = rememberScalingLazyListState()

        Scaffold(
            positionIndicator = {
                AlwaysVisiblePositionIndicator(scalingLazyListState = listState)
            }
        ) { innerPadding ->
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = listState,
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    Text(
                        text = stringResource(id = R.string.medical_info_title),
                        color = TextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    Text(
                        text = stringResource(id = R.string.medical_info_intro),
                        color = TextColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
                item {
                    Section(
                        titleRes = titleRes,
                        messageRes = messageRes
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    private fun HomeScreenItem.titleRes(): Int {
        return when (this) {
            HomeScreenItem.BLOOD_OXYGEN -> R.string.blood_oxygen
            HomeScreenItem.ECG -> R.string.ecg
            HomeScreenItem.BODY_COMPOSITION -> R.string.body_composition
            HomeScreenItem.PPG_RED -> R.string.ppg_red
            HomeScreenItem.PPG_IR -> R.string.ppg_ir
        }
    }
}
