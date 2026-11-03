package com.hivid.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hivid.app.domain.model.ConversionState
import com.hivid.app.ui.components.AppHeader
import com.hivid.app.ui.components.BottomNavPill
import com.hivid.app.ui.components.NavTab
import com.hivid.app.ui.screens.convert.ConvertScreen
import com.hivid.app.ui.screens.convert.ConvertViewModel
import com.hivid.app.ui.screens.guide.GuideScreen
import com.hivid.app.ui.screens.history.HistoryScreen
import com.hivid.app.ui.screens.history.HistoryViewModel
import com.hivid.app.ui.screens.logs.LogsScreen
import com.hivid.app.ui.screens.logs.LogsViewModel
import com.hivid.app.ui.screens.remux.RemuxScreen
import com.hivid.app.ui.screens.remux.RemuxViewModel
import com.hivid.app.ui.screens.settings.SettingsScreen
import com.hivid.app.ui.screens.settings.SettingsViewModel
import com.hivid.app.ui.theme.HdrColors
import com.hivid.app.ui.theme.HividTheme

class MainActivity : ComponentActivity() {

    private val convertViewModel: ConvertViewModel by viewModels()
    private val remuxViewModel: RemuxViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val logsViewModel: LogsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            val userSettings by settingsViewModel.userSettings.collectAsState()
            val isDark = userSettings.themeMode != "LIGHT"

            HividTheme(darkTheme = isDark) {
                var currentTab by remember { mutableStateOf(NavTab.CONVERT) }
                var showSettings by remember { mutableStateOf(false) }
                var showLogs by remember { mutableStateOf(false) }

                val convertState by convertViewModel.uiState.collectAsState()

                // Keep screen on while converting if configured
                LaunchedEffect(convertState.conversionState, userSettings.keepScreenOn) {
                    val isConverting = convertState.conversionState is ConversionState.Processing ||
                            convertState.conversionState is ConversionState.Preparing
                    if (isConverting && userSettings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                val isOverlayOpen = showSettings || showLogs

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HdrColors.Bg),
                    containerColor = HdrColors.Bg,
                    topBar = {
                        if (!isOverlayOpen) {
                            AppHeader(
                                onSettingsClick = { showSettings = true }
                            )
                        }
                    },
                    bottomBar = {
                        if (!isOverlayOpen) {
                            BottomNavPill(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        when {
                            showLogs -> {
                                LogsScreen(
                                    viewModel = logsViewModel,
                                    onBack = { showLogs = false }
                                )
                            }
                            showSettings -> {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { showSettings = false },
                                    onOpenLogs = { showLogs = true }
                                )
                            }
                            else -> {
                                when (currentTab) {
                                    NavTab.CONVERT -> ConvertScreen(
                                        viewModel = convertViewModel,
                                        onOpenLogs = { showLogs = true }
                                    )
                                    NavTab.REMUX -> RemuxScreen(viewModel = remuxViewModel)
                                    NavTab.GUIDE -> GuideScreen()
                                    NavTab.HISTORY -> HistoryScreen(viewModel = historyViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
