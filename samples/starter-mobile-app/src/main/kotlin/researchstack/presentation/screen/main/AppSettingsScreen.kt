package researchstack.presentation.screen.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import researchstack.R

private const val ABOUT_URL = "https://example.com/about"
private const val PRIVACY_URL = "https://example.com/privacy"

@Composable
fun AppSettingsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName ?: ""
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toString()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong().toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color(0xFF7F8C8D), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.flexed_biceps),
                    contentDescription = stringResource(id = R.string.home),
                    tint = Color.Yellow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.settings),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(Color(0xFF232527)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        label = stringResource(id = R.string.about_us),
                        onClick = { launchUrl(context, ABOUT_URL) }
                    )
                    Divider(color = Color(0xFF3D3D3D))
                    SettingsRow(
                        icon = Icons.Filled.Lock,
                        label = stringResource(id = R.string.privacy_policy),
                        onClick = { launchUrl(context, PRIVACY_URL) }
                    )
                    Divider(color = Color(0xFF3D3D3D))
                    SettingsRow(
                        icon = Icons.Filled.Email,
                        label = stringResource(id = R.string.contact_us),
                        onClick = { sendEmail(context) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.build_info),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(Color(0xFF232527)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    BuildRow(
                        label = stringResource(id = R.string.build_number),
                        value = versionName
                    )
                    Divider(color = Color(0xFF3D3D3D))
                    BuildRow(
                        label = stringResource(id = R.string.build_code),
                        value = versionCode
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun BuildRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, textAlign = TextAlign.End)
    }
}

private fun launchUrl(context: android.content.Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show()
        }
    }
}

private fun sendEmail(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("alexa.skill.89@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Test Subject")
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show()
    }
}

