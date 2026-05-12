package com.example.worktimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worktimer.ui.FocusScreen
import com.example.worktimer.ui.WeeklyScreen
import com.example.worktimer.ui.theme.WorkTimerTheme
import com.example.worktimer.viewmodel.TimeTrackerViewModel

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkTimerTheme {
                val viewModel: TimeTrackerViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
private fun MainScreen(viewModel: TimeTrackerViewModel) {
    val darkTheme = isSystemInDarkTheme()
    val navContainer = if (darkTheme) Color(0xFF171A22) else Color.White
    val selectedBlue = if (darkTheme) Color(0xFF6EA3FF) else Color(0xFF2962FF)
    val unselected = if (darkTheme) Color(0xFFA5ADBA) else Color(0xFF9CA3AF)
    val indicator = if (darkTheme) Color(0xFF17345F) else Color(0xFFE3F2FD)
    val tabs = listOf(
        BottomNavItem("Track", Icons.Filled.Timer, Icons.Outlined.Timer),
        BottomNavItem("Weekly", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = navContainer,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedBlue,
                            selectedTextColor = selectedBlue,
                            unselectedIconColor = unselected,
                            unselectedTextColor = unselected,
                            indicatorColor = indicator
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> FocusScreen(viewModel)
                1 -> WeeklyScreen(viewModel)
            }
        }
    }
}
