package com.example.esnmessenger.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.esnmessenger.model.INTEREST_EMOJIS
import com.example.esnmessenger.model.INTERESTS
import com.example.esnmessenger.model.LANGUAGES
import com.example.esnmessenger.model.STUDENT_TYPES
import com.example.esnmessenger.model.YEARS
import com.example.esnmessenger.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun OnboardingScreen(email: String, onOnboardingComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var studentType by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var selectedLanguages by remember { mutableStateOf(setOf<String>()) }
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

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(280)) { it * direction } + fadeIn(tween(280))) togetherWith
                (slideOutHorizontally(tween(220)) { -it * direction } + fadeOut(tween(180)))
            },
            label = "onboarding_step"
        ) { currentStep ->
            when (currentStep) {
                1 -> StepOne(
                    name = name,
                    onNameChange = { name = it },
                    country = country,
                    onCountryChange = { country = it },
                    city = city,
                    onCityChange = { city = it },
                    university = university,
                    onUniversityChange = { university = it },
                    studentType = studentType,
                    onStudentTypeChange = { studentType = it },
                    errorMessage = errorMessage,
                    onNext = {
                        if (name.isBlank() || country.isBlank() || university.isBlank() || studentType.isBlank()) {
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
                else -> StepThree(
                    selectedInterests = selectedInterests,
                    onInterestToggle = { interest ->
                        selectedInterests = if (interest in selectedInterests)
                            selectedInterests - interest
                        else
                            selectedInterests + interest
                    },
                    selectedLanguages = selectedLanguages,
                    onLanguageToggle = { lang ->
                        selectedLanguages = if (lang in selectedLanguages)
                            selectedLanguages - lang
                        else
                            selectedLanguages + lang
                    },
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    onBack = { step = 2 },
                    onFinish = {
                        if (selectedInterests.isEmpty()) {
                            errorMessage = "Select at least one interest"
                            return@StepThree
                        }
                        if (selectedLanguages.isEmpty()) {
                            errorMessage = "Select at least one language"
                            return@StepThree
                        }
                        isLoading = true
                        errorMessage = ""
                        val currentUser = FirebaseAuth.getInstance().currentUser ?: return@StepThree
                        val uid = currentUser.uid
                        val profile = hashMapOf(
                            "email" to email.trim().lowercase(),
                            "name" to name,
                            "country" to country,
                            "city" to city,
                            "university" to university,
                            "studentType" to studentType,
                            "major" to major,
                            "year" to year,
                            "interests" to selectedInterests.toList(),
                            "languages" to selectedLanguages.toList()
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
}

@Composable
private fun StepOne(
    name: String, onNameChange: (String) -> Unit,
    country: String, onCountryChange: (String) -> Unit,
    city: String, onCityChange: (String) -> Unit,
    university: String, onUniversityChange: (String) -> Unit,
    studentType: String, onStudentTypeChange: (String) -> Unit,
    errorMessage: String, onNext: () -> Unit
) {
    var nameError by remember { mutableStateOf<String?>(null) }
    var countryError by remember { mutableStateOf<String?>(null) }
    var universityError by remember { mutableStateOf<String?>(null) }
    var studentTypeError by remember { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            isLocating = true
            locationError = null
            coroutineScope.launch {
                val result = fetchCityFromLocation(context)
                if (result != null) onCityChange(result) else locationError = "Couldn't determine city"
                isLocating = false
            }
        } else {
            locationError = "Location permission denied"
        }
    }

    fun onLocationClick() {
        locationError = null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            isLocating = true
            coroutineScope.launch {
                val result = fetchCityFromLocation(context)
                if (result != null) onCityChange(result) else locationError = "Couldn't determine city"
                isLocating = false
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        Text(
            "Let's get started!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Tell us a bit about yourself",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        Text("Full Name", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { onNameChange(it); nameError = if (it.isBlank()) "Required" else null },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = { if (nameError != null) Text(nameError!!) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan,
            )
        )

        Spacer(Modifier.height(12.dp))
        Text("Country", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        CountryDropdown(
            value = country,
            onValueChange = { onCountryChange(it); countryError = null },
            modifier = Modifier.fillMaxWidth()
        )
        if (countryError != null) {
            Text(countryError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("City", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = city,
            onValueChange = { onCityChange(it); locationError = null },
            placeholder = { Text("Your current city") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            trailingIcon = {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = androidx.compose.ui.Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ESNCyan
                    )
                } else {
                    IconButton(onClick = ::onLocationClick) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Use my location",
                            tint = ESNCyan
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan,
            )
        )
        if (locationError != null) {
            Text(locationError!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }

        Spacer(Modifier.height(12.dp))
        Text("University", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = university,
            onValueChange = { onUniversityChange(it); universityError = if (it.isBlank()) "Required" else null },
            placeholder = { Text("Your university") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = universityError != null,
            supportingText = { if (universityError != null) Text(universityError!!) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan,
            )
        )

        Spacer(Modifier.height(12.dp))
        Text("Student Type", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            STUDENT_TYPES.forEach { type ->
                FilterChip(
                    selected = studentType == type,
                    onClick = { onStudentTypeChange(type); studentTypeError = null },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ESNCyan,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                    )
                )
            }
        }
        if (studentTypeError != null) {
            Text(studentTypeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                nameError = if (name.isBlank()) "Required" else null
                countryError = if (country.isBlank()) "Required" else null
                universityError = if (university.isBlank()) "Required" else null
                studentTypeError = if (studentType.isBlank()) "Select a type" else null
                if (name.isNotBlank() && country.isNotBlank() && university.isNotBlank() && studentType.isNotBlank()) {
                    onNext()
                }
            },
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
    var majorError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        Text(
            "Academic Details", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Help us match you with the right people",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        Text("Major / Field of Study", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = major,
            onValueChange = { onMajorChange(it); majorError = if (it.isBlank()) "Required" else null },
            placeholder = { Text("e.g. Computer Science") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            isError = majorError != null,
            supportingText = { if (majorError != null) Text(majorError!!) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ESNCyan,
                focusedLabelColor = ESNCyan,
            )
        )

        Spacer(Modifier.height(12.dp))
        Text("Year of Study", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            YEARS.forEach { y ->
                FilterChip(
                    selected = year == y,
                    onClick = { onYearChange(y); yearError = null },
                    label = { Text(y) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ESNCyan,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                    )
                )
            }
        }
        if (yearError != null) {
            Text(yearError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp))
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
                onClick = {
                    majorError = if (major.isBlank()) "Required" else null
                    yearError = if (year.isBlank()) "Select a year" else null
                    if (major.isNotBlank() && year.isNotBlank()) onNext()
                },
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
    selectedLanguages: Set<String>,
    onLanguageToggle: (String) -> Unit,
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "What do you enjoy doing?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        label = { Text(interest) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ESNMagenta,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Languages you speak",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Select all that apply",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LANGUAGES.forEach { lang ->
                    val selected = lang in selectedLanguages
                    FilterChip(
                        selected = selected,
                        onClick = { onLanguageToggle(lang) },
                        label = { Text(lang) },
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

@Suppress("DEPRECATION")
private suspend fun fetchCityFromLocation(context: android.content.Context): String? =
    withContext(Dispatchers.IO) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = client.lastLocation.await() ?: return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        cont.resume(addresses.firstOrNull()?.locality, null)
                    }
                }
            } else {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()?.locality
            }
        } catch (_: Exception) {
            null
        }
    }
