package researchstack.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import researchstack.R
import researchstack.presentation.theme.HealthWearableTheme
import researchstack.presentation.theme.TextColor

class MedicalInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthWearableTheme {
                MedicalInfoScreen()
            }
        }
    }

    @Composable
    private fun Section(titleRes: Int, messageRes: Int) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
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

    @Composable
    fun MedicalInfoScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(id = R.string.medical_info_title),
                color = TextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.medical_info_intro),
                color = TextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Section(
                titleRes = R.string.blood_oxygen,
                messageRes = R.string.medical_info_blood_oxygen
            )
            Spacer(modifier = Modifier.height(20.dp))

            Section(
                titleRes = R.string.ecg,
                messageRes = R.string.medical_info_ecg
            )
            Spacer(modifier = Modifier.height(20.dp))

            Section(
                titleRes = R.string.body_composition,
                messageRes = R.string.medical_info_body_composition
            )
            Spacer(modifier = Modifier.height(20.dp))

            Section(
                titleRes = R.string.ppg_red,
                messageRes = R.string.medical_info_ppg_red
            )
            Spacer(modifier = Modifier.height(20.dp))

            Section(
                titleRes = R.string.ppg_ir,
                messageRes = R.string.medical_info_ppg_ir
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
