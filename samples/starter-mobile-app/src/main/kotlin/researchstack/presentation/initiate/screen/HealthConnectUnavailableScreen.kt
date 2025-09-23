package researchstack.presentation.initiate.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import researchstack.R

private val BackgroundColor = Color(0xFF222222)
private val HealthConnectButtonColor = Color(0xFF2B9179)
private val SamsungHealthButtonColor = Color(0xFF287CC3)

@Composable
fun HealthConnectUnavailableScreen(
    showHealthConnectButton: Boolean,
    showSamsungHealthButton: Boolean,
    isHealthConnectUpdateRequired: Boolean,
    onInstallHealthConnect: () -> Unit,
    onInstallSamsungHealth: () -> Unit,
    onRetry: () -> Unit,
) {
    val healthConnectMessageRes = if (isHealthConnectUpdateRequired) {
        R.string.health_connect_update_required_message
    } else {
        R.string.health_connect_required_message
    }
    val healthConnectButtonTextRes = if (isHealthConnectUpdateRequired) {
        R.string.update_health_connect
    } else {
        R.string.install_health_connect
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 24.dp)
            .padding(top = 64.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.health_connect_required_title),
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (showHealthConnectButton) {
            Text(
                text = stringResource(id = healthConnectMessageRes),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
        }

        if (showSamsungHealthButton) {
            if (showHealthConnectButton) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = stringResource(id = R.string.samsung_health_required_message),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
        }

        if (!showHealthConnectButton && !showSamsungHealthButton) {
            Text(
                text = stringResource(id = healthConnectMessageRes),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (showHealthConnectButton) {
            Button(
                onClick = onInstallHealthConnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = HealthConnectButtonColor,
                    contentColor = Color.White,
                )
            ) {
                Text(text = stringResource(id = healthConnectButtonTextRes))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showSamsungHealthButton) {
            Button(
                onClick = onInstallSamsungHealth,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SamsungHealthButtonColor,
                    contentColor = Color.White,
                )
            ) {
                Text(text = stringResource(id = R.string.install_samsung_health))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.White,
            ),
            border = BorderStroke(1.dp, Color.White),
        ) {
            Text(text = stringResource(id = R.string.health_connect_retry))
        }
    }
}
