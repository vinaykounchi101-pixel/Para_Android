package com.paradox.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.ui.theme.ParadoxTheme
import com.paradox.app.domain.repository.ProfileRepository
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ParadoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val profileCount = profileRepository.getProfileCount()
                        val activeId = sessionDataStore.activeProfileId.firstOrNull()
                        val isLocked = sessionDataStore.isAppLocked.firstOrNull() ?: true

                        startDestination = when {
                            profileCount == 0 -> Screen.Onboarding.route
                            isLocked || activeId == null -> Screen.Unlock.route
                            else -> Screen.Dashboard.route
                        }
                    }

                    if (startDestination != null) {
                        val navController = rememberNavController()
                        ParadoxNavGraph(
                            navController = navController,
                            startDestination = startDestination!!
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
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
