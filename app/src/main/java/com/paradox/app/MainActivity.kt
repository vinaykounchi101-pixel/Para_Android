package com.paradox.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.ui.components.QuickBallView
import com.paradox.app.core.ui.theme.ParadoxTheme
import com.paradox.app.core.ui.theme.ThemePalette
import com.paradox.app.domain.repository.ProfileRepository
import com.paradox.app.feature.quickball.QuickBallOverlayService
import com.paradox.app.navigation.ParadoxNavGraph
import com.paradox.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var sessionDataStore: SessionDataStore

    @Inject
    lateinit var profileRepository: ProfileRepository

    private var pendingNavigationRoute: String? = null
    private var activeNavController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingNavigationRoute = intent?.getStringExtra(QuickBallOverlayService.EXTRA_ROUTE)

        setContent {
            val themeMode by sessionDataStore.themeMode.collectAsState(initial = "SYSTEM")
            val themePalette by sessionDataStore.themePalette.collectAsState(initial = "SLATE")
            val quickBallMode by sessionDataStore.quickBallMode.collectAsState(initial = "IN_APP")
            val isLocked by sessionDataStore.isAppLocked.collectAsState(initial = true)

            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            val palette = ThemePalette.fromName(themePalette)

            // Start or stop overlay service based on mode setting
            LaunchedEffect(quickBallMode) {
                if (quickBallMode == "SYSTEM_WIDE") {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity)) {
                        QuickBallOverlayService.start(this@MainActivity)
                    }
                } else {
                    QuickBallOverlayService.stop(this@MainActivity)
                }
            }

            ParadoxTheme(
                darkTheme = isDark,
                palette = palette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    val sharedText = remember {
                        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
                            intent.getStringExtra(Intent.EXTRA_TEXT)
                        } else null
                    }
                    val sharedImageUri = remember {
                        if (intent?.action == Intent.ACTION_SEND && intent?.type?.startsWith("image/") == true) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(Intent.EXTRA_STREAM)
                            }
                        } else null
                    }

                    LaunchedEffect(Unit) {
                        val profileCount = profileRepository.getProfileCount()
                        val activeId = sessionDataStore.activeProfileId.firstOrNull()
                        val locked = sessionDataStore.isAppLocked.firstOrNull() ?: true

                        startDestination = when {
                            profileCount == 0 -> Screen.Onboarding.route
                            locked || activeId == null -> Screen.Unlock.route
                            else -> Screen.Dashboard.route
                        }
                    }

                    if (startDestination != null) {
                        val navController = rememberNavController()
                        activeNavController = navController

                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        // Handle pending deep link from Quick Ball service
                        LaunchedEffect(currentRoute, pendingNavigationRoute, isLocked) {
                            if (!isLocked && pendingNavigationRoute != null) {
                                val target = pendingNavigationRoute
                                pendingNavigationRoute = null
                                target?.let { navController.navigate(it) }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            ParadoxNavGraph(
                                navController = navController,
                                startDestination = startDestination!!,
                                sharedText = sharedText,
                                sharedImageUri = sharedImageUri
                            )

                            // Assistive In-App Quick Ball Overlay
                            val canShowQuickBall = quickBallMode == "IN_APP" &&
                                !isLocked &&
                                currentRoute != Screen.Onboarding.route &&
                                currentRoute != Screen.Unlock.route

                            if (canShowQuickBall) {
                                QuickBallView(
                                    onQuickAdd = {
                                        navController.navigate(Screen.Capture.createRoute("QUICK_ADD"))
                                    },
                                    onVoiceEntry = {
                                        navController.navigate(Screen.Capture.createRoute("VOICE"))
                                    },
                                    onScanReceipt = {
                                        navController.navigate(Screen.Capture.createRoute("OCR"))
                                    },
                                    onAskParadox = {
                                        navController.navigate(Screen.AskParadox.route)
                                    }
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = intent.getStringExtra(QuickBallOverlayService.EXTRA_ROUTE)
        if (route != null) {
            lifecycleScope.launch {
                val isLocked = sessionDataStore.isAppLocked.firstOrNull() ?: true
                if (!isLocked) {
                    activeNavController?.navigate(route)
                } else {
                    pendingNavigationRoute = route
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val quickBallMode = sessionDataStore.quickBallMode.firstOrNull() ?: "IN_APP"
            if (quickBallMode == "SYSTEM_WIDE") {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity)) {
                    QuickBallOverlayService.start(this@MainActivity)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Lock vault when app is backgrounded
        lifecycleScope.launch {
            sessionDataStore.setAppLocked(true)
        }
    }
}

