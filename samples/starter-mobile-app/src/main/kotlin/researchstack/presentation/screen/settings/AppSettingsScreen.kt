package researchstack.presentation.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import researchstack.BuildConfig
import researchstack.presentation.util.ABOUT_URL
import researchstack.presentation.util.PRIVACY_URL
import researchstack.presentation.util.contactSupport

@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    showBackButton: Boolean = false
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val versionName = BuildConfig.VERSION_NAME
    val versionCode = remember {
        PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0)
        ).toString()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column {
                        SettingsItem(
                            title = "About Us",
                            icon = Icons.Default.Info,
                            contentDescription = "About Us",
                            onClick = { onOpenUrl(ABOUT_URL) }
                        )
                        Divider()
                        SettingsItem(
                            title = "Privacy Policy",
                            icon = Icons.Default.Lock,
                            contentDescription = "Privacy Policy",
                            onClick = { onOpenUrl(PRIVACY_URL) }
                        )
                        Divider()
                        SettingsItem(
                            title = "Contact Us",
                            icon = Icons.Default.Email,
                            contentDescription = "Contact Us",
                            onClick = { contactSupport(context, snackbarHostState, scope) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Build Info",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column {
                        KeyValueRow("Build Number", versionName)
                        Divider()
                        KeyValueRow("Build Code", versionCode)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null
        )
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Preview
@Composable
private fun AppSettingsScreenPreview() {
    AppSettingsScreen(onBack = {}, onOpenUrl = {})
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppSettingsScreenDarkPreview() {
    AppSettingsScreen(onBack = {}, onOpenUrl = {})
}

