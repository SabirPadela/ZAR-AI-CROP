package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.gemini.GeminiService
import com.example.data.repository.FirestoreRepository
import com.example.ui.screens.AgriAdvisorScreen
import com.example.ui.screens.FieldLogsScreen
import com.example.ui.screens.KisanCentersScreen
import com.example.ui.screens.MandiRatesScreen
import com.example.ui.screens.ScannerScreen
import com.example.util.AudioHelper

enum class AppTab(val title: String, val testTag: String) {
    SCANNER("Scan Crop", "nav_tab_scanner"),
    ADVISOR("Advisor", "nav_tab_advisor"),
    MANDI("Mandi Rates", "nav_tab_mandi"),
    CENTERS("Centers", "nav_tab_centers"),
    LOGS("Field Logs", "nav_tab_logs")
}

@Composable
fun MainApp(
    geminiService: GeminiService,
    firestoreRepository: FirestoreRepository,
    audioHelper: AudioHelper,
    onSignOutComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AppTab.SCANNER) }
    var advisorInitialQuery by remember { mutableStateOf("") }

    // Hardware back handler returns to home scanner tab if on another tab
    BackHandler(enabled = currentTab != AppTab.SCANNER) {
        currentTab = AppTab.SCANNER
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.SCANNER,
                    onClick = { currentTab = AppTab.SCANNER },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Crop") },
                    label = { Text("Scan") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.SCANNER.testTag)
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.ADVISOR,
                    onClick = { currentTab = AppTab.ADVISOR },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Agri Advisor") },
                    label = { Text("Advisor") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.ADVISOR.testTag)
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.MANDI,
                    onClick = { currentTab = AppTab.MANDI },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Mandi Rates") },
                    label = { Text("Mandi") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.MANDI.testTag)
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.CENTERS,
                    onClick = { currentTab = AppTab.CENTERS },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Kisan Centers") },
                    label = { Text("Centers") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.CENTERS.testTag)
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.LOGS,
                    onClick = { currentTab = AppTab.LOGS },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Field Logs") },
                    label = { Text("Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(AppTab.LOGS.testTag)
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
                AppTab.SCANNER -> {
                    ScannerScreen(
                        geminiService = geminiService,
                        firestoreRepository = firestoreRepository,
                        audioHelper = audioHelper,
                        onNavigateToAdvisorWithContext = { query ->
                            advisorInitialQuery = query
                            currentTab = AppTab.ADVISOR
                        }
                    )
                }
                AppTab.ADVISOR -> {
                    AgriAdvisorScreen(
                        geminiService = geminiService,
                        audioHelper = audioHelper,
                        initialQuery = advisorInitialQuery
                    )
                }
                AppTab.MANDI -> {
                    MandiRatesScreen(
                        geminiService = geminiService
                    )
                }
                AppTab.CENTERS -> {
                    KisanCentersScreen(
                        geminiService = geminiService
                    )
                }
                AppTab.LOGS -> {
                    FieldLogsScreen(
                        firestoreRepository = firestoreRepository,
                        geminiService = geminiService,
                        audioHelper = audioHelper,
                        onSignOutComplete = onSignOutComplete
                    )
                }
            }
        }
    }
}
