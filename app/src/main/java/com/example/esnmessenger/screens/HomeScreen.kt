package com.example.esnmessenger.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.esnmessenger.ui.theme.ESNCyan
import com.example.esnmessenger.ui.theme.ESNCyanLight

private enum class HomeTab { Chats, Food, Statistics, Profile }

@Composable
fun HomeScreen(onLogout: () -> Unit, onOpenChat: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(HomeTab.Chats) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Chats,
                    onClick = { selectedTab = HomeTab.Chats },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Chats") },
                    label = { Text("Chats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ESNCyan,
                        selectedTextColor = ESNCyan,
                        indicatorColor = ESNCyanLight
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Food,
                    onClick = { selectedTab = HomeTab.Food },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = "Food") },
                    label = { Text("Food") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ESNCyan,
                        selectedTextColor = ESNCyan,
                        indicatorColor = ESNCyanLight
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Statistics,
                    onClick = { selectedTab = HomeTab.Statistics },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ESNCyan,
                        selectedTextColor = ESNCyan,
                        indicatorColor = ESNCyanLight
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Profile,
                    onClick = { selectedTab = HomeTab.Profile },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ESNCyan,
                        selectedTextColor = ESNCyan,
                        indicatorColor = ESNCyanLight
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                HomeTab.Chats -> ExistingChatsTab(onOpenChat = onOpenChat)
                HomeTab.Food -> RestaurantsScreen()
                HomeTab.Statistics -> StatisticsScreen()
                HomeTab.Profile -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}
