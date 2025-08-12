package researchstack.presentation.initiate

import android.R
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
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
            val openWeeklyProgress = intent.getBooleanExtra("openWeeklyProgress", false)

            startDestination?.let {
                AppTheme {
                    ContentComposable(it, page, openWeeklyProgress)
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
        splashLoadingViewModel.setStartMainPage()
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
    ) {
        Surface {
            val navController = rememberNavController()
            val providedController = LocalNavController.provides(navController)

            CompositionLocalProvider(providedController) {
                Router(
                    navController = navController,
                    startRoute = startDestination,
                    askedPage = intent.getIntExtra("page", page),
                )
                if (openWeeklyProgress) {
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
}
