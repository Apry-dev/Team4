package com.example.esnmessenger.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.esnmessenger.model.INTEREST_EMOJIS
import com.example.esnmessenger.model.INTERESTS
import com.example.esnmessenger.model.STUDENT_TYPES
import com.example.esnmessenger.model.YEARS
import com.example.esnmessenger.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun OnboardingScreen(email: String, onOnboardingComplete: () -> Unit) {
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Gradient header with segmented step indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(ESNCyanDark, ESNCyan)))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..3).forEach { i ->
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .weight(1f)
                                .background(
                                    color = if (i <= step) Color.White else Color.White.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when (step) {
                        1 -> "About You"
                        2 -> "Your Studies"
                        else -> "Your Interests"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Step $step of 3",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
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
                    val currentUser = FirebaseAuth.getInstance().currentUser ?: return@StepThree
                    val uid = currentUser.uid
                    val profile = hashMapOf(
                        "email" to email.trim().lowercase(),
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
        Text(
            "Let's get started!",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tell us a bit about yourself",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(28.dp))

        Text("Full Name", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan,
            )
        )

        Spacer(Modifier.height(20.dp))
        Text("University", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = university,
            onValueChange = onUniversityChange,
            placeholder = { Text("Your university") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan,
            )
        )

        Spacer(Modifier.height(20.dp))
        Text("Student Type", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            STUDENT_TYPES.forEach { type ->
                FilterChip(
                    selected = studentType == type,
                    onClick = { onStudentTypeChange(type) },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ESNCyan,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                    )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ESNCyan)
        ) {
            Text("Continue", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepTwo(
    major: String, onMajorChange: (String) -> Unit,
    year: String, onYearChange: (String) -> Unit,
    errorMessage: String, onBack: () -> Unit, onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        Text(
            "Academic Details",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Help us match you with the right people",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(28.dp))

        Text("Major / Field of Study", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = major,
            onValueChange = onMajorChange,
            placeholder = { Text("e.g. Computer Science") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan,
            )
        )

        Spacer(Modifier.height(20.dp))
        Text("Year of Study", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            YEARS.forEach { y ->
                FilterChip(
                    selected = year == y,
                    onClick = { onYearChange(y) },
                    label = { Text(y) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ESNCyan,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                    )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ESNCyan)
            ) {
                Text("Continue", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Your Interests",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "What do you enjoy doing?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(20.dp))

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                INTERESTS.forEach { interest ->
                    val selected = interest in selectedInterests
                    FilterChip(
                        selected = selected,
                        onClick = { onInterestToggle(interest) },
                        label = {
                            Text("${INTEREST_EMOJIS[interest] ?: ""} $interest")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ESNMagenta,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                        )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f).height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ESNCyan)
            ) {
                if (isLoading)
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                else
                    Text(
                        "Get Started",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
