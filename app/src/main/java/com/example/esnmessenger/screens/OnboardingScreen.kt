package com.example.esnmessenger.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.esnmessenger.ui.theme.ESNCyan
import com.example.esnmessenger.ui.theme.ESNMagenta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val INTERESTS = listOf(
    "Sports", "Music", "Cooking", "Travel", "Photography",
    "Gaming", "Reading", "Art", "Technology", "Languages",
    "Dancing", "Hiking", "Cinema", "Volunteering", "Fitness"
)

private val STUDENT_TYPES = listOf("International", "Local")

@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var studentType by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ESNCyan)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Step $step of 3",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        when (step) {
            1 -> StepOne(
                name = name,
                onNameChange = { name = it },
                university = university,
                onUniversityChange = { university = it },
                studentType = studentType,
                onStudentTypeChange = { studentType = it },
                errorMessage = errorMessage,
                onNext = {
                    if (name.isBlank() || university.isBlank() || studentType.isBlank()) {
                        errorMessage = "Please fill in all fields"
                    } else {
                        errorMessage = ""
                        step = 2
                    }
                }
            )
            2 -> StepTwo(
                major = major,
                onMajorChange = { major = it },
                year = year,
                onYearChange = { year = it },
                errorMessage = errorMessage,
                onBack = { step = 1 },
                onNext = {
                    if (major.isBlank() || year.isBlank()) {
                        errorMessage = "Please fill in all fields"
                    } else {
                        errorMessage = ""
                        step = 3
                    }
                }
            )
            3 -> StepThree(
                selectedInterests = selectedInterests,
                onInterestToggle = { interest ->
                    selectedInterests = if (interest in selectedInterests)
                        selectedInterests - interest
                    else
                        selectedInterests + interest
                },
                errorMessage = errorMessage,
                isLoading = isLoading,
                onBack = { step = 2 },
                onFinish = {
                    if (selectedInterests.isEmpty()) {
                        errorMessage = "Select at least one interest"
                        return@StepThree
                    }
                    isLoading = true
                    errorMessage = ""
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@StepThree
                    val profile = hashMapOf(
                        "name" to name,
                        "university" to university,
                        "studentType" to studentType,
                        "major" to major,
                        "year" to year,
                        "interests" to selectedInterests.toList()
                    )
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(profile)
                        .addOnSuccessListener { onOnboardingComplete() }
                        .addOnFailureListener {
                            isLoading = false
                            errorMessage = "Failed to save profile. Try again."
                        }
                }
            )
        }
    }
}

@Composable
private fun StepOne(
    name: String, onNameChange: (String) -> Unit,
    university: String, onUniversityChange: (String) -> Unit,
    studentType: String, onStudentTypeChange: (String) -> Unit,
    errorMessage: String, onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        Text("Let's get started!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Tell us a bit about yourself", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))

        Text("Full Name", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(18.dp))
        Text("University", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = university,
            onValueChange = onUniversityChange,
            placeholder = { Text("Your university") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(18.dp))
        Text("Student Type", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            STUDENT_TYPES.forEach { type ->
                val selected = studentType == type
                OutlinedButton(
                    onClick = { onStudentTypeChange(type) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) ESNCyan else Color.Transparent,
                        contentColor = if (selected) Color.White else ESNCyan
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ESNCyan)
                    )
                ) {
                    Text(type)
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StepTwo(
    major: String, onMajorChange: (String) -> Unit,
    year: String, onYearChange: (String) -> Unit,
    errorMessage: String, onBack: () -> Unit, onNext: () -> Unit
) {
    val years = listOf("1st year", "2nd year", "3rd year", "4th year", "Master", "PhD")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        Text("Academic Details", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Help us match you with the right people", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))

        Text("Major / Field of Study", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = major,
            onValueChange = onMajorChange,
            placeholder = { Text("e.g. Computer Science") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(18.dp))
        Text("Year of Study", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        years.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { y ->
                    val selected = year == y
                    OutlinedButton(
                        onClick = { onYearChange(y) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) ESNCyan else Color.Transparent,
                            contentColor = if (selected) Color.White else ESNCyan
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ESNCyan)
                        )
                    ) {
                        Text(y, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StepThree(
    selectedInterests: Set<String>,
    onInterestToggle: (String) -> Unit,
    errorMessage: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        Text("Your Interests", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("What do you enjoy doing?", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(INTERESTS) { interest ->
                val selected = interest in selectedInterests
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            color = if (selected) ESNMagenta else Color.LightGray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = if (selected) ESNMagenta.copy(alpha = 0.1f) else Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onInterestToggle(interest) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = interest,
                        color = if (selected) ESNMagenta else Color.Gray,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back")
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f).height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else
                    Text("Get Started", fontWeight = FontWeight.Bold)
            }
        }
    }
}
