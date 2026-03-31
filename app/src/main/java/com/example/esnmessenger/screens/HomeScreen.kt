package com.example.esnmessenger.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun HomeScreen(onLogout: () -> Unit, onOpenChat: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    var recipientEmail by remember { mutableStateOf("") }
    var isLookingUp by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<String?>(null) }

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
                    user?.email?.let { email ->
                        Text(
                            text = email,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                TextButton(onClick = onLogout) {
                    Text(
                        text = "Sign out",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge
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
