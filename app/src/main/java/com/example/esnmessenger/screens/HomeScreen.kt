package com.example.esnmessenger.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esnmessenger.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private enum class HomeTab { Messages, Restaurants, Statistics, Profile }

@Composable
fun HomeScreen(onLogout: () -> Unit, onOpenChat: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(HomeTab.Messages) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
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
                    label = { Text("Restaurants") },
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
    var recipientEmail by remember { mutableStateOf("") }
    var isLookingUp by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc -> displayName = doc.getString("name") ?: "" }
    }

    fun openChatByEmail() {
        isLookingUp = true
        lookupError = null
        FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("email", recipientEmail.trim().lowercase())
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                isLookingUp = false
                val uid = snapshot.documents.firstOrNull()?.id
                if (uid != null) {
                    onOpenChat(uid)
                } else {
                    lookupError = "No user found with that email."
                }
            }
            .addOnFailureListener { e ->
                isLookingUp = false
                lookupError = e.message ?: "Lookup failed."
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient header with logout button
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

        // Chat entry
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
                text = "Enter a user ID to open a chat",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = recipientEmail,
                onValueChange = { recipientEmail = it; lookupError = null },
                label = { Text("Recipient email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                isError = lookupError != null,
                supportingText = lookupError?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ESNCyan,
                    unfocusedBorderColor = OutlineColor
                )
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { openChatByEmail() },
                enabled = recipientEmail.isNotBlank() && !isLookingUp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ESNCyan)
            ) {
                if (isLookingUp) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Open Chat", color = Color.White, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}
