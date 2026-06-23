package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatScreen
import com.example.ui.MapScreen
import com.example.ui.MapRouteViewModel
import com.example.ui.PlannerScreen
import com.example.ui.SavedRoutesScreen
import com.example.ui.ProfileHeaderButton
import com.example.ui.AuthDialog
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector

class MainActivity : ComponentActivity() {
    private val viewModel: MapRouteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for arabic writing
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { AppHeader(viewModel = viewModel) },
                        bottomBar = { AppBottomBar(viewModel = viewModel) }
                    ) { innerPadding ->
                        val currentTab by viewModel.currentTab.collectAsState()

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (currentTab) {
                                0 -> MapScreen(viewModel = viewModel)
                                1 -> PlannerScreen(viewModel = viewModel)
                                2 -> ChatScreen(viewModel = viewModel)
                                3 -> SavedRoutesScreen(viewModel = viewModel)
                            }
                        }
                    }
                    AuthDialog(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(viewModel: MapRouteViewModel) {
    val pins by viewModel.allPins.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()
    
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleek Indigo container for header icon matching tailwind shape.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "بوصلة",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "المسار الذكي",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = (-0.2).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "المواقع المخزنة: ${pins.size}",
                        style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            if (isOffline) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.toggleOfflineMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isOffline) MaterialTheme.colorScheme.error 
                                else Color(0xFF4CAF50), 
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text(
                        text = if (isOffline) "دون اتصال 📴" else "متصل 🌐",
                        style = TextStyle(
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (isOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                ProfileHeaderButton(viewModel = viewModel)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.shadow(2.dp)
    )
}

@Composable
fun AppBottomBar(viewModel: MapRouteViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { viewModel.setTab(0) },
            icon = { Icon(Icons.Default.Map, contentDescription = "الخريطة") },
            label = { Text("الخريطة", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { viewModel.setTab(1) },
            icon = { Icon(Icons.Default.Route, contentDescription = "المخطط") },
            label = { Text("المخطط", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { viewModel.setTab(2) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "المساعد AI") },
            label = { Text("المساعد AI", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { viewModel.setTab(3) },
            icon = { Icon(Icons.Default.Bookmarks, contentDescription = "المفضلة") },
            label = { Text("المفضلة", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
