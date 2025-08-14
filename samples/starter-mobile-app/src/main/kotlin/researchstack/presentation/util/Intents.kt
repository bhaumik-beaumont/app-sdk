package researchstack.presentation.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val ABOUT_URL = "https://example.com/about"
const val PRIVACY_URL = "https://example.com/privacy"

fun openUrl(context: Context, url: String) {
    val uri = Uri.parse(url)
    val customTabsIntent = CustomTabsIntent.Builder().build()
    val packageManager = context.packageManager
    if (customTabsIntent.intent.resolveActivity(packageManager) != null) {
        customTabsIntent.launchUrl(context, uri)
    } else {
        val viewIntent = Intent(Intent.ACTION_VIEW, uri)
        if (viewIntent.resolveActivity(packageManager) != null) {
            context.startActivity(viewIntent)
        } else {
            Toast.makeText(context, "No browser available", Toast.LENGTH_LONG).show()
        }
    }
}

fun contactSupport(
    context: Context,
    snackbarHostState: SnackbarHostState? = null,
    coroutineScope: CoroutineScope? = null
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("alexa.skill.89@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Test Subject")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val message = "No email app found"
        if (snackbarHostState != null && coroutineScope != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
