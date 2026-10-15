package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.example.ui.StudentViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuranAndAdhkarScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.StudentTheme

enum class MainTab(val title: String, val tag: String) {
    HOME("الرئيسية", "nav_tab_home"),
    SCHEDULE("الجدول", "nav_tab_schedule"),
    TIMER("المذاكرة", "nav_tab_timer"),
    QURAN("المصحف والأذكار", "nav_tab_quran"),
    SETTINGS("الإعدادات", "nav_tab_settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTab = parseNavIntent(intent)

        setContent {
            StudentTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppContent(
                        viewModel = viewModel,
                        initialTab = initialTab
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun parseNavIntent(intent: Intent?): MainTab {
        return when (intent?.getStringExtra("EXTRA_NAV_TARGET")) {
            "timer" -> {
                viewModel.setTimerScreenTab(0)
                MainTab.TIMER
            }
            "workouts" -> {
                viewModel.setTimerScreenTab(1)
                MainTab.TIMER
            }
            "tasks" -> MainTab.HOME
            "schedule" -> MainTab.SCHEDULE
            else -> MainTab.HOME
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: StudentViewModel,
    initialTab: MainTab
) {
    var currentTab by remember { mutableStateOf(initialTab) }

    // Request notification permission if needed on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Android BackHandler: if not on Home screen, go back to Home screen
    if (currentTab != MainTab.HOME) {
        BackHandler {
            currentTab = MainTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.HOME,
                    onClick = { currentTab = MainTab.HOME },
                    icon = {
                        Icon(
                            if (currentTab == MainTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = MainTab.HOME.title
                        )
                    },
                    label = { Text(MainTab.HOME.title) },
                    modifier = Modifier.testTag(MainTab.HOME.tag)
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.SCHEDULE,
                    onClick = { currentTab = MainTab.SCHEDULE },
                    icon = {
                        Icon(
                            if (currentTab == MainTab.SCHEDULE) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = MainTab.SCHEDULE.title
                        )
                    },
                    label = { Text(MainTab.SCHEDULE.title) },
                    modifier = Modifier.testTag(MainTab.SCHEDULE.tag)
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.TIMER,
                    onClick = { currentTab = MainTab.TIMER },
                    icon = {
                        Icon(
                            if (currentTab == MainTab.TIMER) Icons.Default.Timer else Icons.Outlined.Timer,
                            contentDescription = MainTab.TIMER.title
                        )
                    },
                    label = { Text(MainTab.TIMER.title) },
                    modifier = Modifier.testTag(MainTab.TIMER.tag)
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.QURAN,
                    onClick = { currentTab = MainTab.QURAN },
                    icon = {
                        Icon(
                            if (currentTab == MainTab.QURAN) Icons.Default.Book else Icons.Outlined.Book,
                            contentDescription = MainTab.QURAN.title
                        )
                    },
                    label = { Text(MainTab.QURAN.title) },
                    modifier = Modifier.testTag(MainTab.QURAN.tag)
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = {
                        Icon(
                            if (currentTab == MainTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = MainTab.SETTINGS.title
                        )
                    },
                    label = { Text(MainTab.SETTINGS.title) },
                    modifier = Modifier.testTag(MainTab.SETTINGS.tag)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSchedule = { currentTab = MainTab.SCHEDULE },
                    onNavigateToTimer = {
                        viewModel.setTimerScreenTab(0)
                        currentTab = MainTab.TIMER
                    },
                    onNavigateToQuran = { currentTab = MainTab.QURAN },
                    onNavigateToWorkouts = {
                        viewModel.setTimerScreenTab(1)
                        currentTab = MainTab.TIMER
                    }
                )
                MainTab.SCHEDULE -> ScheduleScreen(viewModel = viewModel)
                MainTab.TIMER -> TimerScreen(viewModel = viewModel)
                MainTab.QURAN -> QuranAndAdhkarScreen(viewModel = viewModel)
                MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
