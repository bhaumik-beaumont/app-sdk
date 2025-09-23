package researchstack.presentation.initiate.screen

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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import researchstack.BuildConfig
import researchstack.R

private val BackgroundColor = Color(0xFF222222)
private val UpdateButtonColor = Color(0xFF4C8BF5)

@Composable
fun AppUpdateRequiredScreen(
    latestVersionName: String?,
    onUpdateApp: () -> Unit,
) {
    val message = latestVersionName?.let {
        stringResource(id = R.string.app_update_required_message_with_version, it)
    } ?: stringResource(id = R.string.app_update_required_message)

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
            text = stringResource(id = R.string.app_update_required_title),
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.app_update_current_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onUpdateApp,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = UpdateButtonColor,
                contentColor = Color.White,
            )
        ) {
            Text(text = stringResource(id = R.string.update_app))
        }
    }
}
