package com.example.esnmessenger.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.esnmessenger.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

private enum class HomeTab { Chats, Messages, Restaurants, Statistics, Profile }

@Composable
fun HomeScreen(onLogout: () -> Unit, onOpenChat: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(HomeTab.Chats) }

    // Reset to Messages tab whenever the app comes back from background.
    // ON_STOP fires when the app is backgrounded (not when navigating within the app),
    // so this only resets on foreground resume, not on back-navigation from ChatScreen.
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasInBackground by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP  -> wasInBackground = true
                Lifecycle.Event.ON_START -> if (wasInBackground) {
                    selectedTab = HomeTab.Messages
                    wasInBackground = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    selected = selectedTab == HomeTab.Messages,
                    onClick = { selectedTab = HomeTab.Messages },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Messages") },
                    label = { Text("Messages") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ESNCyan,
                        selectedTextColor = ESNCyan,
                        indicatorColor = ESNCyanLight
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Restaurants,
                    onClick = { selectedTab = HomeTab.Restaurants },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = "Restaurants") },
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
                HomeTab.Messages -> MessagesTab(onLogout = onLogout, onOpenChat = onOpenChat)
                HomeTab.Restaurants -> RestaurantsScreen()
                HomeTab.Statistics -> StatisticsScreen()
                HomeTab.Profile -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun MessagesTab(onLogout: () -> Unit, onOpenChat: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    var displayName by remember { mutableStateOf("") }
    var showSignOutDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    // Triple: uid, email, name
    var searchResults by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc -> displayName = doc.getString("name") ?: "" }
    }

    // Debounced search: fires 300 ms after the user stops typing.
    // Runs two prefix queries in parallel (email + name) and merges results,
    // so the user can be found by either field regardless of chat history.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        delay(300)
        isSearching = true
        try {
            val db = FirebaseFirestore.getInstance()
            val query = searchQuery.trim().lowercase()
            val queryEnd = query + '\uf8ff'

            // Run both field queries concurrently then merge
            val byEmail = db.collection("users")
                .whereGreaterThanOrEqualTo("email", query)
                .whereLessThan("email", queryEnd)
                .limit(20)
                .get()
                .await()

            val byName = db.collection("users")
                .whereGreaterThanOrEqualTo("name", searchQuery.trim())
                .whereLessThan("name", searchQuery.trim() + '\uf8ff')
                .limit(20)
                .get()
                .await()

            val merged = (byEmail.documents + byName.documents)
                .distinctBy { it.id }
                .filter { it.id != user?.uid }
                .map { doc ->
                    Triple(
                        doc.id,
                        doc.getString("email") ?: "",
                        doc.getString("name") ?: ""
                    )
                }

            searchResults = merged
        } catch (_: Exception) {
            searchResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ESN",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ESN Messenger",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    val subtitle = displayName.ifEmpty { user?.email ?: "" }
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                TextButton(onClick = { showSignOutDialog = true }) {
                    Text(
                        text = "Sign out",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (showSignOutDialog) {
                    AlertDialog(
                        onDismissRequest = { showSignOutDialog = false },
                        title = { Text("Sign out") },
                        text = { Text("Are you sure you want to sign out?") },
                        confirmButton = {
                            TextButton(onClick = { showSignOutDialog = false; onLogout() }) {
                                Text("Sign out", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSignOutDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users by email…") },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ESNCyan
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = ESNCyan)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                unfocusedBorderColor = OutlineColor
            )
        )

        // Results / empty state
        if (searchQuery.isNotBlank()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (searchResults.isEmpty() && !isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
                items(searchResults) { (uid, email, name) ->
                    UserResultCard(name = name, email = email, onClick = { onOpenChat(uid) })
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(ESNCyanLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💬", fontSize = 40.sp)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Start a conversation",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Search for a user above to begin chatting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun UserResultCard(name: String, email: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(ESNCyanLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = (name.firstOrNull() ?: email.firstOrNull() ?: '?')
                    .uppercaseChar().toString()
                Text(
                    text = initial,
                    color = ESNCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (name.isNotEmpty()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Open chat",
                tint = ESNCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
