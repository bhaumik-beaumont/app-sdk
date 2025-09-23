package researchstack.presentation.initiate

import android.R
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import researchstack.presentation.LocalNavController
import researchstack.presentation.initiate.route.Route
import researchstack.presentation.initiate.route.Router
import researchstack.presentation.theme.AppTheme
import researchstack.presentation.viewmodel.SplashLoadingViewModel
import researchstack.util.NotificationUtil
import researchstack.util.scheduleComplianceCheck
import researchstack.util.setAlarm
import androidx.core.net.toUri

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var splashLoadingViewModel: SplashLoadingViewModel

    private var isContentReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initSplashLoadingViewModel()

        setContent {
            val startDestination: Route? by splashLoadingViewModel.routeDestination.observeAsState()
            val page by splashLoadingViewModel.startMainPage.observeAsState(0)
            val healthConnectAvailable by splashLoadingViewModel.healthConnectAvailable.observeAsState()
            val samsungHealthAvailable by splashLoadingViewModel.samsungHealthAvailable.observeAsState()
            val healthConnectAvailabilityStatus by splashLoadingViewModel.healthConnectAvailabilityStatus.observeAsState()
            val openWeeklyProgress = intent.getBooleanExtra("openWeeklyProgress", false)
            val latestRequiredAppVersion by splashLoadingViewModel.latestRequiredAppVersion.observeAsState()

            AppTheme {
                startDestination?.let { destination ->
                    ContentComposable(
                        startDestination = destination,
                        page = page,
                        openWeeklyProgress = openWeeklyProgress,
                        healthConnectAvailable = healthConnectAvailable,
                        samsungHealthAvailable = samsungHealthAvailable,
                        healthConnectAvailabilityStatus = healthConnectAvailabilityStatus,
                        latestRequiredAppVersion = latestRequiredAppVersion,
                        onUpdateApp = { openAppInStore() },
                    )
                }
            }
        }

        setSuspendDrawingTheFirstView()
        setAlarm(this)
        scheduleComplianceCheck(this)
    }

    private fun initSplashLoadingViewModel() {
        splashLoadingViewModel = ViewModelProvider(this)[SplashLoadingViewModel::class.java]

        splashLoadingViewModel.isReady.observe(
            this
        ) { newIsReady -> isContentReady = newIsReady }

        splashLoadingViewModel.setStartRouteDestination()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    @Composable
    private fun ContentComposable(
        startDestination: Route,
        page: Int = 0,
        openWeeklyProgress: Boolean = false,
        healthConnectAvailable: Boolean? = null,
        samsungHealthAvailable: Boolean? = null,
        healthConnectAvailabilityStatus: Int? = null,
        latestRequiredAppVersion: String? = null,
        onUpdateApp: () -> Unit,
    ) {
        Surface {
            val navController = rememberNavController()
            val providedController = LocalNavController.provides(navController)

            CompositionLocalProvider(providedController) {
                Router(
                    navController = navController,
                    startRoute = startDestination,
                    askedPage = intent.getIntExtra("page", page),
                    onInstallHealthConnect = { openHealthConnectInPlayStore() },
                    onInstallSamsungHealth = { openSamsungHealthInStore() },
                    onHealthConnectRetry = { handleHealthConnectRetry() },
                    isHealthConnectAvailable = healthConnectAvailable,
                    isSamsungHealthAvailable = samsungHealthAvailable,
                    healthConnectAvailabilityStatus = healthConnectAvailabilityStatus,
                    latestRequiredAppVersion = latestRequiredAppVersion,
                    onUpdateApp = onUpdateApp,
                )
                if (openWeeklyProgress && startDestination == Route.Main) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        navController.navigate(Route.WeeklyProgress.name)
                    }
                }
            }
            RegisterSignOutReceiver(navController)
        }
    }

    private fun setSuspendDrawingTheFirstView() {
        val content: View = findViewById(R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                // return true if the content is ready and finish splash screen
                override fun onPreDraw(): Boolean {
                    return if (isContentReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )
    }

    // NOTE: currently not support system font configuration
    override fun attachBaseContext(newBase: Context) {
        val newOverride = Configuration(newBase.resources?.configuration)
        newOverride.fontScale = 1.0f
        applyOverrideConfiguration(newOverride)

        super.attachBaseContext(newBase)
    }

    override fun onResume() {
        super.onResume()
        NotificationUtil.initialize(this).let { NotificationUtil.getInstance().cancelAllNotification() }
    }

    private fun handleHealthConnectRetry() {
        splashLoadingViewModel.setStartRouteDestination()
    }

    private fun openAppInStore() {
        val appPackageName = packageName
        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=$appPackageName".toUri()
            setPackage("com.android.vending")
        }
        val launched = startActivityIfAvailable(playStoreIntent)
        if (launched) {
            return
        }

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$appPackageName".toUri()
        )
        if (startActivityIfAvailable(webIntent)) {
            return
        }

        Toast.makeText(this, getString(researchstack.R.string.no_app_found), Toast.LENGTH_LONG).show()
    }

    private fun openHealthConnectInPlayStore() {
        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=$HEALTH_CONNECT_PACKAGE_NAME".toUri()
            setPackage("com.android.vending")
        }
        val launched = startActivityIfAvailable(playStoreIntent)
        if (!launched) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE_NAME".toUri()
            )
            if (!startActivityIfAvailable(webIntent)) {
                Toast.makeText(this, getString(researchstack.R.string.no_app_found), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openSamsungHealthInStore() {
        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "market://details?id=$SAMSUNG_HEALTH_PACKAGE_NAME".toUri()
            setPackage("com.android.vending")
        }
        if (startActivityIfAvailable(playStoreIntent)) {
            return
        }

        val galaxyStoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "samsungapps://ProductDetail/$SAMSUNG_HEALTH_PACKAGE_NAME".toUri()
        }
        if (startActivityIfAvailable(galaxyStoreIntent)) {
            return
        }

        val webPlayStoreIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$SAMSUNG_HEALTH_PACKAGE_NAME".toUri()
        )
        if (startActivityIfAvailable(webPlayStoreIntent)) {
            return
        }

        val galaxyStoreWebIntent = Intent(
            Intent.ACTION_VIEW,
            "https://galaxystore.samsung.com/detail/$SAMSUNG_HEALTH_PACKAGE_NAME".toUri()
        )
        if (startActivityIfAvailable(galaxyStoreWebIntent)) {
            return
        }

        Toast.makeText(this, getString(researchstack.R.string.no_app_found), Toast.LENGTH_LONG).show()
    }

    private fun startActivityIfAvailable(intent: Intent): Boolean {
        val resolveInfo = intent.resolveActivity(packageManager)
        return if (resolveInfo != null) {
            startActivity(intent)
            true
        } else {
            false
        }
    }

    private companion object {
        private const val HEALTH_CONNECT_PACKAGE_NAME = "com.google.android.apps.healthdata"
        private const val SAMSUNG_HEALTH_PACKAGE_NAME = "com.sec.android.app.shealth"
    }
}
